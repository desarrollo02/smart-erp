package py.com.logixone.web.admin;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.admin.BusinessSecuritySnapshot;
import py.com.logixone.kernel.application.security.admin.CompanySecurityAdministrationView;
import py.com.logixone.kernel.application.security.admin.SecurityAdministrationActionResult;
import py.com.logixone.kernel.application.security.audit.SecurityAuditActor;
import py.com.logixone.kernel.application.security.command.AssignRoleCommand;
import py.com.logixone.kernel.application.security.command.ChangeAppUserStatusCommand;
import py.com.logixone.kernel.application.security.command.ChangeMembershipStatusCommand;
import py.com.logixone.kernel.application.security.command.ChangeRoleStatusCommand;
import py.com.logixone.kernel.application.security.command.GrantPermissionCommand;
import py.com.logixone.kernel.application.security.command.RegisterAppUserCommand;
import py.com.logixone.kernel.application.security.command.RegisterMembershipCommand;
import py.com.logixone.kernel.application.security.command.RegisterRoleCommand;
import py.com.logixone.kernel.application.security.command.RevokePermissionCommand;
import py.com.logixone.kernel.application.security.command.UnassignRoleCommand;
import py.com.logixone.kernel.application.security.port.BusinessSecurityAdministrationPort;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityContext;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.MembershipStatus;
import py.com.logixone.kernel.domain.security.RoleStatus;
import py.com.logixone.kernel.domain.security.UserStatus;
import py.com.logixone.web.security.RequestCorrelation;
import py.com.logixone.web.security.TrustedAdminWebAccess;
import py.com.logixone.web.security.TrustedWebAccessException;
import py.com.logixone.web.security.ValidatedOidcPrincipal;
import py.com.logixone.web.selector.NativeSelectorSourceCatalog;
import py.com.logixone.web.shell.NativeSelectorReturnRestoration;
import py.com.logixone.web.shell.NativeSelectorReturnViewBean;

/** Request-scoped adapter for local users and company-owned authorization. */
@Named("businessSecurityAdminView")
@RequestScoped
public class BusinessSecurityAdminViewBean {

    @Inject TrustedAdminWebAccess access;
    @Inject RequestCorrelation correlation;
    @Inject ValidatedOidcPrincipal oidcPrincipal;
    @Inject BusinessSecurityAdministrationPort administration;
    @Inject HttpServletRequest request;
    @Inject NativeSelectorReturnViewBean nativeSelectorReturn;

    private BusinessSecuritySnapshot snapshot;
    private List<AdminSecurityUserView> users = List.of();
    private List<AdminSecurityCompanyView> companies = List.of();
    private List<AdminMembershipView> memberships = List.of();
    private List<AdminCompanyRoleView> roles = List.of();
    private List<AdminOptionView> membershipUserOptions = List.of();
    private List<AdminOptionView> membershipOptions = List.of();
    private List<AdminOptionView> roleOptions = List.of();
    private List<AdminOptionView> permissionOptions = List.of();
    private AdminSecurityCompanyView selectedCompany;
    private String configuredIssuer;
    private String companyId;
    private String subject;
    private String displayName;
    private String membershipUserId;
    private String roleCode;
    private String roleDisplayName;
    private String assignmentUserId;
    private String assignmentRoleId;
    private String grantRoleId;
    private String grantPermissionId;
    private boolean operational;
    private boolean canManageSystemAuthority;

    @PostConstruct
    void initialize() {
        SystemAuthorityContext context = access.require(SystemPermission.SECURITY_MANAGE);
        canManageSystemAuthority =
                context.hasPermission(SystemPermission.SYSTEM_ADMINISTRATION_MANAGE);
        configuredIssuer = oidcPrincipal.currentIdentity()
                .orElseThrow(TrustedWebAccessException::unauthorized)
                .issuer();
        snapshot = administration.administrationSnapshot();
        users = snapshot.users().stream().map(AdminSecurityUserView::from).toList();
        companies = snapshot.companies().stream().map(AdminSecurityCompanyView::from).toList();
        Map<String, String> targetInputs = nativeSelectorReturn.targetInputs(
                "/admin/security.xhtml");
        Optional<NativeSelectorReturnRestoration> restoration =
                nativeSelectorReturn.restore("/admin/security.xhtml");
        String requestedCompany = restoration
                .map(value -> value.inputs().get("company_id"))
                .orElseGet(() -> firstNonBlank(
                        targetInputs.get("company_id"), request.getParameter("company")));
        if (requestedCompany != null && !requestedCompany.isBlank()) {
            companyId = requestedCompany;
            loadCompany();
        }
        restoration.ifPresent(this::applyRestoredDraft);
    }

