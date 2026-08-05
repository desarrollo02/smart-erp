package py.com.logixone.kernel.api.security;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;

/**
 * Minimal server-session reference. It is historical state, never current proof of
 * membership or authorization, and must be revalidated on every use.
 */
public record CompanySessionReference(
        AppUserId userId,
        CompanyId companyId) {

    public CompanySessionReference {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(companyId, "companyId");
    }
}
