package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;

@Entity @Table(name="catalog_item_scope", schema=CommercialCatalogPersistenceNames.SCHEMA)
@IdClass(CatalogItemStringKey.class)
public class CatalogItemScopeEntity {
    @Id @Column(name="company_id") private UUID companyId;
    @Id @Column(name="catalog_item_id") private UUID catalogItemId;
    @Id @Column(name="scope_code", length=16) private String detailCode;
    protected CatalogItemScopeEntity() { }
    static CatalogItemScopeEntity from(UUID company, UUID item, CatalogItemScope scope) { var e=new CatalogItemScopeEntity(); e.companyId=company; e.catalogItemId=item; e.detailCode=scope.name(); return e; }
    CatalogItemScope toDomain() { return CatalogItemScope.valueOf(detailCode); }
    UUID catalogItemId() { return catalogItemId; }
    String key() { return detailCode; }
}
