package py.com.logixone.plugins.businesspartners.domain;

import java.time.Instant;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;

/** Immutable historical snapshot of a company-owned selector definition. */
public record BusinessPartnerDefinitionRevision(
        CompanyId companyId,
        BusinessPartnerDefinitionKind kind,
        BusinessPartnerAttributeCode code,
        BusinessPartnerName displayName,
        BusinessPartnerState state,
        long version,
        Instant changedAt) {

    public BusinessPartnerDefinitionRevision {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(changedAt, "changedAt");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
