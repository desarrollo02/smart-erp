package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.sales.domain.SalesLineSnapshot;

@Embeddable
public class SalesLineEmbeddable {
 @Column(name="catalog_item_id",nullable=false) UUID catalogItemId;
 @Column(name="catalog_code_snapshot",nullable=false,length=64) String catalogCode;
 @Column(name="item_description_snapshot",nullable=false,length=240) String description;
 @Column(name="unit_code_snapshot",nullable=false,length=16) String unitCode;
 @Column(name="stock_managed",nullable=false) boolean stockManaged;
 @Column(name="document_quantity",nullable=false,precision=30,scale=6) BigDecimal quantity;
 @Column(name="unit_price",nullable=false,precision=30,scale=6) BigDecimal unitPrice;
 @Column(name="tax_code_snapshot",nullable=false,length=32) String taxCode;
 @Column(name="price_list_id_snapshot",length=128) String priceListId;
 @Column(name="manual_price",nullable=false) boolean manualPrice;
 @Column(name="price_exception_reason",length=240) String exceptionReason;
 @Column(name="catalog_source_version",nullable=false) long catalogVersion;
 public SalesLineEmbeddable(){}
 static SalesLineEmbeddable from(SalesLineSnapshot s){var v=new SalesLineEmbeddable();v.catalogItemId=s.catalogItemId().value();v.catalogCode=s.catalogCode();v.description=s.description();v.unitCode=s.unitCode();v.stockManaged=s.stockManaged();v.quantity=s.quantity();v.unitPrice=s.unitPrice();v.taxCode=s.taxCode();v.priceListId=s.priceListId().orElse(null);v.manualPrice=s.manualPrice();v.exceptionReason=s.priceExceptionReason().orElse(null);v.catalogVersion=s.catalogVersion();return v;}
 SalesLineSnapshot snapshot(UUID id){return new SalesLineSnapshot(id,new CatalogItemId(catalogItemId),catalogCode,description,unitCode,stockManaged,quantity,unitPrice,taxCode,Optional.ofNullable(priceListId),manualPrice,Optional.ofNullable(exceptionReason),catalogVersion);}
}
