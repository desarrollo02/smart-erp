package py.com.logixone.plugins.inventory.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of a stock reservation. */
public record StockReservationId(UUID value) {
    public StockReservationId {
        Objects.requireNonNull(value, "value");
    }

    public static StockReservationId parse(String value) {
        return new StockReservationId(ContractValues.canonicalUuid(value, "Stock reservation id"));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
