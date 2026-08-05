package py.com.logixone.kernel.application.security;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.CompanyMembership;
import py.com.logixone.kernel.domain.security.CompanyRole;
import py.com.logixone.kernel.domain.security.MembershipRoleAssignment;
import py.com.logixone.kernel.domain.security.RolePermissionGrant;

public record SecurityBootstrapState(
        AppUser user,
        CompanyMembership membership,
        CompanyRole role,
        MembershipRoleAssignment assignment,
        Set<RolePermissionGrant> permissionGrants) {

    public SecurityBootstrapState {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(membership, "membership");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(assignment, "assignment");
        permissionGrants = immutableGrants(permissionGrants);
    }

    private static Set<RolePermissionGrant> immutableGrants(Set<RolePermissionGrant> grants) {
        Objects.requireNonNull(grants, "permissionGrants");
        LinkedHashSet<RolePermissionGrant> ordered = grants.stream()
                .map(grant -> Objects.requireNonNull(grant, "permission grant"))
                .sorted(Comparator.comparing(grant -> grant.permissionId().value()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(ordered);
    }
}
