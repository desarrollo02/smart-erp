package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.sales.application.port.*;

@ApplicationScoped @Transactional
public class JpaSalesOperationRepository implements SalesOperationRepository {
    @PersistenceContext(unitName=SalesPersistenceNames.UNIT_NAME) EntityManager em;
    public JpaSalesOperationRepository() { }
    JpaSalesOperationRepository(EntityManager em) { this.em = Objects.requireNonNull(em); }
    public Optional<SalesOperationRecord> find(CompanyId companyId,String key){
        return Optional.ofNullable(em.find(SalesOperationEntity.class,
                new SalesOperationEntity.Key(companyId.value(),key))).map(SalesOperationEntity::record);
    }
    public void append(SalesOperationRecord value){try{em.persist(SalesOperationEntity.from(value));em.flush();}catch(RuntimeException failure){throw SalesConflictMapper.map(failure);}}
}
