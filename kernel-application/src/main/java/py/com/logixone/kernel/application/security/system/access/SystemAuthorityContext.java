package py.com.logixone.kernel.application.security.system.access;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;

/** Current, request-scoped authority projection for one local user. */
public record SystemAuthorityContext(
        AppUserId actorUserId,
        Set<SystemPermission> permissions) {

    public SystemAuthorityContext {
        Objects.requireNonNull(actorUserId, "actorUserId");
        permissions = Collections.unmodifiableSet(
                new TreeSet<>(Objects.requireNonNull(permissions, "permissions")));
        if (permissions.isEmpty()) {
            throw new IllegalArgumentException("system authority context requires a permission");
        }
        if (!SystemPermission.knownPermissions().containsAll(permissions)) {
            throw new IllegalArgumentException("system authority context contains unknown permissions");
        }
    }

    public boolean hasPermission(SystemPermission permission) {
        return permissions.contains(Objects.requireNonNull(permission, "permission"));
    }
}
