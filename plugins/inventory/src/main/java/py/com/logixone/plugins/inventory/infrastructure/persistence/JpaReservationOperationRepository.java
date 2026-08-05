package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.application.port.ReservationOperationRepository;
import py.com.logixone.plugins.inventory.domain.ReservationOperation;

@ApplicationScoped
@Transactional
public class JpaReservationOperationRepository implements ReservationOperationRepository {
    @PersistenceContext(unitName = InventoryPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaReservationOperationRepository() {
    }

    JpaReservationOperationRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<ReservationOperation> findByIdempotencyKey(
            CompanyId companyId, String idempotencyKey) {
        Objects.requireNonNull(companyId, "companyId");
        String canonicalKey = ReservationOperation.canonicalIdempotencyKey(idempotencyKey);
        ReservationOperationEntity entity = entityManager.find(
                ReservationOperationEntity.class,
                new ReservationOperationEntity.Key(companyId.value(), canonicalKey));
        return Optional.ofNullable(entity).map(ReservationOperationEntity::snapshot);
    }

    @Override
    public ReservationOperation append(ReservationOperation operation) {
        Objects.requireNonNull(operation, "operation");
        try {
            entityManager.persist(ReservationOperationEntity.from(operation));
            entityManager.flush();
            return operation;
        } catch (PersistenceException failure) {
            throw PostgreSqlInventoryConflictMapper.map(failure);
        }
    }
}
