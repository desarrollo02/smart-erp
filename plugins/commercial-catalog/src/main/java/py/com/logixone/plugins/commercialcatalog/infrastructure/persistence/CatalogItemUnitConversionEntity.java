package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.domain.*;

@Entity @Table(name="catalog_item_unit_conversion", schema=CommercialCatalogPersistenceNames.SCHEMA)
@IdClass(CatalogItemStringKey.class)
public class CatalogItemUnitConversionEntity {
    @Id @Column(name="company_id") private UUID companyId;
    @Id @Column(name="catalog_item_id") private UUID catalogItemId;
    @Id @Column(name="unit_code", length=16) private String detailCode;
    @Column(name="to_base_factor", nullable=false, precision=38, scale=18) private BigDecimal toBaseFactor;
    @Column(name="active", nullable=false) private boolean active;
    protected CatalogItemUnitConversionEntity() { }
    static CatalogItemUnitConversionEntity from(UUID company, UUID item, ItemUnitConversion value) { var e=new CatalogItemUnitConversionEntity(); e.companyId=company; e.catalogItemId=item; e.detailCode=value.unit().value(); e.toBaseFactor=value.toBaseFactor(); e.active=value.active(); return e; }
    void apply(ItemUnitConversion value) { toBaseFactor=value.toBaseFactor(); active=value.active(); }
    ItemUnitConversion toDomain(Set<UnitPurpose> purposes, Set<UnitPurpose> defaults) { return new ItemUnitConversion(new UnitCode(detailCode), toBaseFactor, purposes, defaults, active); }
    String key() { return detailCode; }
}
