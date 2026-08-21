package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.persistence.*; import java.io.Serializable; import java.util.*; import py.com.logixone.plugins.sales.domain.SalesLineSnapshot;

@Entity @Table(name="sales_order_line",schema=SalesPersistenceNames.SCHEMA) @IdClass(SalesOrderLineEntity.Key.class)
public class SalesOrderLineEntity {
 @Id @Column(name="company_id") UUID companyId; @Id @Column(name="sales_order_id") UUID orderId; @Id @Column(name="sales_order_line_id") UUID id;
 @Column(name="line_position",nullable=false) int position;
 @Embedded @AttributeOverride(name="quantity",column=@Column(name="ordered_quantity",nullable=false,precision=30,scale=6)) SalesLineEmbeddable value;
 public SalesOrderLineEntity(){}
 static SalesOrderLineEntity from(UUID c,UUID o,int p,SalesLineSnapshot s){var e=new SalesOrderLineEntity();e.companyId=c;e.orderId=o;e.id=s.id();e.position=p;e.value=SalesLineEmbeddable.from(s);return e;}
 SalesLineSnapshot snapshot(){return value.snapshot(id);} UUID id(){return id;}
 public static final class Key implements Serializable {public UUID companyId;public UUID orderId;public UUID id;public Key(){}@Override public boolean equals(Object o){return this==o||o instanceof Key k&&Objects.equals(companyId,k.companyId)&&Objects.equals(orderId,k.orderId)&&Objects.equals(id,k.id);}@Override public int hashCode(){return Objects.hash(companyId,orderId,id);}}
}
