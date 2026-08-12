package py.com.logixone.plugins.purchasing.application.port;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;

public record PurchasingOperationRecord(
        CompanyId companyId,
        String idempotencyKey,
        String operationType,
        String requestFingerprint,
        String resourceType,
        UUID resourceId,
        long resultingVersion,
        Instant occurredAt) {
    public PurchasingOperationRecord {
        Objects.requireNonNull(companyId, "companyId");
        idempotencyKey = text(idempotencyKey, "idempotencyKey", 160);
        operationType = text(operationType, "operationType", 64);
        requestFingerprint = text(requestFingerprint, "requestFingerprint", 64);
        resourceType = text(resourceType, "resourceType", 64);
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (!requestFingerprint.matches("[0-9a-f]{64}") || resultingVersion < 0) {
            throw new IllegalArgumentException("Invalid operation fingerprint or version");
        }
    }

    private static String text(String value, String field, int maximum) {
        value = Objects.requireNonNull(value, field).strip();
        if (value.isEmpty() || value.length() > maximum) {
            throw new IllegalArgumentException("Invalid " + field);
        }
        return value;
    }
}
