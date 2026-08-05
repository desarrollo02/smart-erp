package py.com.logixone.kernel.domain.security;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;

/** Versioned local authorization for an application user to operate in one company. */
public record CompanyMembership(
        AppUserId userId,
        CompanyId companyId,
        MembershipStatus status,
        long version) {

    public CompanyMembership {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(status, "status");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE;
    }
}
