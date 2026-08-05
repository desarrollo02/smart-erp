package py.com.logixone.kernel.domain.security.system;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.domain.security.AppUser;

/** Ensures a desired global-authority snapshot retains an effective administrator. */
public final class SystemAuthoritySafetyPolicy {

    public SystemAuthoritySafetyStatus evaluate(
            Collection<AppUser> users,
            Collection<SystemRole> roles,
            Collection<AppUserSystemRoleAssignment> assignments,
            Collection<SystemRolePermissionGrant> grants) {
        List<AppUser> userList = List.copyOf(Objects.requireNonNull(users, "users"));
        List<SystemRole> roleList = List.copyOf(Objects.requireNonNull(roles, "roles"));
        List<AppUserSystemRoleAssignment> assignmentList =
                List.copyOf(Objects.requireNonNull(assignments, "assignments"));
        List<SystemRolePermissionGrant> grantList =
                List.copyOf(Objects.requireNonNull(grants, "grants"));

        Map<AppUserId, AppUser> usersById = new HashMap<>();
        for (AppUser user : userList) {
            AppUser duplicate = usersById.putIfAbsent(user.id(), user);
            if (duplicate != null && !duplicate.equals(user)) {
                return SystemAuthoritySafetyStatus.INVALID_CONTEXT;
            }
        }

        Map<SystemRoleId, SystemRole> rolesById = new HashMap<>();
        for (SystemRole role : roleList) {
            SystemRole duplicate = rolesById.putIfAbsent(role.id(), role);
            if (duplicate != null && !duplicate.equals(role)) {
                return SystemAuthoritySafetyStatus.INVALID_CONTEXT;
            }
        }

        Set<SystemRoleId> administratorRoles = new HashSet<>();
        for (SystemRolePermissionGrant grant : grantList) {
            SystemRole role = rolesById.get(grant.roleId());
            if (role == null) {
                return SystemAuthoritySafetyStatus.INVALID_CONTEXT;
            }
            if (role.isActive()
                    && grant.permission().equals(SystemPermission.SYSTEM_ADMINISTRATION_MANAGE)) {
                administratorRoles.add(role.id());
            }
        }

        boolean administratorPresent = false;
        for (AppUserSystemRoleAssignment assignment : assignmentList) {
            AppUser user = usersById.get(assignment.userId());
            if (user == null || !rolesById.containsKey(assignment.roleId())) {
                return SystemAuthoritySafetyStatus.INVALID_CONTEXT;
            }
            if (user.isActive() && administratorRoles.contains(assignment.roleId())) {
                administratorPresent = true;
            }
        }

        return administratorPresent
                ? SystemAuthoritySafetyStatus.SAFE
                : SystemAuthoritySafetyStatus.ADMINISTRATOR_REQUIRED;
    }
}
