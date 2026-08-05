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
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.domain.StockCountLineSnapshot;

@Entity
@Table(name = "stock_count_line", schema = InventoryPersistenceNames.SCHEMA)
@IdClass(StockCountLineEntity.Key.class)
public class StockCountLineEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "stock_count_id", nullable = false, updatable = false)
    private UUID stockCountId;
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
    @Column(name = "theoretical_quantity", nullable = false, precision = 30, scale = 6, updatable = false)
    private BigDecimal theoreticalQuantity;
    @Column(name = "counted_quantity", precision = 30, scale = 6)
    private BigDecimal countedQuantity;

    protected StockCountLineEntity() {
    }

    static StockCountLineEntity from(UUID companyId, UUID countId, StockCountLineSnapshot snapshot) {
        StockCountLineEntity entity = new StockCountLineEntity();
        entity.companyId = companyId;
        entity.stockCountId = countId;
        entity.lineNumber = snapshot.lineNumber();
        entity.inventoryItemId = snapshot.key().inventoryItemId().value();
        entity.warehouseId = snapshot.key().warehouseId().value();
        entity.stockLocationId = snapshot.key().locationId().value();
        entity.lotCode = snapshot.key().lotCode().orElse(null);
        entity.serialNumber = snapshot.key().serialNumber().orElse(null);
        entity.expiryDate = snapshot.key().expiryDate().orElse(null);
        entity.condition = snapshot.key().condition();
        entity.theoreticalQuantity = snapshot.theoreticalQuantity();
        entity.countedQuantity = snapshot.countedQuantity().orElse(null);
        return entity;
    }

    void apply(StockCountLineSnapshot snapshot) { countedQuantity = snapshot.countedQuantity().orElse(null); }

    StockCountLineSnapshot snapshot() {
        return new StockCountLineSnapshot(
                lineNumber,
                InventoryPersistenceValues.stockKey(
                        inventoryItemId, warehouseId, stockLocationId, lotCode,
                        serialNumber, expiryDate, condition),
                theoreticalQuantity, Optional.ofNullable(countedQuantity));
    }

    UUID companyId() { return companyId; }
    UUID stockCountId() { return stockCountId; }
    int lineNumber() { return lineNumber; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID stockCountId;
        public int lineNumber;
        public Key() { }
        Key(UUID companyId, UUID stockCountId, int lineNumber) { this.companyId = companyId; this.stockCountId = stockCountId; this.lineNumber = lineNumber; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && lineNumber == that.lineNumber && Objects.equals(companyId, that.companyId) && Objects.equals(stockCountId, that.stockCountId); }
        @Override public int hashCode() { return Objects.hash(companyId, stockCountId, lineNumber); }
    }
}
