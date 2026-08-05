package py.com.logixone.kernel.application.security.system;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityRepository;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.system.AppUserSystemRoleAssignment;
import py.com.logixone.kernel.domain.security.system.SystemRole;
import py.com.logixone.kernel.domain.security.system.SystemRoleCode;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;
import py.com.logixone.kernel.domain.security.system.SystemRolePermissionGrant;

final class SystemAuthorityTestFixture
        implements AppUserRepository, SystemAuthorityRepository {

    final Map<AppUserId, AppUser> users = new LinkedHashMap<>();
    final Map<SystemRoleId, SystemRole> roles = new LinkedHashMap<>();
    final List<AppUserSystemRoleAssignment> assignments = new ArrayList<>();
    final List<SystemRolePermissionGrant> grants = new ArrayList<>();
    int lockCount;

    @Override
    public void lockAuthorityState() {
        lockCount++;
    }

    @Override
    public List<AppUser> findAll() {
        return List.copyOf(users.values());
    }

    @Override
    public Optional<AppUser> findById(AppUserId userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public Optional<AppUser> findByExternalIdentity(ExternalIdentity externalIdentity) {
        return users.values().stream()
                .filter(user -> user.externalIdentity().equals(externalIdentity))
                .findFirst();
    }

    @Override
    public AppUser save(AppUser user) {
        AppUser current = users.get(user.id());
        AppUser stored = current == null
                ? user
                : new AppUser(
                        user.id(),
                        user.externalIdentity(),
                        user.displayName(),
                        user.status(),
                        current.version() + 1);
        users.put(stored.id(), stored);
        return stored;
    }

    @Override
    public List<AppUser> findAssignedUsers() {
        return assignments.stream()
                .map(AppUserSystemRoleAssignment::userId)
                .distinct()
                .map(users::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    public List<SystemRole> findAllRoles() {
        return List.copyOf(roles.values());
    }

    @Override
    public List<AppUserSystemRoleAssignment> findAllAssignments() {
        return List.copyOf(assignments);
    }

    @Override
    public List<SystemRolePermissionGrant> findAllPermissionGrants() {
        return List.copyOf(grants);
    }

    @Override
    public Optional<SystemRole> findRoleById(SystemRoleId roleId) {
        return Optional.ofNullable(roles.get(roleId));
    }

    @Override
    public Optional<SystemRole> findRoleByCode(SystemRoleCode roleCode) {
        return roles.values().stream().filter(role -> role.code().equals(roleCode)).findFirst();
    }

    @Override
    public Optional<AppUserSystemRoleAssignment> findAssignment(
            AppUserId userId,
            SystemRoleId roleId) {
        return assignments.stream()
                .filter(value -> value.userId().equals(userId) && value.roleId().equals(roleId))
                .findFirst();
    }

    @Override
    public Optional<SystemRolePermissionGrant> findPermissionGrant(
            SystemRoleId roleId,
            SystemPermission permission) {
        return grants.stream()
                .filter(value -> value.roleId().equals(roleId)
                        && value.permission().equals(permission))
                .findFirst();
    }

    @Override
    public SystemRole saveRole(SystemRole role) {
        SystemRole current = roles.get(role.id());
        SystemRole stored = current == null
                ? role
                : new SystemRole(
                        role.id(),
                        role.code(),
                        role.displayName(),
                        role.status(),
                        current.version() + 1);
        roles.put(stored.id(), stored);
        return stored;
    }

    @Override
    public AppUserSystemRoleAssignment saveAssignment(AppUserSystemRoleAssignment assignment) {
        if (!assignments.contains(assignment)) {
            assignments.add(assignment);
        }
        return assignment;
    }

    @Override
    public SystemRolePermissionGrant savePermissionGrant(SystemRolePermissionGrant grant) {
        if (!grants.contains(grant)) {
            grants.add(grant);
        }
        return grant;
    }

    @Override
    public void removeAssignment(AppUserSystemRoleAssignment assignment) {
        assignments.remove(assignment);
    }

    @Override
    public void removePermissionGrant(SystemRolePermissionGrant grant) {
        grants.remove(grant);
    }
}
