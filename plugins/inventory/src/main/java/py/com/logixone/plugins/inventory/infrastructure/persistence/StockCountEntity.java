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
import py.com.logixone.plugins.inventory.domain.StockCountSnapshot;
import py.com.logixone.plugins.inventory.domain.StockCountState;

@Entity
@Table(name = "stock_count", schema = InventoryPersistenceNames.SCHEMA)
@IdClass(StockCountEntity.Key.class)
public class StockCountEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "stock_count_id", nullable = false, updatable = false)
    private UUID stockCountId;
    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private UUID warehouseId;
    @Column(name = "stock_location_id", updatable = false)
    private UUID stockLocationId;
    @Enumerated(EnumType.STRING) @Column(name = "count_state", nullable = false, length = 24)
    private StockCountState state;
    @Version @Column(name = "entity_version", nullable = false)
    private long version;

    protected StockCountEntity() {
    }

    static StockCountEntity from(StockCountSnapshot snapshot) {
        StockCountEntity entity = new StockCountEntity();
        entity.companyId = snapshot.companyId().value();
        entity.stockCountId = snapshot.id().value();
        entity.warehouseId = snapshot.scope().warehouseId().value();
        entity.stockLocationId = snapshot.scope().locationId().map(value -> value.value()).orElse(null);
        entity.version = snapshot.version();
        entity.apply(snapshot);
        return entity;
    }

    void apply(StockCountSnapshot snapshot) { state = snapshot.state(); }

    UUID companyId() { return companyId; }
    UUID stockCountId() { return stockCountId; }
    UUID warehouseId() { return warehouseId; }
    UUID stockLocationId() { return stockLocationId; }
    StockCountState state() { return state; }
    long version() { return version; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID stockCountId;
        public Key() { }
        Key(UUID companyId, UUID stockCountId) { this.companyId = companyId; this.stockCountId = stockCountId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(stockCountId, that.stockCountId); }
        @Override public int hashCode() { return Objects.hash(companyId, stockCountId); }
    }
}
