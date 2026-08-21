package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.persistence.*; import java.io.Serializable; import java.util.*;
import py.com.logixone.kernel.api.company.CompanyId; import py.com.logixone.plugins.sales.api.*; import py.com.logixone.plugins.sales.domain.*;

@Entity @Table(name="sales_order",schema=SalesPersistenceNames.SCHEMA) @IdClass(SalesOrderEntity.Key.class)
public class SalesOrderEntity {
 @Id @Column(name="company_id") UUID companyId; @Id @Column(name="sales_order_id") UUID id;
 @Column(name="order_number",nullable=false,length=64) String number; @Column(name="source_quote_id") UUID sourceQuoteId;
 @Embedded CustomerEmbeddable customer; @Embedded CurrencyEmbeddable currency; @Embedded TermEmbeddable term;
 @Enumerated(EnumType.STRING) @Column(name="order_state",nullable=false,length=24) SalesOrderState state; @Version @Column(name="entity_version",nullable=false) long version;
 public SalesOrderEntity(){}
 static SalesOrderEntity from(SalesOrder.Snapshot s){var e=new SalesOrderEntity();e.companyId=s.companyId().value();e.id=s.id().value();e.number=s.number();e.sourceQuoteId=s.sourceQuoteId().map(SalesQuoteId::value).orElse(null);e.customer=CustomerEmbeddable.from(s.customer());e.currency=CurrencyEmbeddable.from(s.currency());e.term=TermEmbeddable.from(s.term());e.apply(s);return e;}
 void apply(SalesOrder.Snapshot s){state=s.state();version=s.version();}
 SalesOrder.Snapshot snapshot(List<SalesLineSnapshot> lines,Map<UUID,py.com.logixone.plugins.inventory.api.StockReservationId> reservations){return new SalesOrder.Snapshot(new CompanyId(companyId),new SalesOrderId(id),number,customer.snapshot(),currency.snapshot(),term.snapshot(),Optional.ofNullable(sourceQuoteId).map(SalesQuoteId::new),lines,state,reservations,version);}
 long version(){return version;} UUID companyId(){return companyId;} UUID id(){return id;}
 public static final class Key implements Serializable {public UUID companyId;public UUID id;public Key(){} Key(UUID c,UUID i){companyId=c;id=i;}@Override public boolean equals(Object o){return this==o||o instanceof Key k&&Objects.equals(companyId,k.companyId)&&Objects.equals(id,k.id);}@Override public int hashCode(){return Objects.hash(companyId,id);}}
}
