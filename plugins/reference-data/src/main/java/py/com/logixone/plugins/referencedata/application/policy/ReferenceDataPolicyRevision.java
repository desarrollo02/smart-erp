package py.com.logixone.plugins.referencedata.application.policy;

import java.time.Instant;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;

/** Append-only business history of one enterprise policy. */
public record ReferenceDataPolicyRevision(
        CompanyId companyId,
        ReferenceDataCatalog catalog,
        String code,
        boolean enabled,
        long version,
        AppUserId actorUserId,
        String correlationId,
        Instant changedAt) {

    public ReferenceDataPolicyRevision {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(catalog, "catalog");
        code = ReferenceDataPolicy.canonicalCode(catalog, code);
        if (version < 1) {
            throw new IllegalArgumentException("revision version must be positive");
        }
        Objects.requireNonNull(actorUserId, "actorUserId");
        correlationId = Objects.requireNonNull(correlationId, "correlationId").strip();
        if (!correlationId.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")) {
            throw new IllegalArgumentException("Invalid correlationId");
        }
        Objects.requireNonNull(changedAt, "changedAt");
    }
}
