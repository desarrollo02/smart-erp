package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.persistence.*; import java.io.Serializable; import java.util.*; import py.com.logixone.plugins.sales.domain.SalesLineSnapshot;

@Entity @Table(name="sales_quote_line",schema=SalesPersistenceNames.SCHEMA) @IdClass(SalesQuoteLineEntity.Key.class)
public class SalesQuoteLineEntity {
 @Id @Column(name="company_id") UUID companyId; @Id @Column(name="sales_quote_id") UUID quoteId; @Id @Column(name="sales_quote_line_id") UUID id;
 @Column(name="line_position",nullable=false) int position;
 @Embedded @AttributeOverride(name="quantity",column=@Column(name="quoted_quantity",nullable=false,precision=30,scale=6)) SalesLineEmbeddable value;
 public SalesQuoteLineEntity(){}
 static SalesQuoteLineEntity from(UUID c,UUID q,int p,SalesLineSnapshot s){var e=new SalesQuoteLineEntity();e.companyId=c;e.quoteId=q;e.id=s.id();e.position=p;e.value=SalesLineEmbeddable.from(s);return e;}
 SalesLineSnapshot snapshot(){return value.snapshot(id);} int position(){return position;}
 public static final class Key implements Serializable {public UUID companyId;public UUID quoteId;public UUID id;public Key(){}@Override public boolean equals(Object o){return this==o||o instanceof Key k&&Objects.equals(companyId,k.companyId)&&Objects.equals(quoteId,k.quoteId)&&Objects.equals(id,k.id);}@Override public int hashCode(){return Objects.hash(companyId,quoteId,id);}}
}
