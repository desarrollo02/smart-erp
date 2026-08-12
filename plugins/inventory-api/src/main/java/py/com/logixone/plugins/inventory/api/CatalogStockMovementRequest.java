package py.com.logixone.plugins.inventory.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Idempotent movement addressed by the public catalog identity.
 * Inventory remains responsible for resolving its private enrolled item id.
 */
public record CatalogStockMovementRequest(
        StockMovementType type,
        String reasonCode,
        StockSourceReference source,
        String idempotencyKey,
        UUID catalogItemId,
        WarehouseId warehouseId,
        StockLocationId locationId,
        Optional<String> lotCode,
        Optional<String> serialNumber,
        Optional<LocalDate> expiryDate,
        StockCondition condition,
        MovementQuantity quantity) {

    public CatalogStockMovementRequest {
        Objects.requireNonNull(type, "type");
        if (type != StockMovementType.RECEIPT && type != StockMovementType.ISSUE) {
            throw new IllegalArgumentException("Catalog movements support only RECEIPT or ISSUE");
        }
        reasonCode = ContractValues.code(reasonCode, "reasonCode", 64);
        Objects.requireNonNull(source, "source");
        idempotencyKey = ContractValues.key(idempotencyKey, "idempotencyKey", 160);
        Objects.requireNonNull(catalogItemId, "catalogItemId");
        Objects.requireNonNull(warehouseId, "warehouseId");
        Objects.requireNonNull(locationId, "locationId");
        lotCode = normalize(lotCode, "lotCode", 80);
        serialNumber = normalize(serialNumber, "serialNumber", 120);
        expiryDate = Objects.requireNonNull(expiryDate, "expiryDate");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(quantity, "quantity");
        if (quantity.presentedQuantity().multiply(quantity.conversionFactor())
                .compareTo(quantity.baseQuantity()) != 0) {
            throw new IllegalArgumentException(
                    "baseQuantity must equal presentedQuantity multiplied by conversionFactor");
        }
    }

    public StockMovementRequest resolve(InventoryItemId inventoryItemId) {
        StockMovementDirection direction = type == StockMovementType.RECEIPT
                ? StockMovementDirection.INCREASE : StockMovementDirection.DECREASE;
        return new StockMovementRequest(
                type,
                reasonCode,
                source,
                idempotencyKey,
                List.of(new StockMovementLine(
                        new StockKey(
                                inventoryItemId,
                                warehouseId,
                                locationId,
                                lotCode,
                                serialNumber,
                                expiryDate,
                                condition),
                        direction,
                        quantity)),
                Optional.empty());
    }

    private static Optional<String> normalize(
            Optional<String> value, String field, int maximumLength) {
        Objects.requireNonNull(value, field);
        return value.map(text -> ContractValues.text(text, field, maximumLength));
    }
}
