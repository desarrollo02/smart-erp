package py.com.logixone.plugins.inventory.application.port;

import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;

public interface InventoryIdGenerator {
    WarehouseId nextWarehouseId();
    StockLocationId nextLocationId();
    InventoryItemId nextItemId();
    StockMovementId nextMovementId();
    StockReservationId nextReservationId();
    StockCountId nextCountId();
}
