package py.com.logixone.plugins.inventory.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of a location inside one warehouse. */
public record StockLocationId(UUID value) implements Comparable<StockLocationId> {
    public StockLocationId {
        Objects.requireNonNull(value, "value");
    }

    public static StockLocationId parse(String value) {
        return new StockLocationId(ContractValues.canonicalUuid(value, "Stock location id"));
    }

    @Override
    public int compareTo(StockLocationId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
