package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;

@Entity
@Table(name = "warehouse", schema = InventoryPersistenceNames.SCHEMA)
@IdClass(WarehouseEntity.Key.class)
public class WarehouseEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "warehouse_id", nullable = false, updatable = false)
    private UUID warehouseId;
    @Column(name = "warehouse_code", nullable = false, length = 64, updatable = false)
    private String code;
    @Column(name = "warehouse_name", nullable = false, length = 160)
    private String name;
    @Column(name = "active", nullable = false)
    private boolean active;
    @Version @Column(name = "entity_version", nullable = false)
    private long version;

    protected WarehouseEntity() {
    }

    static WarehouseEntity from(WarehouseSnapshot snapshot) {
        WarehouseEntity entity = new WarehouseEntity();
        entity.companyId = snapshot.companyId().value();
        entity.warehouseId = snapshot.id().value();
        entity.code = snapshot.code();
        entity.version = snapshot.version();
        entity.apply(snapshot);
        return entity;
    }

    void apply(WarehouseSnapshot snapshot) {
        name = snapshot.name();
        active = snapshot.active();
    }

    UUID companyId() { return companyId; }
    UUID warehouseId() { return warehouseId; }
    String code() { return code; }
    String name() { return name; }
    boolean active() { return active; }
    long version() { return version; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID warehouseId;
        public Key() { }
        Key(UUID companyId, UUID warehouseId) { this.companyId = companyId; this.warehouseId = warehouseId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(warehouseId, that.warehouseId); }
        @Override public int hashCode() { return Objects.hash(companyId, warehouseId); }
    }
}
