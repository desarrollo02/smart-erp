package py.com.logixone.plugins.inventory.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of a physical stock count. */
public record StockCountId(UUID value) {
    public StockCountId {
        Objects.requireNonNull(value, "value");
    }

    public static StockCountId parse(String value) {
        return new StockCountId(ContractValues.canonicalUuid(value, "Stock count id"));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
