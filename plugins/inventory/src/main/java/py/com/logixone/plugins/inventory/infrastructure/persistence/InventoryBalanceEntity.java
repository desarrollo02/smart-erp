package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.domain.InventoryBalanceSnapshot;

@Entity
@Table(name = "inventory_balance", schema = InventoryPersistenceNames.SCHEMA)
@IdClass(InventoryBalanceEntity.Key.class)
public class InventoryBalanceEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "inventory_balance_id", nullable = false, updatable = false)
    private UUID inventoryBalanceId;
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
    @Column(name = "base_unit_code", nullable = false, length = 16, updatable = false)
    private String baseUnitCode;
    @Column(name = "physical_quantity", nullable = false, precision = 30, scale = 6)
    private BigDecimal physicalQuantity;
    @Column(name = "reserved_quantity", nullable = false, precision = 30, scale = 6)
    private BigDecimal reservedQuantity;
    @Version @Column(name = "entity_version", nullable = false)
    private long version;

    protected InventoryBalanceEntity() {
    }

    static InventoryBalanceEntity from(UUID balanceId, InventoryBalanceSnapshot snapshot) {
        InventoryBalanceEntity entity = new InventoryBalanceEntity();
        StockKey key = snapshot.key();
        entity.companyId = snapshot.companyId().value();
        entity.inventoryBalanceId = Objects.requireNonNull(balanceId, "balanceId");
        entity.inventoryItemId = key.inventoryItemId().value();
        entity.warehouseId = key.warehouseId().value();
        entity.stockLocationId = key.locationId().value();
        entity.lotCode = key.lotCode().orElse(null);
        entity.serialNumber = key.serialNumber().orElse(null);
        entity.expiryDate = key.expiryDate().orElse(null);
        entity.condition = key.condition();
        entity.baseUnitCode = snapshot.baseUnitCode();
        entity.version = snapshot.version();
        entity.apply(snapshot);
        return entity;
    }

    void apply(InventoryBalanceSnapshot snapshot) {
        physicalQuantity = snapshot.physicalQuantity();
        reservedQuantity = snapshot.reservedQuantity();
    }

    InventoryBalanceSnapshot snapshot() {
        return new InventoryBalanceSnapshot(
                new CompanyId(companyId), stockKey(), baseUnitCode,
                physicalQuantity, reservedQuantity, version);
    }

    StockKey stockKey() {
        return InventoryPersistenceValues.stockKey(
                inventoryItemId, warehouseId, stockLocationId, lotCode,
                serialNumber, expiryDate, condition);
    }

    long version() { return version; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID inventoryBalanceId;
        public Key() { }
        Key(UUID companyId, UUID inventoryBalanceId) { this.companyId = companyId; this.inventoryBalanceId = inventoryBalanceId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(inventoryBalanceId, that.inventoryBalanceId); }
        @Override public int hashCode() { return Objects.hash(companyId, inventoryBalanceId); }
    }
}
