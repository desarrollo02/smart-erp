package py.com.logixone.plugins.inventory.api;

import java.util.Objects;
import java.util.UUID;

/** Local inventory enrollment identity, distinct from the catalog item id. */
public record InventoryItemId(UUID value) implements Comparable<InventoryItemId> {
    public InventoryItemId {
        Objects.requireNonNull(value, "value");
    }

    public static InventoryItemId parse(String value) {
        return new InventoryItemId(ContractValues.canonicalUuid(value, "Inventory item id"));
    }

    @Override
    public int compareTo(InventoryItemId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
