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
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.api.CatalogTaxMode;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListSnapshot;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListState;

@Entity
@Table(name = "price_list", schema = CommercialCatalogPersistenceNames.SCHEMA)
@IdClass(PriceListEntity.Key.class)
public class PriceListEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "price_list_id", nullable = false, updatable = false)
    private UUID priceListId;
    @Column(name = "code", nullable = false, length = 64)
    private String code;
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;
    @Column(name = "currency_code", nullable = false, length = 3, updatable = false)
    private String currencyCode;
    @Enumerated(EnumType.STRING) @Column(name = "tax_mode", nullable = false, length = 16, updatable = false)
    private CatalogTaxMode taxMode;
    @Column(name = "amount_scale", nullable = false, updatable = false)
    private int amountScale;
    @Enumerated(EnumType.STRING) @Column(name = "rounding_mode", nullable = false, length = 16, updatable = false)
    private RoundingMode roundingMode;
    @Enumerated(EnumType.STRING) @Column(name = "state", nullable = false, length = 16)
    private PriceListState state;
    @Version @Column(name = "version", nullable = false)
    private long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PriceListEntity() {
    }

    static PriceListEntity from(PriceListSnapshot snapshot) {
        PriceListEntity entity = new PriceListEntity();
        entity.companyId = snapshot.companyId().value();
        entity.priceListId = snapshot.id().value();
        entity.code = snapshot.code().value();
        entity.currencyCode = snapshot.currency();
        entity.taxMode = snapshot.taxMode();
        entity.amountScale = snapshot.scale();
        entity.roundingMode = snapshot.roundingMode();
        entity.version = snapshot.version();
        entity.apply(snapshot);
        return entity;
    }

    void apply(PriceListSnapshot snapshot) {
        displayName = snapshot.name().value();
        state = snapshot.state();
        updatedAt = Instant.now();
    }

    UUID companyId() { return companyId; }
    UUID priceListId() { return priceListId; }
    String code() { return code; }
    String displayName() { return displayName; }
    String currencyCode() { return currencyCode; }
    CatalogTaxMode taxMode() { return taxMode; }
    int amountScale() { return amountScale; }
    RoundingMode roundingMode() { return roundingMode; }
    PriceListState state() { return state; }
    long version() { return version; }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID priceListId;
        public Key() { }
        Key(UUID companyId, UUID priceListId) { this.companyId = companyId; this.priceListId = priceListId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(priceListId, that.priceListId); }
        @Override public int hashCode() { return Objects.hash(companyId, priceListId); }
    }
}
