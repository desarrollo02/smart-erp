package py.com.logixone.plugins.inventory.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Public reservation projection with explicit lifecycle quantities. */
public record StockReservationReference(
        StockReservationId id,
        StockKey key,
        BigDecimal originalQuantity,
        BigDecimal consumedQuantity,
        BigDecimal releasedQuantity,
        BigDecimal remainingQuantity,
        StockReservationState state,
        StockSourceReference source,
        Instant expiresAt,
        long version) {

    public StockReservationReference {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(key, "key");
        originalQuantity = ContractValues.positiveQuantity(originalQuantity, "originalQuantity");
        consumedQuantity = ContractValues.nonNegativeQuantity(consumedQuantity, "consumedQuantity");
        releasedQuantity = ContractValues.nonNegativeQuantity(releasedQuantity, "releasedQuantity");
        remainingQuantity = ContractValues.nonNegativeQuantity(remainingQuantity, "remainingQuantity");
        if (consumedQuantity.add(releasedQuantity).add(remainingQuantity).compareTo(originalQuantity) != 0) {
            throw new IllegalArgumentException("reservation quantities must reconcile to originalQuantity");
        }
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
