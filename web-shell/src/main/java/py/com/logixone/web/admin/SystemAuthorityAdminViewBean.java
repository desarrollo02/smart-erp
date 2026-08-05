package py.com.logixone.web.admin;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.admin.SecurityAdministrationActionResult;
import py.com.logixone.kernel.application.security.admin.SystemAuthorityAdministrationSnapshot;
import py.com.logixone.kernel.application.security.audit.SecurityAuditActor;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityContext;
import py.com.logixone.kernel.application.security.system.command.AssignSystemRoleCommand;
import py.com.logixone.kernel.application.security.system.command.ChangeSystemRoleStatusCommand;
import py.com.logixone.kernel.application.security.system.command.GrantSystemPermissionCommand;
import py.com.logixone.kernel.application.security.system.command.RegisterSystemRoleCommand;
import py.com.logixone.kernel.application.security.system.command.RevokeSystemPermissionCommand;
import py.com.logixone.kernel.application.security.system.command.UnassignSystemRoleCommand;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityAdministrationPort;
import py.com.logixone.kernel.domain.security.system.SystemRoleStatus;
import py.com.logixone.web.security.RequestCorrelation;
import py.com.logixone.web.security.TrustedAdminWebAccess;
import py.com.logixone.web.security.TrustedWebAccessException;
import py.com.logixone.web.selector.NativeSelectorSourceCatalog;
import py.com.logixone.web.shell.NativeSelectorReturnRestoration;
import py.com.logixone.web.shell.NativeSelectorReturnViewBean;

/** Request-scoped adapter for instance-wide roles and system permissions. */
@Named("systemAuthorityAdminView")
@RequestScoped
public class SystemAuthorityAdminViewBean {

    @Inject TrustedAdminWebAccess access;
    @Inject RequestCorrelation correlation;
    @Inject SystemAuthorityAdministrationPort administration;
    @Inject NativeSelectorReturnViewBean nativeSelectorReturn;

    private List<AdminSecurityUserView> users = List.of();
    private List<AdminSystemRoleView> roles = List.of();
    private List<AdminOptionView> userOptions = List.of();
    private List<AdminOptionView> roleOptions = List.of();
    private List<AdminOptionView> permissionOptions = List.of();
    private String roleCode;
    private String roleDisplayName;
    private String assignmentUserId;
    private String assignmentRoleId;
    private String grantRoleId;
    private String grantPermission;
    private boolean canManageBusinessSecurity;

    @PostConstruct
    void initialize() {
        SystemAuthorityContext context =
                access.require(SystemPermission.SYSTEM_ADMINISTRATION_MANAGE);
        canManageBusinessSecurity = context.hasPermission(SystemPermission.SECURITY_MANAGE);
        SystemAuthorityAdministrationSnapshot snapshot = administration.authoritySnapshot();
        users = snapshot.users().stream().map(AdminSecurityUserView::from).toList();
        Map<String, String> userLabels = users.stream().collect(Collectors.toUnmodifiableMap(
                AdminSecurityUserView::getUserId,
                user -> user.getDisplayName() + " · " + user.getUserId()));
        roles = snapshot.roles().stream()
                .map(role -> AdminSystemRoleView.from(role, userLabels))
                .toList();
        userOptions = users.stream()
                .map(user -> new AdminOptionView(user.getUserId(), userLabels.get(user.getUserId())))
                .toList();
        roleOptions = snapshot.roles().stream()
                .map(role -> new AdminOptionView(
                        role.roleId().toString(), role.displayName() + " · " + role.code()))
                .toList();
        permissionOptions = snapshot.knownPermissions().stream()
                .map(permission -> new AdminOptionView(permission.value(), permission.value()))
                .toList();
        nativeSelectorReturn.restore("/admin/system-authority.xhtml")
                .ifPresent(this::applyRestoredDraft);
    }

