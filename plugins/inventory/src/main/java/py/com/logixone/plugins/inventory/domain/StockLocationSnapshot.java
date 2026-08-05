package py.com.logixone.plugins.inventory.domain;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;

public record StockLocationSnapshot(
        CompanyId companyId,
        WarehouseId warehouseId,
        StockLocationId id,
        String code,
        String name,
        StockLocationType type,
        boolean active,
        long version) {
    public StockLocationSnapshot {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(warehouseId, "warehouseId");
        Objects.requireNonNull(id, "id");
        code = InventoryValues.code(code, "code", 64);
        name = InventoryValues.text(name, "name", 160);
        Objects.requireNonNull(type, "type");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
