package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.domain.CategoryId;

@Entity @Table(name="catalog_item_category", schema=CommercialCatalogPersistenceNames.SCHEMA)
@IdClass(CatalogItemUuidKey.class)
public class CatalogItemCategoryEntity {
    @Id @Column(name="company_id") private UUID companyId;
    @Id @Column(name="catalog_item_id") private UUID catalogItemId;
    @Id @Column(name="category_id") private UUID detailId;
    @Column(name="is_primary", nullable=false) private boolean primary;
    protected CatalogItemCategoryEntity() { }
    static CatalogItemCategoryEntity from(UUID company, UUID item, CategoryId id, boolean primary) { var e=new CatalogItemCategoryEntity(); e.companyId=company; e.catalogItemId=item; e.detailId=id.value(); e.primary=primary; return e; }
    CategoryId toDomain() { return new CategoryId(detailId); }
    boolean primary() { return primary; }
    UUID key() { return detailId; }
}
