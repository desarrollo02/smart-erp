package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogDetailId;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemIdentifier;

@Entity @Table(name="catalog_item_identifier", schema=CommercialCatalogPersistenceNames.SCHEMA)
@IdClass(CatalogItemUuidKey.class)
public class CatalogItemIdentifierEntity {
    @Id @Column(name="company_id") private UUID companyId;
    @Id @Column(name="catalog_item_id") private UUID catalogItemId;
    @Id @Column(name="identifier_id") private UUID detailId;
    @Column(name="type_code", nullable=false, length=32) private String typeCode;
    @Column(name="presented_value", nullable=false, length=128) private String presentedValue;
    @Column(name="normalized_value", nullable=false, length=128) private String normalizedValue;
    @Column(name="active", nullable=false) private boolean active;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected CatalogItemIdentifierEntity() { }
    static CatalogItemIdentifierEntity from(UUID company, UUID item, CatalogItemIdentifier value) { var e=new CatalogItemIdentifierEntity(); e.companyId=company; e.catalogItemId=item; e.detailId=value.id().value(); e.apply(value); return e; }
    void apply(CatalogItemIdentifier value) { typeCode=value.typeCode(); presentedValue=value.presentedValue(); normalizedValue=value.normalizedValue(); active=value.active(); updatedAt=Instant.now(); }
    CatalogItemIdentifier toDomain() { return new CatalogItemIdentifier(new CatalogDetailId(detailId), typeCode, presentedValue, normalizedValue, active); }
    UUID key() { return detailId; }
    @PrePersist void timestamps() { Instant now=Instant.now(); createdAt=createdAt==null?now:createdAt; updatedAt=updatedAt==null?now:updatedAt; }
}
