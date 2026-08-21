package py.com.logixone.plugins.inventory.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Reservation addressed by catalog identity; Inventory resolves its private item. */
public record CatalogStockReservationRequest(
        UUID catalogItemId, WarehouseId warehouseId, StockLocationId locationId,
        Optional<String> lotCode, Optional<String> serialNumber,
        Optional<LocalDate> expiryDate, StockCondition condition,
        BigDecimal quantity, StockSourceReference source, Instant expiresAt,
        String idempotencyKey) {

    public CatalogStockReservationRequest {
        Objects.requireNonNull(catalogItemId, "catalogItemId");
        Objects.requireNonNull(warehouseId, "warehouseId");
        Objects.requireNonNull(locationId, "locationId");
        lotCode = normalize(lotCode, "lotCode", 80);
        serialNumber = normalize(serialNumber, "serialNumber", 120);
        expiryDate = Objects.requireNonNull(expiryDate, "expiryDate");
        Objects.requireNonNull(condition, "condition");
        quantity = ContractValues.positiveQuantity(quantity, "quantity");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(expiresAt, "expiresAt");
        idempotencyKey = ContractValues.key(idempotencyKey, "idempotencyKey", 160);
    }

    public StockReservationRequest resolve(InventoryItemId inventoryItemId) {
        return new StockReservationRequest(
                new StockKey(inventoryItemId, warehouseId, locationId, lotCode,
                        serialNumber, expiryDate, condition),
                quantity, source, expiresAt, idempotencyKey);
    }

    private static Optional<String> normalize(
            Optional<String> value, String field, int maximumLength) {
        Objects.requireNonNull(value, field);
        return value.map(text -> ContractValues.text(text, field, maximumLength));
    }
}
