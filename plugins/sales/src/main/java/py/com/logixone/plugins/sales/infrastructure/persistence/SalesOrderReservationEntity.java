package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.persistence.*; import java.io.Serializable; import java.util.*; import py.com.logixone.plugins.inventory.api.StockReservationId;

@Entity @Table(name="sales_order_reservation",schema=SalesPersistenceNames.SCHEMA) @IdClass(SalesOrderReservationEntity.Key.class)
public class SalesOrderReservationEntity {
 @Id @Column(name="company_id") UUID companyId; @Id @Column(name="sales_order_id") UUID orderId; @Id @Column(name="sales_order_line_id") UUID lineId; @Column(name="reservation_id",nullable=false) UUID reservationId;
 public SalesOrderReservationEntity(){}
 static SalesOrderReservationEntity from(UUID c,UUID o,UUID l,StockReservationId r){var e=new SalesOrderReservationEntity();e.companyId=c;e.orderId=o;e.lineId=l;e.reservationId=r.value();return e;}
 UUID lineId(){return lineId;} StockReservationId reservation(){return new StockReservationId(reservationId);}
 public static final class Key implements Serializable {public UUID companyId;public UUID orderId;public UUID lineId;public Key(){}@Override public boolean equals(Object o){return this==o||o instanceof Key k&&Objects.equals(companyId,k.companyId)&&Objects.equals(orderId,k.orderId)&&Objects.equals(lineId,k.lineId);}@Override public int hashCode(){return Objects.hash(companyId,orderId,lineId);}}
}
