package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;

@Entity @Table(name="catalog_item_variant", schema=CommercialCatalogPersistenceNames.SCHEMA)
@IdClass(CatalogItemEntity.Key.class)
public class CatalogItemVariantEntity {
    @Id @Column(name="company_id") private UUID companyId;
    @Id @Column(name="catalog_item_id") private UUID catalogItemId;
    @Column(name="variant_family_id", nullable=false) private UUID variantFamilyId;
    @Column(name="variant_family_version", nullable=false) private long variantFamilyVersion;
    protected CatalogItemVariantEntity() { }
    static CatalogItemVariantEntity from(UUID company, UUID item, VariantFamilyId family, long version) { var e=new CatalogItemVariantEntity(); e.companyId=company; e.catalogItemId=item; e.variantFamilyId=family.value(); e.variantFamilyVersion=version; return e; }
    VariantFamilyId family() { return new VariantFamilyId(variantFamilyId); }
    long familyVersion() { return variantFamilyVersion; }
}
