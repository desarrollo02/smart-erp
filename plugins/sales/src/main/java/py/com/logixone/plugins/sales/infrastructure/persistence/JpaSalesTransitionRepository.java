package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.Objects;
import py.com.logixone.plugins.sales.application.port.*;

@ApplicationScoped @Transactional
public class JpaSalesTransitionRepository implements SalesTransitionRepository {
    @PersistenceContext(unitName=SalesPersistenceNames.UNIT_NAME) EntityManager em;
    public JpaSalesTransitionRepository() { }
    JpaSalesTransitionRepository(EntityManager em) { this.em = Objects.requireNonNull(em); }
    public void append(SalesTransitionRecord value){try{em.persist(SalesTransitionHistoryEntity.from(value));em.flush();}catch(RuntimeException failure){throw SalesConflictMapper.map(failure);}}
}