    public String registerRole() {
        try {
            var result = administration.registerSystemRole(new RegisterSystemRoleCommand(
                    AdminTechnicalInput.systemRoleCode(roleCode),
                    AdminTechnicalInput.requiredText(roleDisplayName, "system role display name")), actor());
            return finish(result, "El rol global fue registrado inactivo.",
                    "El rol global ya existía.");
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    public String activateRole(String roleId, long version) {
        return changeRoleStatus(roleId, version, SystemRoleStatus.ACTIVE);
    }

    public String inactivateRole(String roleId, long version) {
        return changeRoleStatus(roleId, version, SystemRoleStatus.INACTIVE);
    }

    private String changeRoleStatus(
            String roleId, long version, SystemRoleStatus desiredStatus) {
        try {
            var result = administration.changeSystemRoleStatus(new ChangeSystemRoleStatusCommand(
                    AdminTechnicalInput.systemRoleId(roleId), desiredStatus, version), actor());
            return finish(result,
                    desiredStatus == SystemRoleStatus.ACTIVE
                            ? "El rol global quedó activo."
                            : "El rol global quedó inactivo.",
                    "El rol global ya tenía el estado solicitado.");
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    public String assignRole() {
        try {
            var result = administration.assignSystemRole(new AssignSystemRoleCommand(
                    AdminTechnicalInput.userId(assignmentUserId),
                    AdminTechnicalInput.systemRoleId(assignmentRoleId)), actor());
            return finish(result, "El rol global fue asignado al usuario.",
                    "La asignación global ya existía.");
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    public String unassignRole(String userId, String roleId) {
        try {
            var result = administration.unassignSystemRole(new UnassignSystemRoleCommand(
                    AdminTechnicalInput.userId(userId),
                    AdminTechnicalInput.systemRoleId(roleId)), actor());
            return finish(result, "El rol global fue desasignado.",
                    "La asignación global ya no existía.");
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    public String grantPermission() {
        try {
            var result = administration.grantSystemPermission(new GrantSystemPermissionCommand(
                    AdminTechnicalInput.systemRoleId(grantRoleId),
                    AdminTechnicalInput.systemPermission(grantPermission)), actor());
            return finish(result, "El permiso global fue concedido.",
                    "La concesión global ya existía.");
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    public String revokePermission(String roleId, String permission) {
        try {
            var result = administration.revokeSystemPermission(new RevokeSystemPermissionCommand(
                    AdminTechnicalInput.systemRoleId(roleId),
                    AdminTechnicalInput.systemPermission(permission)), actor());
            return finish(result, "El permiso global fue revocado.",
                    "La concesión global ya no existía.");
        } catch (TrustedWebAccessException denied) {
            return denied();
        } catch (IllegalArgumentException invalid) {
            return invalid();
        }
    }

    private SecurityAuditActor actor() {
        SystemAuthorityContext context =
                access.require(SystemPermission.SYSTEM_ADMINISTRATION_MANAGE);
        return SecurityAuditActor.authenticated(context.actorUserId(), correlation.value());
    }

    private void applyRestoredDraft(NativeSelectorReturnRestoration restoration) {
        Map<String, String> inputs = restoration.inputs();
        switch (restoration.usageId()) {
            case NativeSelectorSourceCatalog.SYSTEM_ASSIGNMENT_USER,
                    NativeSelectorSourceCatalog.SYSTEM_ASSIGNMENT_ROLE -> {
                assignmentUserId = available(inputs.get("assignment_user_id"), userOptions);
                assignmentRoleId = available(inputs.get("assignment_role_id"), roleOptions);
            }
            case NativeSelectorSourceCatalog.SYSTEM_GRANT_ROLE -> {
                grantRoleId = available(inputs.get("grant_role_id"), roleOptions);
                grantPermission = available(inputs.get("grant_permission_id"), permissionOptions);
            }
            default -> {
                // No other native source has this page as its origin.
            }
        }
    }

    private static String available(String value, List<AdminOptionView> options) {
        if (value == null || options.stream().noneMatch(option -> option.getValue().equals(value))) {
            return null;
        }
        return value;
    }

    private String finish(
            SecurityAdministrationActionResult result, String changed, String unchanged) {
        return AdminSecurityOperationMessages.finish(
                result, changed, unchanged,
                nativeSelectorReturn.preserve(
                        "/admin/system-authority.xhtml?faces-redirect=true"));
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
    public List<AdminSystemRoleView> getRoles() { return roles; }
    public List<AdminOptionView> getUserOptions() { return userOptions; }
    public List<AdminOptionView> getRoleOptions() { return roleOptions; }
    public List<AdminOptionView> getPermissionOptions() { return permissionOptions; }
    public boolean isCanManageBusinessSecurity() { return canManageBusinessSecurity; }
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
    public String getGrantPermission() { return grantPermission; }
    public void setGrantPermission(String value) { grantPermission = value; }
}
