package py.com.logixone.kernel.application.security.system;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.SecurityOperationCode;
import py.com.logixone.kernel.application.security.SecurityOperationResult;
import py.com.logixone.kernel.application.security.audit.SecurityAuditActor;
import py.com.logixone.kernel.application.security.audit.SecurityAuditOutcome;
import py.com.logixone.kernel.application.security.command.ChangeAppUserStatusCommand;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAuditEvent;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAuditOperation;
import py.com.logixone.kernel.application.security.system.command.AssignSystemRoleCommand;
import py.com.logixone.kernel.application.security.system.command.ChangeSystemRoleStatusCommand;
import py.com.logixone.kernel.application.security.system.command.GrantSystemPermissionCommand;
import py.com.logixone.kernel.application.security.system.command.RegisterSystemRoleCommand;
import py.com.logixone.kernel.application.security.system.command.RevokeSystemPermissionCommand;
import py.com.logixone.kernel.application.security.system.command.UnassignSystemRoleCommand;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityAuditPort;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityRepository;
import py.com.logixone.kernel.application.security.system.port.SystemRoleIdGenerator;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.UserStatus;
import py.com.logixone.kernel.domain.security.system.AppUserSystemRoleAssignment;
import py.com.logixone.kernel.domain.security.system.SystemAuthoritySafetyPolicy;
import py.com.logixone.kernel.domain.security.system.SystemAuthoritySafetyStatus;
import py.com.logixone.kernel.domain.security.system.SystemRole;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;
import py.com.logixone.kernel.domain.security.system.SystemRolePermissionGrant;
import py.com.logixone.kernel.domain.security.system.SystemRoleStatus;

/** Pure administration of kernel-wide authority; Jakarta supplies one JTA transaction. */
public final class SystemAuthorityAdministrationService {

    private final AppUserRepository userRepository;
    private final SystemAuthorityRepository authorityRepository;
    private final SystemRoleIdGenerator roleIdGenerator;
    private final SystemAuthorityAuditPort auditPort;
    private final Clock clock;
    private final SecurityAuditActor actor;
    private final SystemAuthoritySafetyPolicy safetyPolicy = new SystemAuthoritySafetyPolicy();

