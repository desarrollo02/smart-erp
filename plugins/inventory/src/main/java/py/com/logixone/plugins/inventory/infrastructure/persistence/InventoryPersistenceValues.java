package py.com.logixone.plugins.inventory.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;

final class InventoryPersistenceValues {
    private InventoryPersistenceValues() {
    }

    static StockKey stockKey(
            UUID inventoryItemId,
            UUID warehouseId,
            UUID stockLocationId,
            String lotCode,
            String serialNumber,
            LocalDate expiryDate,
            StockCondition condition) {
        return new StockKey(
                new InventoryItemId(inventoryItemId), new WarehouseId(warehouseId),
                new StockLocationId(stockLocationId), Optional.ofNullable(lotCode),
                Optional.ofNullable(serialNumber), Optional.ofNullable(expiryDate), condition);
    }
}
