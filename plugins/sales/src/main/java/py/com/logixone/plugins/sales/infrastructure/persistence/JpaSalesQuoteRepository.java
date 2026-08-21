package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped; import jakarta.persistence.*; import jakarta.transaction.Transactional;
import java.util.*; import py.com.logixone.kernel.api.company.CompanyId; import py.com.logixone.plugins.sales.api.*; import py.com.logixone.plugins.sales.application.port.*; import py.com.logixone.plugins.sales.domain.*;

@ApplicationScoped @Transactional
public class JpaSalesQuoteRepository implements SalesQuoteRepository {
 @PersistenceContext(unitName=SalesPersistenceNames.UNIT_NAME) EntityManager em;
 public JpaSalesQuoteRepository(){} JpaSalesQuoteRepository(EntityManager em){this.em=Objects.requireNonNull(em);}
 public Optional<SalesQuote> findById(CompanyId companyId,SalesQuoteId id){return Optional.ofNullable(em.find(SalesQuoteEntity.class,new SalesQuoteEntity.Key(companyId.value(),id.value()))).map(this::restore);}
 public Optional<SalesQuote> findByNumber(CompanyId companyId,String number){return em.createQuery("SELECT q FROM SalesQuoteEntity q WHERE q.companyId=:company AND q.number=:number",SalesQuoteEntity.class).setParameter("company",companyId.value()).setParameter("number",number).getResultStream().findFirst().map(this::restore);}
 public SalesQuote insert(SalesQuote quote){var s=quote.snapshot();if(s.state()!=SalesQuoteState.DRAFT)throw new SalesPersistenceException(SalesPersistenceCode.IMMUTABLE_DOCUMENT);try{em.persist(SalesQuoteEntity.from(s));for(int i=0;i<s.lines().size();i++)em.persist(SalesQuoteLineEntity.from(s.companyId().value(),s.id().value(),i+1,s.lines().get(i)));em.flush();return quote;}catch(RuntimeException x){throw SalesConflictMapper.map(x);}}
 public SalesQuote update(SalesQuote quote,long expected){var target=quote.snapshot();var entity=require(target.companyId().value(),target.id().value(),expected,target.version());var current=restore(entity).snapshot();if(!immutable(current,target))throw new SalesPersistenceException(SalesPersistenceCode.IMMUTABLE_DOCUMENT);try{entity.apply(target);em.flush();return quote;}catch(RuntimeException x){throw SalesConflictMapper.map(x);}}
 private SalesQuoteEntity require(UUID c,UUID id,long expected,long domainVersion){if(expected<0||domainVersion!=expected+1)throw new SalesPersistenceException(SalesPersistenceCode.VERSION_CONFLICT);var e=em.find(SalesQuoteEntity.class,new SalesQuoteEntity.Key(c,id));if(e==null)throw new SalesPersistenceException(SalesPersistenceCode.NOT_FOUND);if(e.version()!=expected)throw new SalesPersistenceException(SalesPersistenceCode.VERSION_CONFLICT);return e;}
 private SalesQuote restore(SalesQuoteEntity e){return SalesQuote.restore(e.snapshot(lines(e.companyId(),e.id())));}
 private List<SalesLineSnapshot> lines(UUID c,UUID id){return em.createQuery("SELECT l FROM SalesQuoteLineEntity l WHERE l.companyId=:company AND l.quoteId=:id ORDER BY l.position",SalesQuoteLineEntity.class).setParameter("company",c).setParameter("id",id).getResultList().stream().map(SalesQuoteLineEntity::snapshot).toList();}
 private static boolean immutable(SalesQuote.Snapshot a,SalesQuote.Snapshot b){return a.number().equals(b.number())&&a.customer().equals(b.customer())&&a.currency().equals(b.currency())&&a.term().equals(b.term())&&a.validUntil().equals(b.validUntil())&&a.lines().equals(b.lines());}
}