    public void loadCompany() {
        if (companyId == null || companyId.isBlank()) {
            return;
        }
        try {
            CompanySecurityAdministrationView detail = administration
                    .findCompanySecurity(AdminTechnicalInput.companyId(companyId))
                    .orElse(null);
            if (detail == null) {
                AdminSecurityOperationMessages.targetUnavailable();
                return;
            }
            selectedCompany = AdminSecurityCompanyView.from(detail.company());
            operational = detail.operational();
            Map<String, String> roleLabels = detail.roles().stream().collect(Collectors.toUnmodifiableMap(
                    role -> role.roleId().toString(), role -> role.displayName() + " · " + role.code()));
            Set<String> availablePermissionIds = detail.availablePermissions().stream()
                    .map(permission -> permission.value()).collect(Collectors.toUnmodifiableSet());
            memberships = detail.memberships().stream()
                    .map(membership -> AdminMembershipView.from(membership, roleLabels)).toList();
            roles = detail.roles().stream()
                    .map(role -> AdminCompanyRoleView.from(role, availablePermissionIds)).toList();
            Set<String> memberIds = detail.memberships().stream()
                    .map(membership -> membership.userId().toString()).collect(Collectors.toUnmodifiableSet());
            membershipUserOptions = users.stream()
                    .filter(user -> !memberIds.contains(user.getUserId()))
                    .map(user -> new AdminOptionView(user.getUserId(), user.getDisplayName() + " · " + user.getUserId()))
                    .toList();
            membershipOptions = detail.memberships().stream()
                    .map(membership -> new AdminOptionView(
                            membership.userId().toString(), membership.userLabel() + " · " + membership.userId()))
                    .toList();
            roleOptions = detail.roles().stream()
                    .map(role -> new AdminOptionView(
                            role.roleId().toString(), role.displayName() + " · " + role.code()))
                    .toList();
            permissionOptions = detail.availablePermissions().stream()
                    .map(permission -> new AdminOptionView(permission.value(), permission.value()))
                    .toList();
        } catch (IllegalArgumentException invalid) {
            AdminSecurityOperationMessages.targetUnavailable();
        }
    }

    public String openCompany() {
        try {
            return companyRedirect(AdminTechnicalInput.companyId(companyId));
        } catch (IllegalArgumentException invalid) {
            AdminSecurityOperationMessages.invalidInput();
            return null;
        }
    }

