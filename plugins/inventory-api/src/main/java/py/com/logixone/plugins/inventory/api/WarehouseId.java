package py.com.logixone.plugins.inventory.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque warehouse identity. */
public record WarehouseId(UUID value) implements Comparable<WarehouseId> {
    public WarehouseId {
        Objects.requireNonNull(value, "value");
    }

    public static WarehouseId parse(String value) {
        return new WarehouseId(ContractValues.canonicalUuid(value, "Warehouse id"));
    }

    @Override
    public int compareTo(WarehouseId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
