package py.com.logixone.kernel.application.security;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.security.audit.SecurityAuditActor;
import py.com.logixone.kernel.application.security.audit.SecurityAuditOperation;
import py.com.logixone.kernel.application.security.audit.SecurityAuditOutcome;
import py.com.logixone.kernel.application.security.command.BootstrapSecurityCommand;
import py.com.logixone.kernel.application.security.port.AppUserIdGenerator;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.port.CompanyAuthorizationRepository;
import py.com.logixone.kernel.application.security.port.CompanyMembershipRepository;
import py.com.logixone.kernel.application.security.port.RoleIdGenerator;
import py.com.logixone.kernel.application.security.port.SecurityAuditPort;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.CompanyMembership;
import py.com.logixone.kernel.domain.security.CompanyRole;
import py.com.logixone.kernel.domain.security.MembershipRoleAssignment;
import py.com.logixone.kernel.domain.security.MembershipStatus;
import py.com.logixone.kernel.domain.security.RoleId;
import py.com.logixone.kernel.domain.security.RolePermissionGrant;
import py.com.logixone.kernel.domain.security.RoleStatus;
import py.com.logixone.kernel.domain.security.UserStatus;
import py.com.logixone.plugin.api.ContributionId;

/** Fail-closed, idempotent security bootstrap with no public transport adapter. */
public final class SecurityBootstrapService {

    private final CompanyRepository companyRepository;
    private final AppUserRepository userRepository;
    private final CompanyMembershipRepository membershipRepository;
    private final CompanyAuthorizationRepository authorizationRepository;
    private final AppUserIdGenerator userIdGenerator;
    private final RoleIdGenerator roleIdGenerator;
    private final SecurityAuditRecorder audit;

    public SecurityBootstrapService(
            CompanyRepository companyRepository,
            AppUserRepository userRepository,
            CompanyMembershipRepository membershipRepository,
            CompanyAuthorizationRepository authorizationRepository,
            AppUserIdGenerator userIdGenerator,
            RoleIdGenerator roleIdGenerator,
            SecurityAuditPort auditPort,
            Clock clock) {
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.membershipRepository = Objects.requireNonNull(
                membershipRepository, "membershipRepository");
        this.authorizationRepository = Objects.requireNonNull(
                authorizationRepository, "authorizationRepository");
        this.userIdGenerator = Objects.requireNonNull(userIdGenerator, "userIdGenerator");
        this.roleIdGenerator = Objects.requireNonNull(roleIdGenerator, "roleIdGenerator");
        audit = new SecurityAuditRecorder(
                auditPort,
                Objects.requireNonNull(clock, "clock"),
                SecurityAuditActor.SYSTEM);
    }

    public SecurityOperationResult<SecurityBootstrapState> bootstrap(
            BootstrapSecurityCommand command) {
        Objects.requireNonNull(command, "command");
        Company company = companyRepository.findById(command.companyId()).orElse(null);
        if (company == null
                || !company.isActive()
                || !company.customizationPluginId().equals(command.expectedCustomizationPluginId())) {
            return rejected(command, null, null, SecurityOperationCode.BOOTSTRAP_COMPANY_INCOMPATIBLE);
        }

        AppUser existingUser = userRepository.findByExternalIdentity(
                command.externalIdentity()).orElse(null);
        CompanyRole existingRole = authorizationRepository.findRoleByCompanyAndCode(
                command.companyId(), command.roleCode()).orElse(null);

        if (existingRole != null && !compatibleRole(existingRole, command)) {
            return rejected(
                    command,
                    existingUser == null ? null : existingUser.id(),
                    existingRole.id(),
                    SecurityOperationCode.BOOTSTRAP_ROLE_INCOMPATIBLE);
        }

        if (existingUser != null) {
            return validateExistingBootstrap(command, existingUser, existingRole);
        }
        return createBootstrap(command, existingRole);
    }

