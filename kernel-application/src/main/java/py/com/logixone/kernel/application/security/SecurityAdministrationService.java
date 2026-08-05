package py.com.logixone.kernel.application.security;

import java.time.Clock;
import java.util.Collection;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.security.audit.SecurityAuditActor;
import py.com.logixone.kernel.application.security.audit.SecurityAuditOperation;
import py.com.logixone.kernel.application.security.audit.SecurityAuditOutcome;
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
import py.com.logixone.kernel.application.security.port.AppUserIdGenerator;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.port.CompanyAuthorizationRepository;
import py.com.logixone.kernel.application.security.port.CompanyMembershipRepository;
import py.com.logixone.kernel.application.security.port.RoleIdGenerator;
import py.com.logixone.kernel.application.security.port.SecurityAuditPort;
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

/** Pure security administration; the Jakarta adapter supplies the transaction boundary. */
public final class SecurityAdministrationService {

    private final AppUserRepository userRepository;
    private final CompanyMembershipRepository membershipRepository;
    private final CompanyAuthorizationRepository authorizationRepository;
    private final CompanyRepository companyRepository;
    private final AppUserIdGenerator userIdGenerator;
    private final RoleIdGenerator roleIdGenerator;
    private final SecurityAuditRecorder audit;

    public SecurityAdministrationService(
            AppUserRepository userRepository,
            CompanyMembershipRepository membershipRepository,
            CompanyAuthorizationRepository authorizationRepository,
            CompanyRepository companyRepository,
            AppUserIdGenerator userIdGenerator,
            RoleIdGenerator roleIdGenerator,
            SecurityAuditPort auditPort,
            Clock clock,
            SecurityAuditActor actor) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.membershipRepository = Objects.requireNonNull(
                membershipRepository, "membershipRepository");
        this.authorizationRepository = Objects.requireNonNull(
                authorizationRepository, "authorizationRepository");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository");
        this.userIdGenerator = Objects.requireNonNull(userIdGenerator, "userIdGenerator");
        this.roleIdGenerator = Objects.requireNonNull(roleIdGenerator, "roleIdGenerator");
        audit = new SecurityAuditRecorder(auditPort, clock, actor);
    }

    public SecurityOperationResult<AppUser> registerUser(RegisterAppUserCommand command) {
        Objects.requireNonNull(command, "command");
        AppUser existing = userRepository.findByExternalIdentity(command.externalIdentity()).orElse(null);
        if (existing != null) {
            return rejected(
                    SecurityAuditOperation.REGISTER_USER,
                    existing.id(),
                    null,
                    null,
                    null,
                    SecurityOperationCode.EXTERNAL_IDENTITY_ALREADY_EXISTS,
                    existing.version());
        }
        AppUserId userId = userIdGenerator.nextId();
        if (userRepository.findById(userId).isPresent()) {
            return rejected(
                    SecurityAuditOperation.REGISTER_USER,
                    userId,
                    null,
                    null,
                    null,
                    SecurityOperationCode.USER_ALREADY_EXISTS,
                    null);
        }
        AppUser stored = userRepository.save(new AppUser(
                userId,
                command.externalIdentity(),
                command.displayName(),
                UserStatus.INACTIVE,
                0));
        changed(SecurityAuditOperation.REGISTER_USER, stored.id(), null, null, null, null, stored.version());
        return SecurityOperationResult.changed(stored);
    }

    public SecurityOperationResult<AppUser> changeUserStatus(ChangeAppUserStatusCommand command) {
        Objects.requireNonNull(command, "command");
        AppUser current = userRepository.findById(command.userId()).orElse(null);
        if (current == null) {
            return rejected(
                    SecurityAuditOperation.CHANGE_USER_STATUS,
                    command.userId(),
                    null,
                    null,
                    null,
                    SecurityOperationCode.USER_NOT_FOUND,
                    null);
        }
        if (current.status() == command.desiredStatus()) {
            unchanged(SecurityAuditOperation.CHANGE_USER_STATUS, current.id(), null, null, null, current.version());
            return SecurityOperationResult.unchanged(current);
        }
        if (current.version() != command.expectedVersion()) {
            return rejected(
                    SecurityAuditOperation.CHANGE_USER_STATUS,
                    current.id(),
                    null,
                    null,
                    null,
                    SecurityOperationCode.USER_VERSION_CONFLICT,
                    current.version());
        }
        AppUser stored = userRepository.save(new AppUser(
                current.id(),
                current.externalIdentity(),
                current.displayName(),
                command.desiredStatus(),
                current.version()));
        changed(
                SecurityAuditOperation.CHANGE_USER_STATUS,
                stored.id(), null, null, null, current.version(), stored.version());
        return SecurityOperationResult.changed(stored);
    }

    public SecurityOperationResult<CompanyMembership> registerMembership(
            RegisterMembershipCommand command) {
        Objects.requireNonNull(command, "command");
        CompanyMembership existing = membershipRepository.findByUserAndCompany(
                command.userId(), command.companyId()).orElse(null);
        if (existing != null) {
            unchanged(
                    SecurityAuditOperation.REGISTER_MEMBERSHIP,
                    existing.userId(), existing.companyId(), null, null, existing.version());
            return SecurityOperationResult.unchanged(existing);
        }
        SecurityOperationCode parentFailure = membershipParentFailure(
                command.userId(), command.companyId());
        if (parentFailure != null) {
            return rejected(
                    SecurityAuditOperation.REGISTER_MEMBERSHIP,
                    command.userId(), command.companyId(), null, null, parentFailure, null);
        }
        CompanyMembership stored = membershipRepository.save(new CompanyMembership(
                command.userId(), command.companyId(), MembershipStatus.INACTIVE, 0));
        changed(
                SecurityAuditOperation.REGISTER_MEMBERSHIP,
                stored.userId(), stored.companyId(), null, null, null, stored.version());
        return SecurityOperationResult.changed(stored);
    }

    public SecurityOperationResult<CompanyMembership> changeMembershipStatus(
            ChangeMembershipStatusCommand command) {
        Objects.requireNonNull(command, "command");
        CompanyMembership current = membershipRepository.findByUserAndCompany(
                command.userId(), command.companyId()).orElse(null);
        if (current == null) {
            return rejected(
                    SecurityAuditOperation.CHANGE_MEMBERSHIP_STATUS,
                    command.userId(), command.companyId(), null, null,
                    SecurityOperationCode.MEMBERSHIP_NOT_FOUND, null);
        }
        if (current.status() == command.desiredStatus()) {
            unchanged(
                    SecurityAuditOperation.CHANGE_MEMBERSHIP_STATUS,
                    current.userId(), current.companyId(), null, null, current.version());
            return SecurityOperationResult.unchanged(current);
        }
        if (current.version() != command.expectedVersion()) {
            return rejected(
                    SecurityAuditOperation.CHANGE_MEMBERSHIP_STATUS,
                    current.userId(), current.companyId(), null, null,
                    SecurityOperationCode.MEMBERSHIP_VERSION_CONFLICT, current.version());
        }
        CompanyMembership stored = membershipRepository.save(new CompanyMembership(
                current.userId(), current.companyId(), command.desiredStatus(), current.version()));
        changed(
                SecurityAuditOperation.CHANGE_MEMBERSHIP_STATUS,
                stored.userId(), stored.companyId(), null, null, current.version(), stored.version());
        return SecurityOperationResult.changed(stored);
    }

    public SecurityOperationResult<CompanyRole> registerRole(RegisterRoleCommand command) {
        Objects.requireNonNull(command, "command");
        if (companyRepository.findById(command.companyId()).isEmpty()) {
            return rejected(
                    SecurityAuditOperation.REGISTER_ROLE,
                    null, command.companyId(), null, null,
                    SecurityOperationCode.COMPANY_NOT_FOUND, null);
        }
        CompanyRole assigned = authorizationRepository.findRoleByCompanyAndCode(
                command.companyId(), command.roleCode()).orElse(null);
        if (assigned != null) {
            return rejected(
                    SecurityAuditOperation.REGISTER_ROLE,
                    null, command.companyId(), assigned.id(), null,
                    SecurityOperationCode.ROLE_CODE_ALREADY_EXISTS, assigned.version());
        }
        RoleId roleId = roleIdGenerator.nextId();
        if (authorizationRepository.findRoleById(roleId).isPresent()) {
            return rejected(
                    SecurityAuditOperation.REGISTER_ROLE,
                    null, command.companyId(), roleId, null,
                    SecurityOperationCode.ROLE_ALREADY_EXISTS, null);
        }
        CompanyRole stored = authorizationRepository.saveRole(new CompanyRole(
                roleId,
                command.companyId(),
                command.roleCode(),
                command.displayName(),
                RoleStatus.INACTIVE,
                0));
        changed(
                SecurityAuditOperation.REGISTER_ROLE,
                null, stored.companyId(), stored.id(), null, null, stored.version());
        return SecurityOperationResult.changed(stored);
    }

    public SecurityOperationResult<CompanyRole> changeRoleStatus(ChangeRoleStatusCommand command) {
        Objects.requireNonNull(command, "command");
        CompanyRole current = authorizationRepository.findRoleById(command.roleId()).orElse(null);
        if (current == null) {
            return rejected(
                    SecurityAuditOperation.CHANGE_ROLE_STATUS,
                    null, command.companyId(), command.roleId(), null,
                    SecurityOperationCode.ROLE_NOT_FOUND, null);
        }
        if (!current.companyId().equals(command.companyId())) {
            return rejected(
                    SecurityAuditOperation.CHANGE_ROLE_STATUS,
                    null, command.companyId(), current.id(), null,
                    SecurityOperationCode.ROLE_COMPANY_MISMATCH, current.version());
        }
        if (current.status() == command.desiredStatus()) {
            unchanged(
                    SecurityAuditOperation.CHANGE_ROLE_STATUS,
                    null, current.companyId(), current.id(), null, current.version());
            return SecurityOperationResult.unchanged(current);
        }
        if (current.version() != command.expectedVersion()) {
            return rejected(
                    SecurityAuditOperation.CHANGE_ROLE_STATUS,
                    null, current.companyId(), current.id(), null,
                    SecurityOperationCode.ROLE_VERSION_CONFLICT, current.version());
        }
        CompanyRole stored = authorizationRepository.saveRole(new CompanyRole(
                current.id(),
                current.companyId(),
                current.code(),
                current.displayName(),
                command.desiredStatus(),
                current.version()));
        changed(
                SecurityAuditOperation.CHANGE_ROLE_STATUS,
                null, stored.companyId(), stored.id(), null, current.version(), stored.version());
        return SecurityOperationResult.changed(stored);
    }

    public SecurityOperationResult<MembershipRoleAssignment> assignRole(AssignRoleCommand command) {
        Objects.requireNonNull(command, "command");
        if (membershipRepository.findByUserAndCompany(command.userId(), command.companyId()).isEmpty()) {
            return rejected(
                    SecurityAuditOperation.ASSIGN_ROLE,
                    command.userId(), command.companyId(), command.roleId(), null,
                    SecurityOperationCode.MEMBERSHIP_NOT_FOUND, null);
        }
        CompanyRole role = authorizationRepository.findRoleById(command.roleId()).orElse(null);
        if (role == null) {
            return rejected(
                    SecurityAuditOperation.ASSIGN_ROLE,
                    command.userId(), command.companyId(), command.roleId(), null,
                    SecurityOperationCode.ROLE_NOT_FOUND, null);
        }
        if (!role.companyId().equals(command.companyId())) {
            return rejected(
                    SecurityAuditOperation.ASSIGN_ROLE,
                    command.userId(), command.companyId(), role.id(), null,
                    SecurityOperationCode.ROLE_COMPANY_MISMATCH, role.version());
        }
        MembershipRoleAssignment existing = authorizationRepository.findAssignment(
                command.userId(), command.companyId(), command.roleId()).orElse(null);
        if (existing != null) {
            unchanged(
                    SecurityAuditOperation.ASSIGN_ROLE,
                    existing.userId(), existing.companyId(), existing.roleId(), null, null);
            return SecurityOperationResult.unchanged(existing);
        }
        MembershipRoleAssignment stored = authorizationRepository.saveAssignment(
                new MembershipRoleAssignment(
                        command.userId(), command.companyId(), command.roleId()));
        changed(
                SecurityAuditOperation.ASSIGN_ROLE,
                stored.userId(), stored.companyId(), stored.roleId(), null, null, null);
        return SecurityOperationResult.changed(stored);
    }

    public SecurityOperationResult<MembershipRoleAssignment> unassignRole(
            UnassignRoleCommand command) {
        Objects.requireNonNull(command, "command");
        MembershipRoleAssignment existing = authorizationRepository.findAssignment(
                command.userId(), command.companyId(), command.roleId()).orElse(null);
        if (existing == null) {
            return rejected(
                    SecurityAuditOperation.UNASSIGN_ROLE,
                    command.userId(), command.companyId(), command.roleId(), null,
                    SecurityOperationCode.ASSIGNMENT_NOT_FOUND, null);
        }
        if (!authorizationRepository.removeAssignment(existing)) {
            return rejected(
                    SecurityAuditOperation.UNASSIGN_ROLE,
                    command.userId(), command.companyId(), command.roleId(), null,
                    SecurityOperationCode.PERSISTENCE_CONFLICT, null);
        }
        changed(
                SecurityAuditOperation.UNASSIGN_ROLE,
                existing.userId(), existing.companyId(), existing.roleId(), null, null, null);
        return SecurityOperationResult.changed(existing);
    }

    public SecurityOperationResult<RolePermissionGrant> grantPermission(
            GrantPermissionCommand command) {
        Objects.requireNonNull(command, "command");
        CompanyRole role = authorizationRepository.findRoleById(command.roleId()).orElse(null);
        if (role == null) {
            return rejected(
                    SecurityAuditOperation.GRANT_PERMISSION,
                    null, command.companyId(), command.roleId(), command.permissionId(),
                    SecurityOperationCode.ROLE_NOT_FOUND, null);
        }
        if (!role.companyId().equals(command.companyId())) {
            return rejected(
                    SecurityAuditOperation.GRANT_PERMISSION,
                    null, command.companyId(), role.id(), command.permissionId(),
                    SecurityOperationCode.ROLE_COMPANY_MISMATCH, role.version());
        }
        RolePermissionGrant existing = authorizationRepository.findPermissionGrant(
                command.companyId(), command.roleId(), command.permissionId()).orElse(null);
        if (existing != null) {
            unchanged(
                    SecurityAuditOperation.GRANT_PERMISSION,
                    null, existing.companyId(), existing.roleId(), existing.permissionId(), null);
            return SecurityOperationResult.unchanged(existing);
        }
        RolePermissionGrant stored = authorizationRepository.savePermissionGrant(
                new RolePermissionGrant(
                        command.companyId(), command.roleId(), command.permissionId()));
        changed(
                SecurityAuditOperation.GRANT_PERMISSION,
                null, stored.companyId(), stored.roleId(), stored.permissionId(), null, null);
        return SecurityOperationResult.changed(stored);
    }

    public SecurityOperationResult<RolePermissionGrant> grantPermission(
            GrantPermissionCommand command,
            Collection<ContributionId> availablePermissions) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(availablePermissions, "availablePermissions");
        if (!availablePermissions.contains(command.permissionId())) {
            return rejected(
                    SecurityAuditOperation.GRANT_PERMISSION,
                    null, command.companyId(), command.roleId(), command.permissionId(),
                    SecurityOperationCode.PERMISSION_NOT_AVAILABLE, null);
        }
        return grantPermission(command);
    }

    public SecurityOperationResult<RolePermissionGrant> revokePermission(
            RevokePermissionCommand command) {
        Objects.requireNonNull(command, "command");
        RolePermissionGrant existing = authorizationRepository.findPermissionGrant(
                command.companyId(), command.roleId(), command.permissionId()).orElse(null);
        if (existing == null) {
            return rejected(
                    SecurityAuditOperation.REVOKE_PERMISSION,
                    null, command.companyId(), command.roleId(), command.permissionId(),
                    SecurityOperationCode.PERMISSION_GRANT_NOT_FOUND, null);
        }
        if (!authorizationRepository.removePermissionGrant(existing)) {
            return rejected(
                    SecurityAuditOperation.REVOKE_PERMISSION,
                    null, command.companyId(), command.roleId(), command.permissionId(),
                    SecurityOperationCode.PERSISTENCE_CONFLICT, null);
        }
        changed(
                SecurityAuditOperation.REVOKE_PERMISSION,
                null, existing.companyId(), existing.roleId(), existing.permissionId(), null, null);
        return SecurityOperationResult.changed(existing);
    }

    private SecurityOperationCode membershipParentFailure(AppUserId userId, CompanyId companyId) {
        if (userRepository.findById(userId).isEmpty()) {
            return SecurityOperationCode.USER_NOT_FOUND;
        }
        return companyRepository.findById(companyId).isEmpty()
                ? SecurityOperationCode.COMPANY_NOT_FOUND
                : null;
    }

    private <T> SecurityOperationResult<T> rejected(
            SecurityAuditOperation operation,
            AppUserId userId,
            CompanyId companyId,
            RoleId roleId,
            ContributionId permissionId,
            SecurityOperationCode code,
            Long previousVersion) {
        audit.record(
                operation, SecurityAuditOutcome.REJECTED,
                userId, companyId, roleId, permissionId, code, previousVersion, null);
        return SecurityOperationResult.rejected(code);
    }

    private void changed(
            SecurityAuditOperation operation,
            AppUserId userId,
            CompanyId companyId,
            RoleId roleId,
            ContributionId permissionId,
            Long previousVersion,
            Long resultingVersion) {
        audit.record(
                operation, SecurityAuditOutcome.CHANGED,
                userId, companyId, roleId, permissionId, null, previousVersion, resultingVersion);
    }

    private void unchanged(
            SecurityAuditOperation operation,
            AppUserId userId,
            CompanyId companyId,
            RoleId roleId,
            ContributionId permissionId,
            Long version) {
        audit.record(
                operation, SecurityAuditOutcome.UNCHANGED,
                userId, companyId, roleId, permissionId, null, version, version);
    }
}
