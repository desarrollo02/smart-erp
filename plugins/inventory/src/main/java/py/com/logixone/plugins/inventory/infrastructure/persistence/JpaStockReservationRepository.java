package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceCode;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceException;
import py.com.logixone.plugins.inventory.application.port.StockReservationRepository;
import py.com.logixone.plugins.inventory.domain.StockReservation;

@ApplicationScoped
@Transactional
public class JpaStockReservationRepository implements StockReservationRepository {
    @PersistenceContext(unitName = InventoryPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaStockReservationRepository() {
    }

    JpaStockReservationRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<StockReservation> findById(CompanyId companyId, StockReservationId reservationId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(reservationId, "reservationId");
        StockReservationEntity entity = entityManager.find(
                StockReservationEntity.class,
                new StockReservationEntity.Key(companyId.value(), reservationId.value()));
        return Optional.ofNullable(entity).map(value -> StockReservation.restore(value.snapshot()));
    }

    @Override
    public Optional<StockReservation> findByIdempotencyKey(
            CompanyId companyId, String sourceType, String idempotencyKey) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        return entityManager.createQuery(
                        "SELECT reservation FROM StockReservationEntity reservation "
                                + "WHERE reservation.companyId = :company "
                                + "AND reservation.sourceType = :sourceType "
                                + "AND reservation.idempotencyKey = :idempotencyKey",
                        StockReservationEntity.class)
                .setParameter("company", companyId.value())
                .setParameter("sourceType", sourceType)
                .setParameter("idempotencyKey", idempotencyKey)
                .getResultStream().findFirst()
                .map(StockReservationEntity::snapshot)
                .map(StockReservation::restore);
    }

    @Override
    public StockReservation insert(StockReservation reservation) {
        Objects.requireNonNull(reservation, "reservation");
        try {
            entityManager.persist(StockReservationEntity.from(reservation.snapshot()));
            entityManager.flush();
            return reservation;
        } catch (PersistenceException failure) {
            throw PostgreSqlInventoryConflictMapper.map(failure);
        }
    }

    @Override
    public StockReservation update(StockReservation reservation, long expectedPersistedVersion) {
        Objects.requireNonNull(reservation, "reservation");
        if (expectedPersistedVersion < 0 || reservation.version() != expectedPersistedVersion + 1) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.VERSION_CONFLICT);
        }
        StockReservationEntity entity = entityManager.find(
                StockReservationEntity.class,
                new StockReservationEntity.Key(reservation.companyId().value(), reservation.id().value()));
        if (entity == null) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.NOT_FOUND);
        }
        if (entity.version() != expectedPersistedVersion) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.VERSION_CONFLICT);
        }
        try {
            entity.apply(reservation.snapshot());
            entityManager.flush();
            return reservation;
        } catch (RuntimeException failure) {
            throw PostgreSqlInventoryConflictMapper.map(failure);
        }
    }
}
