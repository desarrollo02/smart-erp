package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.persistence.*;
import java.io.Serializable; import java.util.Objects; import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId; import py.com.logixone.plugins.sales.domain.SalesTerm;

@Entity @Table(name="sales_term",schema=SalesPersistenceNames.SCHEMA) @IdClass(SalesTermEntity.Key.class)
public class SalesTermEntity {
 @Id @Column(name="company_id") UUID companyId; @Id @Column(name="sales_term_id") UUID id;
 @Column(name="term_code",nullable=false,length=32) String code; @Column(name="display_name",nullable=false,length=120) String name;
 @Column(name="due_days",nullable=false) int dueDays; @Column(name="active",nullable=false) boolean active;
 @Version @Column(name="entity_version",nullable=false) long version;
 public SalesTermEntity(){}
 static SalesTermEntity from(SalesTerm.Snapshot s){var e=new SalesTermEntity();e.apply(s);return e;}
 void apply(SalesTerm.Snapshot s){companyId=s.companyId().value();id=s.id();code=s.code();name=s.displayName();dueDays=s.dueDays();active=s.active();version=s.version();}
 SalesTerm snapshot(){return SalesTerm.restore(new SalesTerm.Snapshot(new CompanyId(companyId),id,code,name,dueDays,active,version));}
 public static final class Key implements Serializable { public UUID companyId;public UUID id;public Key(){} Key(UUID c,UUID i){companyId=c;id=i;} @Override public boolean equals(Object o){return this==o||o instanceof Key k&&Objects.equals(companyId,k.companyId)&&Objects.equals(id,k.id);}@Override public int hashCode(){return Objects.hash(companyId,id);} }
}
