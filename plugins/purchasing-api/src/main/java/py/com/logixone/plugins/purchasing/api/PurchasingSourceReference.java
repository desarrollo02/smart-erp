package py.com.logixone.plugins.purchasing.api;

import java.util.Objects;
import java.util.Optional;

/** Immutable provenance used for idempotent migration imports. */
public record PurchasingSourceReference(
        String sourceSystem,
        String sourceRecordKey,
        Optional<String> batchChecksum) {

    public PurchasingSourceReference {
        sourceSystem = ContractValues.code(sourceSystem, "sourceSystem", 64);
        sourceRecordKey = ContractValues.text(sourceRecordKey, "sourceRecordKey", 160);
        batchChecksum = Objects.requireNonNull(batchChecksum, "batchChecksum")
                .map(value -> ContractValues.text(value, "batchChecksum", 64));
        if (batchChecksum.isPresent() && !batchChecksum.get().matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("batchChecksum must be a lower-case SHA-256");
        }
    }
}
