package py.com.logixone.plugins.inventory.api;

import java.util.List;
import java.util.Objects;

/** Public warehouse projection including its currently known locations. */
public record WarehouseReference(
        WarehouseId id,
        String code,
        String name,
        boolean active,
        long version,
        List<StockLocationReference> locations) {

    public WarehouseReference {
        Objects.requireNonNull(id, "id");
        code = ContractValues.code(code, "code", 64);
        name = ContractValues.text(name, "name", 160);
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        locations = List.copyOf(Objects.requireNonNull(locations, "locations"));
    }
}
