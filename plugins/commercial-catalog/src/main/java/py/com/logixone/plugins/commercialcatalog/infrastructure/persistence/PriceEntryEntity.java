package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.PriceEntryId;
import py.com.logixone.plugins.commercialcatalog.domain.PriceEntry;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;

@Entity @Table(name="price_entry", schema=CommercialCatalogPersistenceNames.SCHEMA)
@IdClass(PriceEntryKey.class)
public class PriceEntryEntity {
    @Id @Column(name="company_id") private UUID companyId;
    @Id @Column(name="price_list_id") private UUID priceListId;
    @Id @Column(name="price_entry_id") private UUID priceEntryId;
    @Column(name="catalog_item_id", nullable=false) private UUID catalogItemId;
    @Column(name="unit_code", nullable=false, length=16) private String unitCode;
    @Column(name="minimum_quantity", nullable=false, precision=38, scale=18) private BigDecimal minimumQuantity;
    @Column(name="amount", nullable=false, precision=38, scale=6) private BigDecimal amount;
    @Column(name="valid_from", nullable=false) private Instant validFrom;
    @Column(name="valid_until") private Instant validUntil;
    @Column(name="active", nullable=false) private boolean active;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected PriceEntryEntity() { }
    static PriceEntryEntity from(UUID company, UUID list, PriceEntry value) { var e=new PriceEntryEntity(); e.companyId=company; e.priceListId=list; e.priceEntryId=value.id().value(); e.apply(value); return e; }
    void apply(PriceEntry value) { catalogItemId=value.itemId().value(); unitCode=value.unit().value(); minimumQuantity=value.minimumQuantity(); amount=value.amount(); validFrom=value.validFrom(); validUntil=value.validUntil().orElse(null); active=value.active(); updatedAt=Instant.now(); }
    PriceEntry toDomain() { return new PriceEntry(new PriceEntryId(priceEntryId), new CatalogItemId(catalogItemId), new UnitCode(unitCode), minimumQuantity, amount, validFrom, Optional.ofNullable(validUntil), active); }
    UUID key() { return priceEntryId; }
    @PrePersist void timestamps() { Instant now=Instant.now(); createdAt=createdAt==null?now:createdAt; updatedAt=updatedAt==null?now:updatedAt; }
}
