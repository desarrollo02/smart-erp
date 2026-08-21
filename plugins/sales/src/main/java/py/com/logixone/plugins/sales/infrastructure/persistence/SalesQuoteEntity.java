package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.persistence.*; import java.io.Serializable; import java.time.*; import java.util.*;
import py.com.logixone.kernel.api.company.CompanyId; import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.sales.api.*; import py.com.logixone.plugins.sales.domain.*;

@Entity @Table(name="sales_quote",schema=SalesPersistenceNames.SCHEMA) @IdClass(SalesQuoteEntity.Key.class)
public class SalesQuoteEntity {
 @Id @Column(name="company_id") UUID companyId; @Id @Column(name="sales_quote_id") UUID id;
 @Column(name="quote_number",nullable=false,length=64) String number; @Embedded CustomerEmbeddable customer; @Embedded CurrencyEmbeddable currency; @Embedded TermEmbeddable term;
 @Column(name="valid_until",nullable=false) LocalDate validUntil; @Enumerated(EnumType.STRING) @Column(name="quote_state",nullable=false,length=24) SalesQuoteState state;
 @Column(name="issued_at") Instant issuedAt; @Column(name="transition_actor_id") UUID transitionActorId; @Version @Column(name="entity_version",nullable=false) long version;
 public SalesQuoteEntity(){}
 static SalesQuoteEntity from(SalesQuote.Snapshot s){var e=new SalesQuoteEntity();e.companyId=s.companyId().value();e.id=s.id().value();e.number=s.number();e.customer=CustomerEmbeddable.from(s.customer());e.currency=CurrencyEmbeddable.from(s.currency());e.term=TermEmbeddable.from(s.term());e.apply(s);return e;}
 void apply(SalesQuote.Snapshot s){state=s.state();validUntil=s.validUntil();issuedAt=s.issuedAt().orElse(null);transitionActorId=s.transitionActor().map(AppUserId::value).orElse(null);version=s.version();}
 SalesQuote.Snapshot snapshot(List<SalesLineSnapshot> lines){return new SalesQuote.Snapshot(new CompanyId(companyId),new SalesQuoteId(id),number,customer.snapshot(),currency.snapshot(),term.snapshot(),validUntil,lines,state,Optional.ofNullable(issuedAt),Optional.ofNullable(transitionActorId).map(AppUserId::new),version);}
 long version(){return version;} UUID companyId(){return companyId;} UUID id(){return id;}
 public static final class Key implements Serializable {public UUID companyId;public UUID id;public Key(){} Key(UUID c,UUID i){companyId=c;id=i;}@Override public boolean equals(Object o){return this==o||o instanceof Key k&&Objects.equals(companyId,k.companyId)&&Objects.equals(id,k.id);}@Override public int hashCode(){return Objects.hash(companyId,id);}}
}
