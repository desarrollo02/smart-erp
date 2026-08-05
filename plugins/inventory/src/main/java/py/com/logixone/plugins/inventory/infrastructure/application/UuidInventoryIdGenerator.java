package py.com.logixone.plugins.inventory.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.port.InventoryIdGenerator;

@ApplicationScoped
public class UuidInventoryIdGenerator implements InventoryIdGenerator {
    @Override public WarehouseId nextWarehouseId() { return new WarehouseId(UUID.randomUUID()); }
    @Override public StockLocationId nextLocationId() { return new StockLocationId(UUID.randomUUID()); }
    @Override public InventoryItemId nextItemId() { return new InventoryItemId(UUID.randomUUID()); }
    @Override public StockMovementId nextMovementId() { return new StockMovementId(UUID.randomUUID()); }
    @Override public StockReservationId nextReservationId() { return new StockReservationId(UUID.randomUUID()); }
    @Override public StockCountId nextCountId() { return new StockCountId(UUID.randomUUID()); }
}
