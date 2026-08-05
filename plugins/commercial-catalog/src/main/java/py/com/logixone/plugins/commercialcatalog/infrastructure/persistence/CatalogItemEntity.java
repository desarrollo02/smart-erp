package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemSnapshot;

@Entity
@Table(name = "catalog_item", schema = CommercialCatalogPersistenceNames.SCHEMA)
@IdClass(CatalogItemEntity.Key.class)
public class CatalogItemEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "catalog_item_id", nullable = false, updatable = false)
    private UUID catalogItemId;
    @Column(name = "code", nullable = false, length = 64)
    private String code;
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;
    @Column(name = "description", nullable = false, length = 1000)
    private String description;
    @Enumerated(EnumType.STRING) @Column(name = "item_type", nullable = false, length = 16, updatable = false)
    private CatalogItemType itemType;
    @Enumerated(EnumType.STRING) @Column(name = "state", nullable = false, length = 16)
    private CatalogItemState state;
    @Column(name = "base_unit_code", nullable = false, length = 16, updatable = false)
    private String baseUnitCode;
    @Column(name = "tax_profile_id", nullable = false)
    private UUID taxProfileId;
    @Column(name = "tax_profile_version", nullable = false)
    private long taxProfileVersion;
    @Column(name = "brand_id")
    private UUID brandId;
    @Column(name = "replacement_item_id")
    private UUID replacementItemId;
    @Version @Column(name = "version", nullable = false)
    private long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CatalogItemEntity() {
    }

    static CatalogItemEntity from(CatalogItemSnapshot snapshot) {
        CatalogItemEntity entity = new CatalogItemEntity();
        entity.companyId = snapshot.companyId().value();
        entity.catalogItemId = snapshot.id().value();
        entity.itemType = snapshot.type();
        entity.baseUnitCode = snapshot.baseUnit().value();
        entity.version = snapshot.version();
        entity.apply(snapshot);
        return entity;
    }

    void apply(CatalogItemSnapshot snapshot) {
        code = snapshot.code().value();
        displayName = snapshot.name().value();
        description = snapshot.description();
        state = snapshot.state();
        taxProfileId = snapshot.taxProfile().id().value();
        taxProfileVersion = snapshot.taxProfile().version();
        brandId = snapshot.classification().flatMap(value -> value.brand()).map(value -> value.value()).orElse(null);
        replacementItemId = snapshot.replacementId().map(value -> value.value()).orElse(null);
        updatedAt = Instant.now();
    }

    UUID companyId() { return companyId; }
    UUID catalogItemId() { return catalogItemId; }
    String code() { return code; }
    String displayName() { return displayName; }
    String description() { return description; }
    CatalogItemType itemType() { return itemType; }
    CatalogItemState state() { return state; }
    String baseUnitCode() { return baseUnitCode; }
    UUID taxProfileId() { return taxProfileId; }
    long taxProfileVersion() { return taxProfileVersion; }
    UUID brandId() { return brandId; }
    UUID replacementItemId() { return replacementItemId; }
    long version() { return version; }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID catalogItemId;
        public Key() { }
        Key(UUID companyId, UUID catalogItemId) { this.companyId = companyId; this.catalogItemId = catalogItemId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(catalogItemId, that.catalogItemId); }
        @Override public int hashCode() { return Objects.hash(companyId, catalogItemId); }
    }
}
