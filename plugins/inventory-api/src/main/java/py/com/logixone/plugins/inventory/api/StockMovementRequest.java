package py.com.logixone.plugins.inventory.api;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Idempotent request to append one movement to the inventory ledger. */
public record StockMovementRequest(
        StockMovementType type,
        String reasonCode,
        StockSourceReference source,
        String idempotencyKey,
        List<StockMovementLine> lines,
        Optional<StockMovementId> reversalOf) {

    public StockMovementRequest {
        Objects.requireNonNull(type, "type");
        reasonCode = ContractValues.code(reasonCode, "reasonCode", 64);
        Objects.requireNonNull(source, "source");
        idempotencyKey = ContractValues.key(idempotencyKey, "idempotencyKey", 160);
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("lines must not be empty");
        }
        reversalOf = Objects.requireNonNull(reversalOf, "reversalOf");
        if ((type == StockMovementType.REVERSAL) != reversalOf.isPresent()) {
            throw new IllegalArgumentException("reversalOf is required only for REVERSAL movements");
        }
    }
}
