package py.com.logixone.plugins.inventory.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable public projection returned after posting. */
public record StockMovementReference(
        StockMovementId id,
        StockMovementType type,
        Instant postedAt,
        List<StockMovementLine> lines,
        Optional<StockMovementId> reversalOf) {

    public StockMovementReference {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(postedAt, "postedAt");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("lines must not be empty");
        }
        reversalOf = Objects.requireNonNull(reversalOf, "reversalOf");
    }
}
