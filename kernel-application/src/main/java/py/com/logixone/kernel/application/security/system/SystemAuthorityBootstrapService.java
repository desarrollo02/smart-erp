package py.com.logixone.kernel.application.security.system;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.SecurityOperationCode;
import py.com.logixone.kernel.application.security.SecurityOperationResult;
import py.com.logixone.kernel.application.security.audit.SecurityAuditActor;
import py.com.logixone.kernel.application.security.audit.SecurityAuditOutcome;
import py.com.logixone.kernel.application.security.port.AppUserIdGenerator;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAuditEvent;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAuditOperation;
import py.com.logixone.kernel.application.security.system.command.BootstrapSystemAuthorityCommand;
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

/** Fail-closed, idempotent bootstrap for the first kernel-wide administrator. */
public final class SystemAuthorityBootstrapService {

    private final AppUserRepository userRepository;
    private final SystemAuthorityRepository authorityRepository;
    private final AppUserIdGenerator userIdGenerator;
    private final SystemRoleIdGenerator roleIdGenerator;
    private final SystemAuthorityAuditPort auditPort;
    private final Clock clock;
    private final SystemAuthoritySafetyPolicy safetyPolicy = new SystemAuthoritySafetyPolicy();

    public SystemAuthorityBootstrapService(
            AppUserRepository userRepository,
            SystemAuthorityRepository authorityRepository,
            AppUserIdGenerator userIdGenerator,
            SystemRoleIdGenerator roleIdGenerator,
            SystemAuthorityAuditPort auditPort,
            Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.authorityRepository = Objects.requireNonNull(
                authorityRepository, "authorityRepository");
        this.userIdGenerator = Objects.requireNonNull(userIdGenerator, "userIdGenerator");
        this.roleIdGenerator = Objects.requireNonNull(roleIdGenerator, "roleIdGenerator");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SecurityOperationResult<SystemAuthorityBootstrapState> bootstrap(
            BootstrapSystemAuthorityCommand command) {
        Objects.requireNonNull(command, "command");
        authorityRepository.lockAuthorityState();
        if (!command.permissions().contains(SystemPermission.SYSTEM_ADMINISTRATION_MANAGE)) {
            return rejected(
                    null,
                    null,
                    SecurityOperationCode.SYSTEM_BOOTSTRAP_ADMINISTRATOR_PERMISSION_REQUIRED);
        }

        AppUser existingUser = userRepository.findByExternalIdentity(
                command.externalIdentity()).orElse(null);
        SystemRole existingRole = authorityRepository.findRoleByCode(
                command.roleCode()).orElse(null);

        if (existingUser != null && !compatibleUser(existingUser, command)) {
            return rejected(
                    existingUser.id(),
                    existingRole == null ? null : existingRole.id(),
                    SecurityOperationCode.SYSTEM_BOOTSTRAP_IDENTITY_INCOMPATIBLE);
        }
        if (existingRole != null && !compatibleRole(existingRole, command)) {
            return rejected(
                    existingUser == null ? null : existingUser.id(),
                    existingRole.id(),
                    SecurityOperationCode.SYSTEM_BOOTSTRAP_ROLE_INCOMPATIBLE);
        }

        if (existingUser != null && existingRole != null) {
            return validateExisting(command, existingUser, existingRole);
        }
        return createMissingState(command, existingUser, existingRole);
    }

    private SecurityOperationResult<SystemAuthorityBootstrapState> validateExisting(
            BootstrapSystemAuthorityCommand command,
            AppUser user,
            SystemRole role) {
        AppUserSystemRoleAssignment assignment = authorityRepository.findAssignment(
                user.id(), role.id()).orElse(null);
        if (assignment == null) {
            return rejected(
                    user.id(),
                    role.id(),
                    SecurityOperationCode.SYSTEM_BOOTSTRAP_ASSIGNMENT_INCOMPATIBLE);
        }

        Set<SystemRolePermissionGrant> grants = declaredGrants(command, role.id());
        if (grants.size() != command.permissions().size()) {
            return rejected(
                    user.id(),
                    role.id(),
                    SecurityOperationCode.SYSTEM_BOOTSTRAP_PERMISSION_INCOMPATIBLE);
        }
        if (!safe(user, role, assignment, grants)) {
            return rejected(
                    user.id(),
                    role.id(),
                    SecurityOperationCode.SYSTEM_BOOTSTRAP_CONTEXT_INVALID);
        }

        SystemAuthorityBootstrapState state = new SystemAuthorityBootstrapState(
                user, role, assignment, grants);
        record(SecurityAuditOutcome.UNCHANGED, user.id(), role.id(), null);
        return SecurityOperationResult.unchanged(state);
    }

    private SecurityOperationResult<SystemAuthorityBootstrapState> createMissingState(
            BootstrapSystemAuthorityCommand command,
            AppUser existingUser,
            SystemRole existingRole) {
        AppUser candidateUser = existingUser == null ? newUser(command) : existingUser;
        if (existingUser == null && userRepository.findById(candidateUser.id()).isPresent()) {
            return rejected(
                    candidateUser.id(),
                    existingRole == null ? null : existingRole.id(),
                    SecurityOperationCode.SYSTEM_BOOTSTRAP_IDENTITY_INCOMPATIBLE);
        }

        SystemRole candidateRole = existingRole == null ? newRole(command) : existingRole;
        if (existingRole == null && authorityRepository.findRoleById(candidateRole.id()).isPresent()) {
            return rejected(
                    candidateUser.id(),
                    candidateRole.id(),
                    SecurityOperationCode.SYSTEM_BOOTSTRAP_ROLE_INCOMPATIBLE);
        }

        if (authorityRepository.findAssignment(candidateUser.id(), candidateRole.id()).isPresent()) {
            return rejected(
                    candidateUser.id(),
                    candidateRole.id(),
                    SecurityOperationCode.SYSTEM_BOOTSTRAP_CONTEXT_INVALID);
        }

        AppUserSystemRoleAssignment candidateAssignment = new AppUserSystemRoleAssignment(
                candidateUser.id(), candidateRole.id());
        List<SystemRolePermissionGrant> missingGrants = new ArrayList<>();
        Set<SystemRolePermissionGrant> candidateGrants = new LinkedHashSet<>();
        for (SystemPermission permission : command.permissions()) {
            Optional<SystemRolePermissionGrant> existingGrant =
                    authorityRepository.findPermissionGrant(candidateRole.id(), permission);
            SystemRolePermissionGrant grant = existingGrant.orElseGet(
                    () -> new SystemRolePermissionGrant(candidateRole.id(), permission));
            candidateGrants.add(grant);
            if (existingGrant.isEmpty()) {
                missingGrants.add(grant);
            }
        }

        if (!safe(candidateUser, candidateRole, candidateAssignment, candidateGrants)) {
            return rejected(
                    candidateUser.id(),
                    candidateRole.id(),
                    SecurityOperationCode.SYSTEM_BOOTSTRAP_CONTEXT_INVALID);
        }

        SystemRole storedRole = existingRole == null
                ? authorityRepository.saveRole(candidateRole)
                : candidateRole;
        AppUser storedUser = existingUser == null
                ? userRepository.save(candidateUser)
                : candidateUser;
        if (!storedRole.id().equals(candidateRole.id())
                || !storedUser.id().equals(candidateUser.id())) {
            throw new IllegalStateException("bootstrap persistence changed a technical identity");
        }

        AppUserSystemRoleAssignment storedAssignment = authorityRepository.saveAssignment(
                candidateAssignment);
        Set<SystemRolePermissionGrant> storedGrants = new LinkedHashSet<>(candidateGrants);
        storedGrants.removeAll(missingGrants);
        for (SystemRolePermissionGrant missingGrant : missingGrants) {
            storedGrants.add(authorityRepository.savePermissionGrant(missingGrant));
        }

        SystemAuthorityBootstrapState state = new SystemAuthorityBootstrapState(
                storedUser, storedRole, storedAssignment, storedGrants);
        record(SecurityAuditOutcome.CHANGED, storedUser.id(), storedRole.id(), null);
        return SecurityOperationResult.changed(state);
    }

    private AppUser newUser(BootstrapSystemAuthorityCommand command) {
        return new AppUser(
                userIdGenerator.nextId(),
                command.externalIdentity(),
                command.userDisplayName(),
                UserStatus.ACTIVE,
                0);
    }

    private SystemRole newRole(BootstrapSystemAuthorityCommand command) {
        return new SystemRole(
                roleIdGenerator.nextId(),
                command.roleCode(),
                command.roleDisplayName(),
                SystemRoleStatus.ACTIVE,
                0);
    }

    private Set<SystemRolePermissionGrant> declaredGrants(
            BootstrapSystemAuthorityCommand command,
            SystemRoleId roleId) {
        Set<SystemRolePermissionGrant> grants = new LinkedHashSet<>();
        for (SystemPermission permission : command.permissions()) {
            authorityRepository.findPermissionGrant(roleId, permission).ifPresent(grants::add);
        }
        return grants;
    }

    private boolean safe(
            AppUser user,
            SystemRole role,
            AppUserSystemRoleAssignment assignment,
            Set<SystemRolePermissionGrant> grants) {
        return safetyPolicy.evaluate(
                List.of(user), List.of(role), List.of(assignment), grants)
                == SystemAuthoritySafetyStatus.SAFE;
    }

    private static boolean compatibleUser(
            AppUser user,
            BootstrapSystemAuthorityCommand command) {
        return user.isActive() && user.displayName().equals(command.userDisplayName());
    }

    private static boolean compatibleRole(
            SystemRole role,
            BootstrapSystemAuthorityCommand command) {
        return role.isActive()
                && role.code().equals(command.roleCode())
                && role.displayName().equals(command.roleDisplayName());
    }

    private SecurityOperationResult<SystemAuthorityBootstrapState> rejected(
            AppUserId userId,
            SystemRoleId roleId,
            SecurityOperationCode code) {
        record(SecurityAuditOutcome.REJECTED, userId, roleId, code);
        return SecurityOperationResult.rejected(code);
    }

    private void record(
            SecurityAuditOutcome outcome,
            AppUserId userId,
            SystemRoleId roleId,
            SecurityOperationCode code) {
        auditPort.record(new SystemAuthorityAuditEvent(
                SystemAuthorityAuditOperation.BOOTSTRAP_SYSTEM_AUTHORITY,
                outcome,
                Optional.ofNullable(userId),
                Optional.ofNullable(roleId),
                Optional.empty(),
                Optional.ofNullable(code),
                Optional.empty(),
                Optional.empty(),
                clock.instant(),
                SecurityAuditActor.SYSTEM));
    }
}
