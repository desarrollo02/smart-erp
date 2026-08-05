package py.com.logixone.plugins.inventory.domain;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.MovementQuantity;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;

final class InventoryDomainFixtures {
    private InventoryDomainFixtures() {
    }

    static CompanyId companyId() { return new CompanyId(uuid(1)); }
    static InventoryItemId itemId() { return new InventoryItemId(uuid(2)); }
    static WarehouseId warehouseId(long suffix) { return new WarehouseId(uuid(suffix)); }
    static StockLocationId locationId(long suffix) { return new StockLocationId(uuid(suffix)); }

    static StockKey key(long warehouse, long location) {
        return new StockKey(
                itemId(), warehouseId(warehouse), locationId(location), Optional.of("LOT-A"),
                Optional.empty(), Optional.empty(), StockCondition.AVAILABLE);
    }

    static MovementQuantity quantity(String value) {
        return new MovementQuantity(
                "EA", new BigDecimal(value), "EA", BigDecimal.ONE, new BigDecimal(value), 3);
    }

    static UUID uuid(long suffix) { return new UUID(0, suffix); }
}
