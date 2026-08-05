package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.inventory.api.MovementQuantity;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockMovementDirection;
import py.com.logixone.plugins.inventory.api.StockMovementLine;
import py.com.logixone.plugins.inventory.domain.StockMovementLineSnapshot;

@Entity
@Table(name = "stock_movement_line", schema = InventoryPersistenceNames.SCHEMA)
@IdClass(StockMovementLineEntity.Key.class)
public class StockMovementLineEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "stock_movement_id", nullable = false, updatable = false)
    private UUID stockMovementId;
    @Id @Column(name = "line_number", nullable = false, updatable = false)
    private int lineNumber;
    @Column(name = "inventory_item_id", nullable = false, updatable = false)
    private UUID inventoryItemId;
    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private UUID warehouseId;
    @Column(name = "stock_location_id", nullable = false, updatable = false)
    private UUID stockLocationId;
    @Column(name = "lot_code", length = 80, updatable = false)
    private String lotCode;
    @Column(name = "serial_number", length = 120, updatable = false)
    private String serialNumber;
    @Column(name = "expiry_date", updatable = false)
    private LocalDate expiryDate;
    @Enumerated(EnumType.STRING) @Column(name = "condition_code", nullable = false, length = 24, updatable = false)
    private StockCondition condition;
    @Enumerated(EnumType.STRING) @Column(name = "movement_direction", nullable = false, length = 16, updatable = false)
    private StockMovementDirection direction;
    @Column(name = "catalog_item_id_snapshot", nullable = false, updatable = false)
    private UUID catalogItemId;
    @Column(name = "catalog_code_snapshot", nullable = false, length = 64, updatable = false)
    private String catalogCode;
    @Column(name = "catalog_name_snapshot", nullable = false, length = 240, updatable = false)
    private String catalogName;
    @Column(name = "presented_unit_code", nullable = false, length = 16, updatable = false)
    private String presentedUnitCode;
    @Column(name = "presented_quantity", nullable = false, precision = 30, scale = 6, updatable = false)
    private BigDecimal presentedQuantity;
    @Column(name = "base_unit_code", nullable = false, length = 16, updatable = false)
    private String baseUnitCode;
    @Column(name = "conversion_factor", nullable = false, precision = 30, scale = 12, updatable = false)
    private BigDecimal conversionFactor;
    @Column(name = "base_quantity", nullable = false, precision = 30, scale = 6, updatable = false)
    private BigDecimal baseQuantity;
    @Column(name = "catalog_item_version", nullable = false, updatable = false)
    private long catalogItemVersion;

    protected StockMovementLineEntity() {
    }

    static StockMovementLineEntity from(UUID companyId, UUID movementId, StockMovementLineSnapshot snapshot) {
        StockMovementLineEntity entity = new StockMovementLineEntity();
        StockMovementLine line = snapshot.line();
        MovementQuantity quantity = line.quantity();
        entity.companyId = companyId;
        entity.stockMovementId = movementId;
        entity.lineNumber = snapshot.lineNumber();
        entity.inventoryItemId = line.key().inventoryItemId().value();
        entity.warehouseId = line.key().warehouseId().value();
        entity.stockLocationId = line.key().locationId().value();
        entity.lotCode = line.key().lotCode().orElse(null);
        entity.serialNumber = line.key().serialNumber().orElse(null);
        entity.expiryDate = line.key().expiryDate().orElse(null);
        entity.condition = line.key().condition();
        entity.direction = line.direction();
        entity.catalogItemId = snapshot.catalogItemId().value();
        entity.catalogCode = snapshot.catalogCode();
        entity.catalogName = snapshot.catalogName();
        entity.presentedUnitCode = quantity.presentedUnitCode();
        entity.presentedQuantity = quantity.presentedQuantity();
        entity.baseUnitCode = quantity.baseUnitCode();
        entity.conversionFactor = quantity.conversionFactor();
        entity.baseQuantity = quantity.baseQuantity();
        entity.catalogItemVersion = quantity.catalogItemVersion();
        return entity;
    }

    StockMovementLineSnapshot snapshot() {
        return new StockMovementLineSnapshot(
                lineNumber,
                new StockMovementLine(
                        InventoryPersistenceValues.stockKey(
                                inventoryItemId, warehouseId, stockLocationId, lotCode,
                                serialNumber, expiryDate, condition),
                        direction,
                        new MovementQuantity(
                                presentedUnitCode, presentedQuantity, baseUnitCode,
                                conversionFactor, baseQuantity, catalogItemVersion)),
                new CatalogItemId(catalogItemId), catalogCode, catalogName);
    }

    UUID companyId() { return companyId; }
    UUID stockMovementId() { return stockMovementId; }
    int lineNumber() { return lineNumber; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID stockMovementId;
        public int lineNumber;
        public Key() { }
        Key(UUID companyId, UUID stockMovementId, int lineNumber) { this.companyId = companyId; this.stockMovementId = stockMovementId; this.lineNumber = lineNumber; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && lineNumber == that.lineNumber && Objects.equals(companyId, that.companyId) && Objects.equals(stockMovementId, that.stockMovementId); }
        @Override public int hashCode() { return Objects.hash(companyId, stockMovementId, lineNumber); }
    }
}
