package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.domain.TagId;

@Entity @Table(name="catalog_item_tag", schema=CommercialCatalogPersistenceNames.SCHEMA)
@IdClass(CatalogItemUuidKey.class)
public class CatalogItemTagEntity {
    @Id @Column(name="company_id") private UUID companyId;
    @Id @Column(name="catalog_item_id") private UUID catalogItemId;
    @Id @Column(name="tag_id") private UUID detailId;
    protected CatalogItemTagEntity() { }
    static CatalogItemTagEntity from(UUID company, UUID item, TagId id) { var e=new CatalogItemTagEntity(); e.companyId=company; e.catalogItemId=item; e.detailId=id.value(); return e; }
    TagId toDomain() { return new TagId(detailId); }
    UUID key() { return detailId; }
}
