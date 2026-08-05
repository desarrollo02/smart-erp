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
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.domain.StockLocationSnapshot;
import py.com.logixone.plugins.inventory.domain.StockLocationType;

@Entity
@Table(name = "stock_location", schema = InventoryPersistenceNames.SCHEMA)
@IdClass(StockLocationEntity.Key.class)
public class StockLocationEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "stock_location_id", nullable = false, updatable = false)
    private UUID stockLocationId;
    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private UUID warehouseId;
    @Column(name = "location_code", nullable = false, length = 64, updatable = false)
    private String code;
    @Column(name = "location_name", nullable = false, length = 160)
    private String name;
    @Enumerated(EnumType.STRING) @Column(name = "location_type", nullable = false, length = 24, updatable = false)
    private StockLocationType type;
    @Column(name = "active", nullable = false)
    private boolean active;
    @Version @Column(name = "entity_version", nullable = false)
    private long version;

    protected StockLocationEntity() {
    }

    static StockLocationEntity from(StockLocationSnapshot snapshot) {
        StockLocationEntity entity = new StockLocationEntity();
        entity.companyId = snapshot.companyId().value();
        entity.stockLocationId = snapshot.id().value();
        entity.warehouseId = snapshot.warehouseId().value();
        entity.code = snapshot.code();
        entity.type = snapshot.type();
        entity.version = snapshot.version();
        entity.apply(snapshot);
        return entity;
    }

    void apply(StockLocationSnapshot snapshot) {
        name = snapshot.name();
        active = snapshot.active();
    }

    StockLocationSnapshot snapshot() {
        return new StockLocationSnapshot(
                new CompanyId(companyId), new WarehouseId(warehouseId), new StockLocationId(stockLocationId),
                code, name, type, active, version);
    }

    UUID companyId() { return companyId; }
    UUID warehouseId() { return warehouseId; }
    UUID stockLocationId() { return stockLocationId; }
    long version() { return version; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID stockLocationId;
        public Key() { }
        Key(UUID companyId, UUID stockLocationId) { this.companyId = companyId; this.stockLocationId = stockLocationId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(stockLocationId, that.stockLocationId); }
        @Override public int hashCode() { return Objects.hash(companyId, stockLocationId); }
    }
}