    private SecurityOperationResult<SecurityBootstrapState> validateExistingBootstrap(
            BootstrapSecurityCommand command,
            AppUser user,
            CompanyRole role) {
        if (!user.isActive() || !user.displayName().equals(command.userDisplayName())) {
            return rejected(
                    command, user.id(), role == null ? null : role.id(),
                    SecurityOperationCode.BOOTSTRAP_IDENTITY_INCOMPATIBLE);
        }
        CompanyMembership membership = membershipRepository.findByUserAndCompany(
                user.id(), command.companyId()).orElse(null);
        if (membership == null || !membership.isActive()) {
            return rejected(
                    command, user.id(), role == null ? null : role.id(),
                    SecurityOperationCode.BOOTSTRAP_MEMBERSHIP_INCOMPATIBLE);
        }
        if (role == null || !compatibleRole(role, command)) {
            return rejected(
                    command, user.id(), role == null ? null : role.id(),
                    SecurityOperationCode.BOOTSTRAP_ROLE_INCOMPATIBLE);
        }
        MembershipRoleAssignment assignment = authorizationRepository.findAssignment(
                user.id(), command.companyId(), role.id()).orElse(null);
        if (assignment == null) {
            return rejected(
                    command, user.id(), role.id(),
                    SecurityOperationCode.BOOTSTRAP_ASSIGNMENT_INCOMPATIBLE);
        }
        Set<RolePermissionGrant> grants = existingDeclaredGrants(command, role.id());
        if (grants.size() != command.permissionIds().size()) {
            return rejected(
                    command, user.id(), role.id(),
                    SecurityOperationCode.BOOTSTRAP_PERMISSION_INCOMPATIBLE);
        }
        SecurityBootstrapState state = new SecurityBootstrapState(
                user, membership, role, assignment, grants);
        audit.record(
                SecurityAuditOperation.BOOTSTRAP_SECURITY,
                SecurityAuditOutcome.UNCHANGED,
                user.id(), command.companyId(), role.id(), null, null, null, null);
        return SecurityOperationResult.unchanged(state);
    }

    private SecurityOperationResult<SecurityBootstrapState> createBootstrap(
            BootstrapSecurityCommand command,
            CompanyRole existingRole) {
        AppUserId userId = userIdGenerator.nextId();
        if (userRepository.findById(userId).isPresent()) {
            return rejected(
                    command, userId, existingRole == null ? null : existingRole.id(),
                    SecurityOperationCode.BOOTSTRAP_IDENTITY_INCOMPATIBLE);
        }

        RoleId roleId;
        CompanyRole role;
        if (existingRole == null) {
            roleId = roleIdGenerator.nextId();
            if (authorizationRepository.findRoleById(roleId).isPresent()) {
                return rejected(
                        command, userId, roleId,
                        SecurityOperationCode.BOOTSTRAP_ROLE_INCOMPATIBLE);
            }
            role = authorizationRepository.saveRole(new CompanyRole(
                    roleId,
                    command.companyId(),
                    command.roleCode(),
                    command.roleDisplayName(),
                    RoleStatus.ACTIVE,
                    0));
        } else {
            role = existingRole;
            roleId = role.id();
        }

        AppUser user = userRepository.save(new AppUser(
                userId,
                command.externalIdentity(),
                command.userDisplayName(),
                UserStatus.ACTIVE,
                0));
        CompanyMembership membership = membershipRepository.save(new CompanyMembership(
                user.id(), command.companyId(), MembershipStatus.ACTIVE, 0));
        MembershipRoleAssignment assignment = authorizationRepository.saveAssignment(
                new MembershipRoleAssignment(user.id(), command.companyId(), roleId));

        Set<RolePermissionGrant> grants = new LinkedHashSet<>();
        for (ContributionId permissionId : command.permissionIds()) {
            RolePermissionGrant grant = authorizationRepository.findPermissionGrant(
                            command.companyId(), roleId, permissionId)
                    .orElseGet(() -> authorizationRepository.savePermissionGrant(
                            new RolePermissionGrant(command.companyId(), roleId, permissionId)));
            grants.add(grant);
        }

        SecurityBootstrapState state = new SecurityBootstrapState(
                user, membership, role, assignment, grants);
        audit.record(
                SecurityAuditOperation.BOOTSTRAP_SECURITY,
                SecurityAuditOutcome.CHANGED,
                user.id(), command.companyId(), role.id(), null, null, null, null);
        return SecurityOperationResult.changed(state);
    }

    private Set<RolePermissionGrant> existingDeclaredGrants(
            BootstrapSecurityCommand command,
            RoleId roleId) {
        Set<RolePermissionGrant> grants = new LinkedHashSet<>();
        for (ContributionId permissionId : command.permissionIds()) {
            Optional<RolePermissionGrant> grant = authorizationRepository.findPermissionGrant(
                    command.companyId(), roleId, permissionId);
            grant.ifPresent(grants::add);
        }
        return grants;
    }

    private static boolean compatibleRole(
            CompanyRole role,
            BootstrapSecurityCommand command) {
        return role.companyId().equals(command.companyId())
                && role.code().equals(command.roleCode())
                && role.displayName().equals(command.roleDisplayName())
                && role.isActive();
    }

    private SecurityOperationResult<SecurityBootstrapState> rejected(
            BootstrapSecurityCommand command,
            AppUserId userId,
            RoleId roleId,
            SecurityOperationCode code) {
        audit.record(
                SecurityAuditOperation.BOOTSTRAP_SECURITY,
                SecurityAuditOutcome.REJECTED,
                userId, command.companyId(), roleId, null, code, null, null);
        return SecurityOperationResult.rejected(code);
    }
}
