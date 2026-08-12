package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.purchasing.application.port.PurchasingImportRecord;

@Entity
@Table(name = "purchasing_import", schema = PurchasingPersistenceNames.SCHEMA)
@IdClass(PurchasingImportEntity.Key.class)
public class PurchasingImportEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "source_system", nullable = false, length = 64, updatable = false)
    private String sourceSystem;
    @Id @Column(name = "source_record_key", nullable = false, length = 160, updatable = false)
    private String sourceRecordKey;
    @Column(name = "batch_checksum", length = 64, updatable = false)
    private String batchChecksum;
    @Column(name = "request_fingerprint", nullable = false, length = 64, updatable = false)
    private String requestFingerprint;
    @Column(name = "document_type", nullable = false, length = 32, updatable = false)
    private String documentType;
    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;
    @Column(name = "imported_at", nullable = false, updatable = false)
    private Instant importedAt;

    protected PurchasingImportEntity() { }

    static PurchasingImportEntity from(PurchasingImportRecord record) {
        PurchasingImportEntity entity = new PurchasingImportEntity();
        entity.companyId = record.companyId().value();
        entity.sourceSystem = record.sourceSystem();
        entity.sourceRecordKey = record.sourceRecordKey();
        entity.batchChecksum = record.batchChecksum().orElse(null);
        entity.requestFingerprint = record.requestFingerprint();
        entity.documentType = record.documentType();
        entity.documentId = record.documentId();
        entity.importedAt = record.importedAt();
        return entity;
    }

    PurchasingImportRecord record() {
        return new PurchasingImportRecord(new CompanyId(companyId), sourceSystem,
                sourceRecordKey, Optional.ofNullable(batchChecksum), requestFingerprint,
                documentType, documentId, importedAt);
    }

    public static final class Key implements Serializable {
        public UUID companyId;
        public String sourceSystem;
        public String sourceRecordKey;
        public Key() { }
        Key(UUID companyId, String sourceSystem, String sourceRecordKey) {
            this.companyId = companyId;
            this.sourceSystem = sourceSystem;
            this.sourceRecordKey = sourceRecordKey;
        }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(sourceSystem, that.sourceSystem) && Objects.equals(sourceRecordKey, that.sourceRecordKey); }
        @Override public int hashCode() { return Objects.hash(companyId, sourceSystem, sourceRecordKey); }
    }
}
