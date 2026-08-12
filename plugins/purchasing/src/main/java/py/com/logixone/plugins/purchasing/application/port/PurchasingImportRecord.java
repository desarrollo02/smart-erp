package py.com.logixone.plugins.purchasing.application.port;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;

public record PurchasingImportRecord(
        CompanyId companyId,
        String sourceSystem,
        String sourceRecordKey,
        Optional<String> batchChecksum,
        String requestFingerprint,
        String documentType,
        UUID documentId,
        Instant importedAt) {
    public PurchasingImportRecord {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(sourceSystem, "sourceSystem");
        Objects.requireNonNull(sourceRecordKey, "sourceRecordKey");
        batchChecksum = Objects.requireNonNull(batchChecksum, "batchChecksum");
        Objects.requireNonNull(requestFingerprint, "requestFingerprint");
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(documentId, "documentId");
        Objects.requireNonNull(importedAt, "importedAt");
    }
}
