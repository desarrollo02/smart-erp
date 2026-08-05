package py.com.logixone.plugins.inventory.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of an immutable stock movement. */
public record StockMovementId(UUID value) {
    public StockMovementId {
        Objects.requireNonNull(value, "value");
    }

    public static StockMovementId parse(String value) {
        return new StockMovementId(ContractValues.canonicalUuid(value, "Stock movement id"));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
