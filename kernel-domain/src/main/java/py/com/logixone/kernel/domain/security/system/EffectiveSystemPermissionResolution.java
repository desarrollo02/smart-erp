package py.com.logixone.kernel.domain.security.system;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;

/** Kernel-wide permissions effective for one local application user. */
public record EffectiveSystemPermissionResolution(
        AppUserId userId,
        Set<SystemPermission> permissions,
        Optional<SystemSecurityDiagnosticCode> failure) {

    public EffectiveSystemPermissionResolution {
        Objects.requireNonNull(userId, "userId");
        permissions = Collections.unmodifiableSet(
                new TreeSet<>(Objects.requireNonNull(permissions, "permissions")));
        failure = Objects.requireNonNull(failure, "failure");
        if (failure.isPresent() && !permissions.isEmpty()) {
            throw new IllegalArgumentException(
                    "a denied system permission result cannot expose permissions");
        }
    }

    public static EffectiveSystemPermissionResolution granted(
            AppUserId userId,
            Set<SystemPermission> permissions) {
        return new EffectiveSystemPermissionResolution(
                userId,
                permissions,
                Optional.empty());
    }

    public static EffectiveSystemPermissionResolution denied(
            AppUserId userId,
            SystemSecurityDiagnosticCode failure) {
        return new EffectiveSystemPermissionResolution(
                userId,
                Set.of(),
                Optional.of(Objects.requireNonNull(failure, "failure")));
    }

    public boolean authorized() {
        return failure.isEmpty();
    }
}
