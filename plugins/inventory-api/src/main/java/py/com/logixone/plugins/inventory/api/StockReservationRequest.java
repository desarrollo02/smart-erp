package py.com.logixone.plugins.inventory.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Idempotent request to reserve available quantity for an external source. */
public record StockReservationRequest(
        StockKey key,
        BigDecimal quantity,
        StockSourceReference source,
        Instant expiresAt,
        String idempotencyKey) {

    public StockReservationRequest {
        Objects.requireNonNull(key, "key");
        quantity = ContractValues.positiveQuantity(quantity, "quantity");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(expiresAt, "expiresAt");
        idempotencyKey = ContractValues.key(idempotencyKey, "idempotencyKey", 160);
    }
}