    public SystemAuthorityAdministrationService(
            AppUserRepository userRepository,
            SystemAuthorityRepository authorityRepository,
            SystemRoleIdGenerator roleIdGenerator,
            SystemAuthorityAuditPort auditPort,
            Clock clock,
            SecurityAuditActor actor) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.authorityRepository = Objects.requireNonNull(authorityRepository, "authorityRepository");
        this.roleIdGenerator = Objects.requireNonNull(roleIdGenerator, "roleIdGenerator");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.actor = Objects.requireNonNull(actor, "actor");
    }

    public SecurityOperationResult<SystemRole> registerRole(RegisterSystemRoleCommand command) {
        Objects.requireNonNull(command, "command");
        authorityRepository.lockAuthorityState();
        SystemRole sameCode = authorityRepository.findRoleByCode(command.roleCode()).orElse(null);
        if (sameCode != null) {
            return rejected(SystemAuthorityAuditOperation.REGISTER_SYSTEM_ROLE, null, sameCode.id(),
                    null, SecurityOperationCode.SYSTEM_ROLE_CODE_ALREADY_EXISTS, sameCode.version());
        }
        SystemRoleId roleId = roleIdGenerator.nextId();
        if (authorityRepository.findRoleById(roleId).isPresent()) {
            return rejected(SystemAuthorityAuditOperation.REGISTER_SYSTEM_ROLE, null, roleId,
                    null, SecurityOperationCode.SYSTEM_ROLE_ALREADY_EXISTS, null);
        }
        SystemRole stored = authorityRepository.saveRole(new SystemRole(
                roleId, command.roleCode(), command.displayName(), SystemRoleStatus.INACTIVE, 0));
        changed(SystemAuthorityAuditOperation.REGISTER_SYSTEM_ROLE, null, stored.id(),
                null, null, stored.version());
        return SecurityOperationResult.changed(stored);
    }

    public SecurityOperationResult<AppUser> changeUserStatus(ChangeAppUserStatusCommand command) {
        Objects.requireNonNull(command, "command");
        authorityRepository.lockAuthorityState();
        AppUser current = userRepository.findById(command.userId()).orElse(null);
        if (current == null) {
            return rejected(SystemAuthorityAuditOperation.CHANGE_SYSTEM_USER_STATUS,
                    command.userId(), null, null, SecurityOperationCode.USER_NOT_FOUND, null);
        }
        if (current.status() == command.desiredStatus()) {
            unchanged(SystemAuthorityAuditOperation.CHANGE_SYSTEM_USER_STATUS,
                    current.id(), null, null, current.version());
            return SecurityOperationResult.unchanged(current);
        }
        if (current.version() != command.expectedVersion()) {
            return rejected(SystemAuthorityAuditOperation.CHANGE_SYSTEM_USER_STATUS,
                    current.id(), null, null, SecurityOperationCode.USER_VERSION_CONFLICT,
                    current.version());
        }
        AppUser candidate = new AppUser(current.id(), current.externalIdentity(), current.displayName(),
                command.desiredStatus(), current.version());
        if (command.desiredStatus() == UserStatus.INACTIVE) {
            SecurityOperationCode safetyFailure = safetyFailure(usersReplacing(candidate),
                    authorityRepository.findAllRoles(), authorityRepository.findAllAssignments(),
                    authorityRepository.findAllPermissionGrants());
            if (safetyFailure != null) {
                return rejected(SystemAuthorityAuditOperation.CHANGE_SYSTEM_USER_STATUS,
                        current.id(), null, null, safetyFailure, current.version());
            }
        }
        AppUser stored = userRepository.save(candidate);
        changed(SystemAuthorityAuditOperation.CHANGE_SYSTEM_USER_STATUS, stored.id(), null,
                null, current.version(), stored.version());
        return SecurityOperationResult.changed(stored);
    }

    public SecurityOperationResult<SystemRole> changeRoleStatus(
            ChangeSystemRoleStatusCommand command) {
        Objects.requireNonNull(command, "command");
        authorityRepository.lockAuthorityState();
        SystemRole current = authorityRepository.findRoleById(command.roleId()).orElse(null);
        if (current == null) {
            return rejected(SystemAuthorityAuditOperation.CHANGE_SYSTEM_ROLE_STATUS,
                    null, command.roleId(), null, SecurityOperationCode.SYSTEM_ROLE_NOT_FOUND, null);
        }
        if (current.status() == command.desiredStatus()) {
            unchanged(SystemAuthorityAuditOperation.CHANGE_SYSTEM_ROLE_STATUS,
                    null, current.id(), null, current.version());
            return SecurityOperationResult.unchanged(current);
        }
        if (current.version() != command.expectedVersion()) {
            return rejected(SystemAuthorityAuditOperation.CHANGE_SYSTEM_ROLE_STATUS,
                    null, current.id(), null, SecurityOperationCode.SYSTEM_ROLE_VERSION_CONFLICT,
                    current.version());
        }
        SystemRole candidate = new SystemRole(current.id(), current.code(), current.displayName(),
                command.desiredStatus(), current.version());
        if (command.desiredStatus() == SystemRoleStatus.INACTIVE) {
            SecurityOperationCode safetyFailure = safetyFailure(authorityRepository.findAssignedUsers(),
                    rolesReplacing(candidate), authorityRepository.findAllAssignments(),
                    authorityRepository.findAllPermissionGrants());
            if (safetyFailure != null) {
                return rejected(SystemAuthorityAuditOperation.CHANGE_SYSTEM_ROLE_STATUS,
                        null, current.id(), null, safetyFailure, current.version());
            }
        }
        SystemRole stored = authorityRepository.saveRole(candidate);
        changed(SystemAuthorityAuditOperation.CHANGE_SYSTEM_ROLE_STATUS, null, stored.id(),
                null, current.version(), stored.version());
        return SecurityOperationResult.changed(stored);
    }

    public SecurityOperationResult<AppUserSystemRoleAssignment> assignRole(
            AssignSystemRoleCommand command) {
        Objects.requireNonNull(command, "command");
        authorityRepository.lockAuthorityState();
        if (userRepository.findById(command.userId()).isEmpty()) {
            return rejected(SystemAuthorityAuditOperation.ASSIGN_SYSTEM_ROLE,
                    command.userId(), command.roleId(), null, SecurityOperationCode.USER_NOT_FOUND, null);
        }
        if (authorityRepository.findRoleById(command.roleId()).isEmpty()) {
            return rejected(SystemAuthorityAuditOperation.ASSIGN_SYSTEM_ROLE,
                    command.userId(), command.roleId(), null,
                    SecurityOperationCode.SYSTEM_ROLE_NOT_FOUND, null);
        }
        AppUserSystemRoleAssignment existing = authorityRepository.findAssignment(
                command.userId(), command.roleId()).orElse(null);
        if (existing != null) {
            unchanged(SystemAuthorityAuditOperation.ASSIGN_SYSTEM_ROLE,
                    existing.userId(), existing.roleId(), null, null);
            return SecurityOperationResult.unchanged(existing);
        }
        AppUserSystemRoleAssignment stored = authorityRepository.saveAssignment(
                new AppUserSystemRoleAssignment(command.userId(), command.roleId()));
        changed(SystemAuthorityAuditOperation.ASSIGN_SYSTEM_ROLE, stored.userId(), stored.roleId(),
                null, null, null);
        return SecurityOperationResult.changed(stored);
    }

    public SecurityOperationResult<AppUserSystemRoleAssignment> unassignRole(
            UnassignSystemRoleCommand command) {
        Objects.requireNonNull(command, "command");
        authorityRepository.lockAuthorityState();
        AppUserSystemRoleAssignment existing = authorityRepository.findAssignment(
                command.userId(), command.roleId()).orElse(null);
        if (existing == null) {
            return rejected(SystemAuthorityAuditOperation.UNASSIGN_SYSTEM_ROLE,
                    command.userId(), command.roleId(), null,
                    SecurityOperationCode.SYSTEM_ASSIGNMENT_NOT_FOUND, null);
        }
        List<AppUserSystemRoleAssignment> proposed = new ArrayList<>(
                authorityRepository.findAllAssignments());
        proposed.remove(existing);
        SecurityOperationCode safetyFailure = safetyFailure(authorityRepository.findAssignedUsers(),
                authorityRepository.findAllRoles(), proposed,
                authorityRepository.findAllPermissionGrants());
        if (safetyFailure != null) {
            return rejected(SystemAuthorityAuditOperation.UNASSIGN_SYSTEM_ROLE,
                    existing.userId(), existing.roleId(), null, safetyFailure, null);
        }
        authorityRepository.removeAssignment(existing);
        changed(SystemAuthorityAuditOperation.UNASSIGN_SYSTEM_ROLE,
                existing.userId(), existing.roleId(), null, null, null);
        return SecurityOperationResult.changed(existing);
    }

    public SecurityOperationResult<SystemRolePermissionGrant> grantPermission(
            GrantSystemPermissionCommand command) {
        Objects.requireNonNull(command, "command");
        authorityRepository.lockAuthorityState();
        if (!SystemPermission.knownPermissions().contains(command.permission())) {
            return rejected(SystemAuthorityAuditOperation.GRANT_SYSTEM_PERMISSION,
                    null, command.roleId(), command.permission(),
                    SecurityOperationCode.SYSTEM_PERMISSION_UNKNOWN, null);
        }
        if (authorityRepository.findRoleById(command.roleId()).isEmpty()) {
            return rejected(SystemAuthorityAuditOperation.GRANT_SYSTEM_PERMISSION,
                    null, command.roleId(), command.permission(),
                    SecurityOperationCode.SYSTEM_ROLE_NOT_FOUND, null);
        }
        SystemRolePermissionGrant existing = authorityRepository.findPermissionGrant(
                command.roleId(), command.permission()).orElse(null);
        if (existing != null) {
            unchanged(SystemAuthorityAuditOperation.GRANT_SYSTEM_PERMISSION,
                    null, existing.roleId(), existing.permission(), null);
            return SecurityOperationResult.unchanged(existing);
        }
        SystemRolePermissionGrant stored = authorityRepository.savePermissionGrant(
                new SystemRolePermissionGrant(command.roleId(), command.permission()));
        changed(SystemAuthorityAuditOperation.GRANT_SYSTEM_PERMISSION,
                null, stored.roleId(), stored.permission(), null, null);
        return SecurityOperationResult.changed(stored);
    }

    public SecurityOperationResult<SystemRolePermissionGrant> revokePermission(
            RevokeSystemPermissionCommand command) {
        Objects.requireNonNull(command, "command");
        authorityRepository.lockAuthorityState();
        SystemRolePermissionGrant existing = authorityRepository.findPermissionGrant(
                command.roleId(), command.permission()).orElse(null);
        if (existing == null) {
            return rejected(SystemAuthorityAuditOperation.REVOKE_SYSTEM_PERMISSION,
                    null, command.roleId(), command.permission(),
                    SecurityOperationCode.SYSTEM_PERMISSION_GRANT_NOT_FOUND, null);
        }
        List<SystemRolePermissionGrant> proposed = new ArrayList<>(
                authorityRepository.findAllPermissionGrants());
        proposed.remove(existing);
        SecurityOperationCode safetyFailure = safetyFailure(authorityRepository.findAssignedUsers(),
                authorityRepository.findAllRoles(), authorityRepository.findAllAssignments(), proposed);
        if (safetyFailure != null) {
            return rejected(SystemAuthorityAuditOperation.REVOKE_SYSTEM_PERMISSION,
                    null, existing.roleId(), existing.permission(), safetyFailure, null);
        }
        authorityRepository.removePermissionGrant(existing);
        changed(SystemAuthorityAuditOperation.REVOKE_SYSTEM_PERMISSION,
                null, existing.roleId(), existing.permission(), null, null);
        return SecurityOperationResult.changed(existing);
    }

    private List<AppUser> usersReplacing(AppUser candidate) {
        List<AppUser> users = new ArrayList<>(authorityRepository.findAssignedUsers());
        users.removeIf(user -> user.id().equals(candidate.id()));
        if (authorityRepository.findAllAssignments().stream()
                .anyMatch(assignment -> assignment.userId().equals(candidate.id()))) {
            users.add(candidate);
        }
        return users;
    }

    private List<SystemRole> rolesReplacing(SystemRole candidate) {
        List<SystemRole> roles = new ArrayList<>(authorityRepository.findAllRoles());
        roles.removeIf(role -> role.id().equals(candidate.id()));
        roles.add(candidate);
        return roles;
    }

    private SecurityOperationCode safetyFailure(
            List<AppUser> users,
            List<SystemRole> roles,
            List<AppUserSystemRoleAssignment> assignments,
            List<SystemRolePermissionGrant> grants) {
        SystemAuthoritySafetyStatus status = safetyPolicy.evaluate(users, roles, assignments, grants);
        return switch (status) {
            case SAFE -> null;
            case ADMINISTRATOR_REQUIRED -> SecurityOperationCode.SYSTEM_LAST_ADMINISTRATOR_REQUIRED;
            case INVALID_CONTEXT -> SecurityOperationCode.SYSTEM_AUTHORITY_CONTEXT_INVALID;
        };
    }

    private <T> SecurityOperationResult<T> rejected(
            SystemAuthorityAuditOperation operation,
            AppUserId userId,
            SystemRoleId roleId,
            SystemPermission permission,
            SecurityOperationCode code,
            Long previousVersion) {
        record(operation, SecurityAuditOutcome.REJECTED, userId, roleId, permission,
                code, previousVersion, null);
        return SecurityOperationResult.rejected(code);
    }

    private void changed(
            SystemAuthorityAuditOperation operation,
            AppUserId userId,
            SystemRoleId roleId,
            SystemPermission permission,
            Long previousVersion,
            Long resultingVersion) {
        record(operation, SecurityAuditOutcome.CHANGED, userId, roleId, permission,
                null, previousVersion, resultingVersion);
    }

    private void unchanged(
            SystemAuthorityAuditOperation operation,
            AppUserId userId,
            SystemRoleId roleId,
            SystemPermission permission,
            Long version) {
        record(operation, SecurityAuditOutcome.UNCHANGED, userId, roleId, permission,
                null, version, version);
    }

    private void record(
            SystemAuthorityAuditOperation operation,
            SecurityAuditOutcome outcome,
            AppUserId userId,
            SystemRoleId roleId,
            SystemPermission permission,
            SecurityOperationCode code,
            Long previousVersion,
            Long resultingVersion) {
        auditPort.record(new SystemAuthorityAuditEvent(
                operation,
                outcome,
                Optional.ofNullable(userId),
                Optional.ofNullable(roleId),
                Optional.ofNullable(permission),
                Optional.ofNullable(code),
                Optional.ofNullable(previousVersion),
                Optional.ofNullable(resultingVersion),
                clock.instant(),
                actor));
    }
}
