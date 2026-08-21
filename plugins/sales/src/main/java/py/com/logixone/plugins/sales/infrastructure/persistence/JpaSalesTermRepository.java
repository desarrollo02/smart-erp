package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped; import jakarta.persistence.*; import jakarta.transaction.Transactional; import java.util.*;
import py.com.logixone.kernel.api.company.CompanyId; import py.com.logixone.plugins.sales.application.port.*; import py.com.logixone.plugins.sales.domain.SalesTerm;

@ApplicationScoped @Transactional
public class JpaSalesTermRepository implements SalesTermRepository {
 @PersistenceContext(unitName=SalesPersistenceNames.UNIT_NAME) EntityManager em;
 public JpaSalesTermRepository(){} JpaSalesTermRepository(EntityManager em){this.em=Objects.requireNonNull(em);}
 public Optional<SalesTerm> findById(CompanyId companyId,UUID id){return Optional.ofNullable(em.find(SalesTermEntity.class,new SalesTermEntity.Key(companyId.value(),id))).map(SalesTermEntity::snapshot);}
 public Optional<SalesTerm> findByCode(CompanyId companyId,String code){return em.createQuery("SELECT t FROM SalesTermEntity t WHERE t.companyId=:company AND t.code=:code",SalesTermEntity.class).setParameter("company",companyId.value()).setParameter("code",code).getResultStream().findFirst().map(SalesTermEntity::snapshot);}
 public SalesTerm insert(SalesTerm term){try{em.persist(SalesTermEntity.from(term.snapshot()));em.flush();return term;}catch(RuntimeException x){throw SalesConflictMapper.map(x);}}
 public SalesTerm update(SalesTerm term,long expected){var s=term.snapshot();if(expected<0||s.version()!=expected+1)throw new SalesPersistenceException(SalesPersistenceCode.VERSION_CONFLICT);var e=em.find(SalesTermEntity.class,new SalesTermEntity.Key(s.companyId().value(),s.id()));if(e==null)throw new SalesPersistenceException(SalesPersistenceCode.NOT_FOUND);if(e.version!=expected)throw new SalesPersistenceException(SalesPersistenceCode.VERSION_CONFLICT);if(!e.code.equals(s.code()))throw new SalesPersistenceException(SalesPersistenceCode.IMMUTABLE_DOCUMENT);try{e.apply(s);em.flush();return term;}catch(RuntimeException x){throw SalesConflictMapper.map(x);}}
}
