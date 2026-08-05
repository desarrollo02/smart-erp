package py.com.logixone.kernel.api.security;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyContext;
import py.com.logixone.kernel.api.company.CompanyId;

/** Company scope paired with an authenticated actor after server-side membership validation. */
public record AuthenticatedCompanyContext(
        AuthenticatedActor actor,
        CompanyId companyId) implements CompanyContext {

    public AuthenticatedCompanyContext {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(companyId, "companyId");
    }

    @Override
    public CompanyId requiredCompanyId() {
        return companyId;
    }
}
