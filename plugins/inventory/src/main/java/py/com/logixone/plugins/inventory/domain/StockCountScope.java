package py.com.logixone.plugins.inventory.domain;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;

/** Warehouse-wide or location-specific lock scope for a physical count. */
public record StockCountScope(WarehouseId warehouseId, Optional<StockLocationId> locationId) {
    public StockCountScope {
        Objects.requireNonNull(warehouseId, "warehouseId");
        locationId = Objects.requireNonNull(locationId, "locationId");
    }

    public boolean contains(StockKey key) {
        Objects.requireNonNull(key, "key");
        return warehouseId.equals(key.warehouseId())
                && locationId.map(id -> id.equals(key.locationId())).orElse(true);
    }
}
