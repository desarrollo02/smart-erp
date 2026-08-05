package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.domain.*;

@Entity @Table(name="catalog_item_variant_attribute", schema=CommercialCatalogPersistenceNames.SCHEMA)
@IdClass(CatalogItemStringKey.class)
public class CatalogItemVariantAttributeEntity {
    @Id @Column(name="company_id") private UUID companyId;
    @Id @Column(name="catalog_item_id") private UUID catalogItemId;
    @Id @Column(name="attribute_code", length=32) private String detailCode;
    @Column(name="variant_family_id", nullable=false) private UUID variantFamilyId;
    @Column(name="variant_family_version", nullable=false) private long variantFamilyVersion;
    @Enumerated(EnumType.STRING) @Column(name="value_type", nullable=false, length=16) private VariantValueType valueType;
    @Column(name="attribute_value", nullable=false, length=100) private String attributeValue;
    protected CatalogItemVariantAttributeEntity() { }
    static CatalogItemVariantAttributeEntity from(UUID company, UUID item, VariantFamilyId family, long version, VariantAttributeCode code, VariantAttributeValue value) { var e=new CatalogItemVariantAttributeEntity(); e.companyId=company; e.catalogItemId=item; e.detailCode=code.value(); e.variantFamilyId=family.value(); e.variantFamilyVersion=version; e.valueType=value.type(); e.attributeValue=value.value(); return e; }
    VariantAttributeCode code() { return new VariantAttributeCode(detailCode); }
    VariantAttributeValue value() { return new VariantAttributeValue(valueType, attributeValue); }
}
