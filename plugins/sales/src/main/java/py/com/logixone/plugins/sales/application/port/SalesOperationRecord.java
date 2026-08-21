package py.com.logixone.plugins.sales.application.port;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;

public record SalesOperationRecord(CompanyId companyId, String idempotencyKey,
        String operationType, String fingerprint, String aggregateType,
        UUID aggregateId, Instant createdAt) {
    public SalesOperationRecord {
        Objects.requireNonNull(companyId); Objects.requireNonNull(idempotencyKey);
        Objects.requireNonNull(operationType); Objects.requireNonNull(fingerprint);
        Objects.requireNonNull(aggregateType); Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(createdAt);
    }
}
