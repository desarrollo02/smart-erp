package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.purchasing.application.port.PurchasingOperationRecord;
import py.com.logixone.plugins.purchasing.application.port.PurchasingOperationRepository;

@ApplicationScoped
@Transactional
public class JpaPurchasingOperationRepository implements PurchasingOperationRepository {
    @PersistenceContext(unitName = PurchasingPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaPurchasingOperationRepository() { }

    JpaPurchasingOperationRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<PurchasingOperationRecord> find(
            CompanyId companyId, String idempotencyKey) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        return Optional.ofNullable(entityManager.find(
                        PurchasingOperationEntity.class,
                        new PurchasingOperationEntity.Key(
                                companyId.value(), idempotencyKey)))
                .map(PurchasingOperationEntity::record);
    }

    @Override
    public void append(PurchasingOperationRecord operation) {
        try {
            entityManager.persist(PurchasingOperationEntity.from(
                    Objects.requireNonNull(operation, "operation")));
            entityManager.flush();
        } catch (PersistenceException failure) {
            throw PostgreSqlPurchasingConflictMapper.map(failure);
        }
    }
}
