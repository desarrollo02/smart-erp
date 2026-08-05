package py.com.logixone.kernel.application.security.system;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.system.AppUserSystemRoleAssignment;
import py.com.logixone.kernel.domain.security.system.SystemRole;
import py.com.logixone.kernel.domain.security.system.SystemRolePermissionGrant;

/** Complete state created or verified by the closed system-authority bootstrap. */
public record SystemAuthorityBootstrapState(
        AppUser user,
        SystemRole role,
        AppUserSystemRoleAssignment assignment,
        Set<SystemRolePermissionGrant> grants) {

    public SystemAuthorityBootstrapState {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(assignment, "assignment");
        Objects.requireNonNull(grants, "grants");
        LinkedHashSet<SystemRolePermissionGrant> ordered = grants.stream()
                .sorted(Comparator.comparing(grant -> grant.permission().value()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        grants = Collections.unmodifiableSet(ordered);
        if (!assignment.userId().equals(user.id())
                || !assignment.roleId().equals(role.id())
                || grants.stream().anyMatch(grant -> !grant.roleId().equals(role.id()))) {
            throw new IllegalArgumentException("bootstrap state references are inconsistent");
        }
    }
}
