package py.com.logixone.plugins.inventory.api;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Complete identity of a balance bucket; warehouse and location are never null. */
public record StockKey(
        InventoryItemId inventoryItemId,
        WarehouseId warehouseId,
        StockLocationId locationId,
        Optional<String> lotCode,
        Optional<String> serialNumber,
        Optional<LocalDate> expiryDate,
        StockCondition condition) {

    public StockKey {
        Objects.requireNonNull(inventoryItemId, "inventoryItemId");
        Objects.requireNonNull(warehouseId, "warehouseId");
        Objects.requireNonNull(locationId, "locationId");
        lotCode = normalize(lotCode, "lotCode", 80);
        serialNumber = normalize(serialNumber, "serialNumber", 120);
        expiryDate = Objects.requireNonNull(expiryDate, "expiryDate");
        Objects.requireNonNull(condition, "condition");
    }

    private static Optional<String> normalize(Optional<String> value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        return value.map(text -> ContractValues.text(text, name, maxLength));
    }
}