    public String registerUser() {
        try {
            var actor = actor();
            Optional<String> name = displayName == null || displayName.isBlank()
                    ? Optional.empty() : Optional.of(displayName);
            var result = administration.registerUser(new RegisterAppUserCommand(
                    new ExternalIdentity(configuredIssuer, AdminTechnicalInput.requiredText(subject, "subject")),
                    name), actor);
            return finish(result, "El usuario fue registrado inactivo.", "El usuario ya existía.", baseRedirect());
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    public String activateUser(String userId, long version) {
        return changeUserStatus(userId, version, UserStatus.ACTIVE);
    }

    public String inactivateUser(String userId, long version) {
        return changeUserStatus(userId, version, UserStatus.INACTIVE);
    }

    private String changeUserStatus(String userId, long version, UserStatus status) {
        try {
            var result = administration.changeUserStatus(new ChangeAppUserStatusCommand(
                    AdminTechnicalInput.userId(userId), status, version), actor());
            return finish(result,
                    status == UserStatus.ACTIVE ? "El usuario quedó activo." : "El usuario quedó inactivo.",
                    "El usuario ya tenía el estado solicitado.", baseRedirect());
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    public String registerMembership() {
        try {
            CompanyId company = selectedCompanyId();
            var result = administration.registerMembership(new RegisterMembershipCommand(
                    AdminTechnicalInput.userId(membershipUserId), company), actor());
            return finish(result, "La membresía fue registrada inactiva.",
                    "La membresía ya existía.", companyRedirect(company));
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    public String activateMembership(String userId, long version) {
        return changeMembershipStatus(userId, version, MembershipStatus.ACTIVE);
    }

    public String inactivateMembership(String userId, long version) {
        return changeMembershipStatus(userId, version, MembershipStatus.INACTIVE);
    }

    private String changeMembershipStatus(
            String userId, long version, MembershipStatus status) {
        try {
            CompanyId company = selectedCompanyId();
            var result = administration.changeMembershipStatus(new ChangeMembershipStatusCommand(
                    AdminTechnicalInput.userId(userId), company, status, version), actor());
            return finish(result,
                    status == MembershipStatus.ACTIVE ? "La membresía quedó activa." : "La membresía quedó inactiva.",
                    "La membresía ya tenía el estado solicitado.", companyRedirect(company));
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    public String registerRole() {
        try {
            CompanyId company = selectedCompanyId();
            var result = administration.registerCompanyRole(new RegisterRoleCommand(
                    company,
                    AdminTechnicalInput.roleCode(roleCode),
                    AdminTechnicalInput.requiredText(roleDisplayName, "role display name")), actor());
            return finish(result, "El rol empresarial fue registrado inactivo.",
                    "El rol ya existía.", companyRedirect(company));
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    public String activateRole(String roleId, long version) {
        return changeRoleStatus(roleId, version, RoleStatus.ACTIVE);
    }

    public String inactivateRole(String roleId, long version) {
        return changeRoleStatus(roleId, version, RoleStatus.INACTIVE);
    }

    private String changeRoleStatus(String roleId, long version, RoleStatus status) {
        try {
            CompanyId company = selectedCompanyId();
            var result = administration.changeCompanyRoleStatus(new ChangeRoleStatusCommand(
                    AdminTechnicalInput.roleId(roleId), company, status, version), actor());
            return finish(result,
                    status == RoleStatus.ACTIVE ? "El rol quedó activo." : "El rol quedó inactivo.",
                    "El rol ya tenía el estado solicitado.", companyRedirect(company));
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    public String assignRole() {
        try {
            CompanyId company = selectedCompanyId();
            var result = administration.assignCompanyRole(new AssignRoleCommand(
                    AdminTechnicalInput.userId(assignmentUserId), company,
                    AdminTechnicalInput.roleId(assignmentRoleId)), actor());
            return finish(result, "El rol fue asignado a la membresía.",
                    "La asignación ya existía.", companyRedirect(company));
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    public String unassignRole(String userId, String roleId) {
        try {
            CompanyId company = selectedCompanyId();
            var result = administration.unassignCompanyRole(new UnassignRoleCommand(
                    AdminTechnicalInput.userId(userId), company,
                    AdminTechnicalInput.roleId(roleId)), actor());
            return finish(result, "El rol fue desasignado.",
                    "La asignación ya no existía.", companyRedirect(company));
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    public String grantPermission() {
        try {
            CompanyId company = selectedCompanyId();
            var result = administration.grantCompanyPermission(new GrantPermissionCommand(
                    company, AdminTechnicalInput.roleId(grantRoleId),
                    AdminTechnicalInput.permissionId(grantPermissionId)), actor());
            return finish(result, "El permiso fue concedido al rol.",
                    "La concesión ya existía.", companyRedirect(company));
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    public String revokePermission(String roleId, String permissionId) {
        try {
            CompanyId company = selectedCompanyId();
            var result = administration.revokeCompanyPermission(new RevokePermissionCommand(
                    company, AdminTechnicalInput.roleId(roleId),
                    AdminTechnicalInput.permissionId(permissionId)), actor());
            return finish(result, "El permiso fue revocado.",
                    "La concesión ya no existía.", companyRedirect(company));
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    private SecurityAuditActor actor() {
        SystemAuthorityContext context = access.require(SystemPermission.SECURITY_MANAGE);
        return SecurityAuditActor.authenticated(context.actorUserId(), correlation.value());
    }

    private CompanyId selectedCompanyId() {
        return AdminTechnicalInput.companyId(companyId);
    }

    private void applyRestoredDraft(NativeSelectorReturnRestoration restoration) {
        Map<String, String> inputs = restoration.inputs();
        switch (restoration.usageId()) {
            case NativeSelectorSourceCatalog.SECURITY_MEMBERSHIP_USER ->
                    membershipUserId = available(inputs.get("membership_user_id"),
                            membershipUserOptions);
            case NativeSelectorSourceCatalog.SECURITY_ASSIGNMENT_USER,
                    NativeSelectorSourceCatalog.SECURITY_ASSIGNMENT_ROLE -> {
                assignmentUserId = available(inputs.get("assignment_user_id"),
                        membershipOptions);
                assignmentRoleId = available(inputs.get("assignment_role_id"), roleOptions);
            }
            case NativeSelectorSourceCatalog.SECURITY_GRANT_ROLE -> {
                grantRoleId = available(inputs.get("grant_role_id"), roleOptions);
                grantPermissionId = available(
                        inputs.get("grant_permission_id"), permissionOptions);
            }
            default -> {
                // SECURITY_COMPANY only restores and reloads the selected company.
            }
        }
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private static String available(String value, List<AdminOptionView> options) {
        if (value == null || options.stream().noneMatch(option -> option.getValue().equals(value))) {
            return null;
        }
        return value;
    }

    private String baseRedirect() {
        return nativeSelectorReturn.preserve(
                "/admin/security.xhtml?faces-redirect=true");
    }

    private String companyRedirect(CompanyId company) {
        return nativeSelectorReturn.preserve(
                "/admin/security.xhtml?faces-redirect=true&company=" + company);
    }

    private String finish(
            SecurityAdministrationActionResult result, String changed, String unchanged, String redirect) {
        return AdminSecurityOperationMessages.finish(result, changed, unchanged, redirect);
    }

    private static String denied() {
        AdminSecurityOperationMessages.denied();
        return null;
    }

    private static String invalid() {
        AdminSecurityOperationMessages.invalidInput();
        return null;
    }

    public List<AdminSecurityUserView> getUsers() { return users; }
    public List<AdminSecurityCompanyView> getCompanies() { return companies; }
    public List<AdminMembershipView> getMemberships() { return memberships; }
    public List<AdminCompanyRoleView> getRoles() { return roles; }
    public List<AdminOptionView> getMembershipUserOptions() { return membershipUserOptions; }
    public List<AdminOptionView> getMembershipOptions() { return membershipOptions; }
    public List<AdminOptionView> getRoleOptions() { return roleOptions; }
    public List<AdminOptionView> getPermissionOptions() { return permissionOptions; }
    public AdminSecurityCompanyView getSelectedCompany() { return selectedCompany; }
    public boolean isCompanySelected() { return selectedCompany != null; }
    public boolean isOperational() { return operational; }
    public boolean isCanManageSystemAuthority() { return canManageSystemAuthority; }
    public String getConfiguredIssuer() { return configuredIssuer; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String value) { companyId = value; }
    public String getSubject() { return subject; }
    public void setSubject(String value) { subject = value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value; }
    public String getMembershipUserId() { return membershipUserId; }
    public void setMembershipUserId(String value) { membershipUserId = value; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String value) { roleCode = value; }
    public String getRoleDisplayName() { return roleDisplayName; }
    public void setRoleDisplayName(String value) { roleDisplayName = value; }
    public String getAssignmentUserId() { return assignmentUserId; }
    public void setAssignmentUserId(String value) { assignmentUserId = value; }
    public String getAssignmentRoleId() { return assignmentRoleId; }
    public void setAssignmentRoleId(String value) { assignmentRoleId = value; }
    public String getGrantRoleId() { return grantRoleId; }
    public void setGrantRoleId(String value) { grantRoleId = value; }
    public String getGrantPermissionId() { return grantPermissionId; }
    public void setGrantPermissionId(String value) { grantPermissionId = value; }
}
