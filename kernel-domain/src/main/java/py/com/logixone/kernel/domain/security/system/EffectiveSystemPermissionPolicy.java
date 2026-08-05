package py.com.logixone.kernel.domain.security.system;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.domain.security.AppUser;

/** Fail-closed resolution of kernel-wide permissions for one active user. */
public final class EffectiveSystemPermissionPolicy {

    public EffectiveSystemPermissionResolution resolve(
            AppUser user,
            Collection<SystemRole> roles,
            Collection<AppUserSystemRoleAssignment> assignments,
            Collection<SystemRolePermissionGrant> grants,
            Collection<SystemPermission> availablePermissions) {
        Objects.requireNonNull(user, "user");
        List<SystemRole> roleList = List.copyOf(Objects.requireNonNull(roles, "roles"));
        List<AppUserSystemRoleAssignment> assignmentList =
                List.copyOf(Objects.requireNonNull(assignments, "assignments"));
        List<SystemRolePermissionGrant> grantList =
                List.copyOf(Objects.requireNonNull(grants, "grants"));
        Set<SystemPermission> available = Set.copyOf(
                Objects.requireNonNull(availablePermissions, "availablePermissions"));

        if (!user.isActive()) {
            return EffectiveSystemPermissionResolution.denied(
                    user.id(),
                    SystemSecurityDiagnosticCode.USER_INACTIVE);
        }

        Map<SystemRoleId, SystemRole> rolesById = new HashMap<>();
        for (SystemRole role : roleList) {
            SystemRole duplicate = rolesById.putIfAbsent(role.id(), role);
            if (duplicate != null && !duplicate.equals(role)) {
                return invalid(user);
            }
        }

        Set<SystemRoleId> assignedActiveRoles = new HashSet<>();
        for (AppUserSystemRoleAssignment assignment : assignmentList) {
            if (!assignment.userId().equals(user.id())) {
                return invalid(user);
            }
            SystemRole role = rolesById.get(assignment.roleId());
            if (role == null) {
                return invalid(user);
            }
            if (role.isActive()) {
                assignedActiveRoles.add(role.id());
            }
        }

        Set<SystemPermission> effective = new TreeSet<>();
        for (SystemRolePermissionGrant grant : grantList) {
            if (!rolesById.containsKey(grant.roleId())) {
                return invalid(user);
            }
            if (assignedActiveRoles.contains(grant.roleId())
                    && available.contains(grant.permission())) {
                effective.add(grant.permission());
            }
        }

        return EffectiveSystemPermissionResolution.granted(user.id(), effective);
    }

    private static EffectiveSystemPermissionResolution invalid(AppUser user) {
        return EffectiveSystemPermissionResolution.denied(
                user.id(),
                SystemSecurityDiagnosticCode.SYSTEM_ROLE_CONTEXT_INVALID);
    }
}
