package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.purchasing.application.port.PurchasingOperationRecord;

@Entity
@Table(name = "purchasing_operation", schema = PurchasingPersistenceNames.SCHEMA)
@IdClass(PurchasingOperationEntity.Key.class)
public class PurchasingOperationEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "idempotency_key", nullable = false, length = 160, updatable = false)
    private String idempotencyKey;
    @Column(name = "operation_type", nullable = false, length = 64, updatable = false)
    private String operationType;
    @Column(name = "request_fingerprint", nullable = false, length = 64, updatable = false)
    private String requestFingerprint;
    @Column(name = "resource_type", nullable = false, length = 64, updatable = false)
    private String resourceType;
    @Column(name = "resource_id", nullable = false, updatable = false)
    private UUID resourceId;
    @Column(name = "resulting_version", nullable = false, updatable = false)
    private long resultingVersion;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected PurchasingOperationEntity() { }

    static PurchasingOperationEntity from(PurchasingOperationRecord record) {
        PurchasingOperationEntity entity = new PurchasingOperationEntity();
        entity.companyId = record.companyId().value();
        entity.idempotencyKey = record.idempotencyKey();
        entity.operationType = record.operationType();
        entity.requestFingerprint = record.requestFingerprint();
        entity.resourceType = record.resourceType();
        entity.resourceId = record.resourceId();
        entity.resultingVersion = record.resultingVersion();
        entity.occurredAt = record.occurredAt();
        return entity;
    }

    PurchasingOperationRecord record() {
        return new PurchasingOperationRecord(new CompanyId(companyId), idempotencyKey,
                operationType, requestFingerprint, resourceType, resourceId,
                resultingVersion, occurredAt);
    }

    public static final class Key implements Serializable {
        public UUID companyId;
        public String idempotencyKey;
        public Key() { }
        Key(UUID companyId, String idempotencyKey) {
            this.companyId = companyId;
            this.idempotencyKey = idempotencyKey;
        }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(idempotencyKey, that.idempotencyKey); }
        @Override public int hashCode() { return Objects.hash(companyId, idempotencyKey); }
    }
}
