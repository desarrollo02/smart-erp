package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.domain.UnitPurpose;

@Entity @Table(name="catalog_item_unit_purpose", schema=CommercialCatalogPersistenceNames.SCHEMA)
@IdClass(CatalogItemPurposeKey.class)
public class CatalogItemUnitPurposeEntity {
    @Id @Column(name="company_id") private UUID companyId;
    @Id @Column(name="catalog_item_id") private UUID catalogItemId;
    @Id @Column(name="unit_code", length=16) private String unitCode;
    @Id @Column(name="purpose_code", length=16) private String purposeCode;
    @Column(name="is_default", nullable=false) private boolean isDefault;
    protected CatalogItemUnitPurposeEntity() { }
    static CatalogItemUnitPurposeEntity from(UUID company, UUID item, String unit, UnitPurpose purpose, boolean isDefault) { var e=new CatalogItemUnitPurposeEntity(); e.companyId=company; e.catalogItemId=item; e.unitCode=unit; e.purposeCode=purpose.name(); e.isDefault=isDefault; return e; }
    String unitCode() { return unitCode; }
    UnitPurpose purpose() { return UnitPurpose.valueOf(purposeCode); }
    boolean isDefault() { return isDefault; }
    CatalogItemPurposeKey key() { return new CatalogItemPurposeKey(companyId, catalogItemId, unitCode, purposeCode); }
    void apply(boolean defaultForPurpose) { this.isDefault = defaultForPurpose; }
}
