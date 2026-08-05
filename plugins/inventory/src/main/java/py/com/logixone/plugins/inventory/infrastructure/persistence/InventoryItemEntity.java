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
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.inventory.api.ExpiryPolicy;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.TrackingMode;
import py.com.logixone.plugins.inventory.domain.InventoryItemSnapshot;

@Entity
@Table(name = "inventory_item", schema = InventoryPersistenceNames.SCHEMA)
@IdClass(InventoryItemEntity.Key.class)
public class InventoryItemEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "inventory_item_id", nullable = false, updatable = false)
    private UUID inventoryItemId;
    @Column(name = "catalog_item_id", nullable = false, updatable = false)
    private UUID catalogItemId;
    @Column(name = "catalog_code_snapshot", nullable = false, length = 64)
    private String catalogCode;
    @Column(name = "catalog_name_snapshot", nullable = false, length = 240)
    private String catalogName;
    @Column(name = "base_unit_code_snapshot", nullable = false, length = 16)
    private String baseUnitCode;
    @Column(name = "catalog_item_version", nullable = false)
    private long catalogItemVersion;
    @Enumerated(EnumType.STRING) @Column(name = "tracking_mode", nullable = false, length = 16, updatable = false)
    private TrackingMode trackingMode;
    @Enumerated(EnumType.STRING) @Column(name = "expiry_policy", nullable = false, length = 16, updatable = false)
    private ExpiryPolicy expiryPolicy;
    @Column(name = "active", nullable = false)
    private boolean active;
    @Version @Column(name = "entity_version", nullable = false)
    private long version;

    protected InventoryItemEntity() {
    }

    static InventoryItemEntity from(InventoryItemSnapshot snapshot) {
        InventoryItemEntity entity = new InventoryItemEntity();
        entity.companyId = snapshot.companyId().value();
        entity.inventoryItemId = snapshot.id().value();
        entity.catalogItemId = snapshot.catalogItemId().value();
        entity.catalogCode = snapshot.catalogCode();
        entity.catalogName = snapshot.catalogName();
        entity.baseUnitCode = snapshot.baseUnitCode();
        entity.catalogItemVersion = snapshot.catalogItemVersion();
        entity.trackingMode = snapshot.trackingMode();
        entity.expiryPolicy = snapshot.expiryPolicy();
        entity.version = snapshot.version();
        entity.apply(snapshot);
        return entity;
    }

    void apply(InventoryItemSnapshot snapshot) {
        catalogCode = snapshot.catalogCode();
        catalogName = snapshot.catalogName();
        baseUnitCode = snapshot.baseUnitCode();
        catalogItemVersion = snapshot.catalogItemVersion();
        active = snapshot.active();
    }

    InventoryItemSnapshot snapshot() {
        return new InventoryItemSnapshot(
                new CompanyId(companyId), new InventoryItemId(inventoryItemId),
                new CatalogItemId(catalogItemId), catalogCode, catalogName, baseUnitCode,
                catalogItemVersion, trackingMode, expiryPolicy, active, version);
    }

    long version() { return version; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID inventoryItemId;
        public Key() { }
        Key(UUID companyId, UUID inventoryItemId) { this.companyId = companyId; this.inventoryItemId = inventoryItemId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(inventoryItemId, that.inventoryItemId); }
        @Override public int hashCode() { return Objects.hash(companyId, inventoryItemId); }
    }
}
