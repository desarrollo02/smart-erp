package py.com.logixone.kernel.domain.security;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import py.com.logixone.plugin.api.ContributionId;

/** Fail-closed permission resolution against the permissions available for one company. */
public final class EffectivePermissionPolicy {

    public EffectivePermissionResolution resolve(
            AppUser user,
            CompanyMembership membership,
            Collection<CompanyRole> roles,
            Collection<MembershipRoleAssignment> assignments,
            Collection<RolePermissionGrant> grants,
            Collection<ContributionId> availablePermissions) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(membership, "membership");
        List<CompanyRole> roleList = List.copyOf(Objects.requireNonNull(roles, "roles"));
        List<MembershipRoleAssignment> assignmentList =
                List.copyOf(Objects.requireNonNull(assignments, "assignments"));
        List<RolePermissionGrant> grantList =
                List.copyOf(Objects.requireNonNull(grants, "grants"));
        Set<ContributionId> available = Set.copyOf(
                Objects.requireNonNull(availablePermissions, "availablePermissions"));

        if (!user.isActive()) {
            return EffectivePermissionResolution.denied(
                    membership.companyId(),
                    SecurityDiagnosticCode.USER_INACTIVE);
        }
        if (!membership.userId().equals(user.id())) {
            return EffectivePermissionResolution.denied(
                    membership.companyId(),
                    SecurityDiagnosticCode.MEMBERSHIP_CONTEXT_INVALID);
        }
        if (!membership.isActive()) {
            return EffectivePermissionResolution.denied(
                    membership.companyId(),
                    SecurityDiagnosticCode.MEMBERSHIP_INACTIVE);
        }

        Map<RoleId, CompanyRole> rolesById = new HashMap<>();
        for (CompanyRole role : roleList) {
            if (!role.companyId().equals(membership.companyId())) {
                return EffectivePermissionResolution.denied(
                        membership.companyId(),
                        SecurityDiagnosticCode.ROLE_CONTEXT_INVALID);
            }
            CompanyRole duplicate = rolesById.putIfAbsent(role.id(), role);
            if (duplicate != null && !duplicate.equals(role)) {
                return EffectivePermissionResolution.denied(
                        membership.companyId(),
                        SecurityDiagnosticCode.ROLE_CONTEXT_INVALID);
            }
        }

        Set<RoleId> assignedActiveRoles = new HashSet<>();
        for (MembershipRoleAssignment assignment : assignmentList) {
            if (!assignment.userId().equals(user.id())
                    || !assignment.companyId().equals(membership.companyId())) {
                return EffectivePermissionResolution.denied(
                        membership.companyId(),
                        SecurityDiagnosticCode.ROLE_CONTEXT_INVALID);
            }
            CompanyRole role = rolesById.get(assignment.roleId());
            if (role == null) {
                return EffectivePermissionResolution.denied(
                        membership.companyId(),
                        SecurityDiagnosticCode.ROLE_CONTEXT_INVALID);
            }
            if (role.isActive()) {
                assignedActiveRoles.add(role.id());
            }
        }

        Set<ContributionId> effective = new TreeSet<>();
        for (RolePermissionGrant grant : grantList) {
            if (!grant.companyId().equals(membership.companyId())
                    || !rolesById.containsKey(grant.roleId())) {
                return EffectivePermissionResolution.denied(
                        membership.companyId(),
                        SecurityDiagnosticCode.ROLE_CONTEXT_INVALID);
            }
            if (assignedActiveRoles.contains(grant.roleId())
                    && available.contains(grant.permissionId())) {
                effective.add(grant.permissionId());
            }
        }

        return EffectivePermissionResolution.granted(membership.companyId(), effective);
    }
}
