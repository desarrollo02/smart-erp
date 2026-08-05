package py.com.logixone.kernel.domain.security;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugin.api.ContributionId;

/** Granted permissions after membership, role and currently available contribution filtering. */
public record EffectivePermissionResolution(
        CompanyId companyId,
        Set<ContributionId> permissions,
        Optional<SecurityDiagnosticCode> failure) {

    public EffectivePermissionResolution {
        Objects.requireNonNull(companyId, "companyId");
        permissions = Collections.unmodifiableSet(
                new TreeSet<>(Objects.requireNonNull(permissions, "permissions")));
        failure = Objects.requireNonNull(failure, "failure");
        if (failure.isPresent() && !permissions.isEmpty()) {
            throw new IllegalArgumentException("a denied permission result cannot expose permissions");
        }
    }

    public static EffectivePermissionResolution granted(
            CompanyId companyId,
            Set<ContributionId> permissions) {
        return new EffectivePermissionResolution(companyId, permissions, Optional.empty());
    }

    public static EffectivePermissionResolution denied(
            CompanyId companyId,
            SecurityDiagnosticCode failure) {
        return new EffectivePermissionResolution(
                companyId,
                Set.of(),
                Optional.of(Objects.requireNonNull(failure, "failure")));
    }

    public boolean authorized() {
        return failure.isEmpty();
    }
}
