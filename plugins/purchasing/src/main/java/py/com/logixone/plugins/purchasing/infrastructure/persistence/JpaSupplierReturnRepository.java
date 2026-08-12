package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnLineId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnState;
import py.com.logixone.plugins.purchasing.application.port.PurchasingPersistenceCode;
import py.com.logixone.plugins.purchasing.application.port.PurchasingPersistenceException;
import py.com.logixone.plugins.purchasing.application.port.SupplierReturnRepository;
import py.com.logixone.plugins.purchasing.domain.SupplierReturn;

@ApplicationScoped
@Transactional
public class JpaSupplierReturnRepository implements SupplierReturnRepository {
    @PersistenceContext(unitName = PurchasingPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaSupplierReturnRepository() {
    }

    JpaSupplierReturnRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<SupplierReturn> findById(
            CompanyId companyId, SupplierReturnId supplierReturnId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(supplierReturnId, "supplierReturnId");
        SupplierReturnEntity entity = entityManager.find(
                SupplierReturnEntity.class,
                new SupplierReturnEntity.Key(companyId.value(), supplierReturnId.value()));
        return Optional.ofNullable(entity).map(this::restore);
    }

    @Override
    public Optional<SupplierReturn> findByNumber(CompanyId companyId, String number) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(number, "number");
        return entityManager.createQuery(
                        "SELECT supplierReturn FROM SupplierReturnEntity supplierReturn "
                                + "WHERE supplierReturn.companyId = :company "
                                + "AND supplierReturn.number = :number",
                        SupplierReturnEntity.class)
                .setParameter("company", companyId.value())
                .setParameter("number", number)
                .getResultStream().findFirst().map(this::restore);
    }

    @Override
    public List<SupplierReturn> findByOrderId(CompanyId companyId, PurchaseOrderId orderId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(orderId, "orderId");
        return entityManager.createQuery(
                        "SELECT supplierReturn FROM SupplierReturnEntity supplierReturn "
                                + "WHERE supplierReturn.companyId = :company "
                                + "AND supplierReturn.purchaseOrderId = :purchaseOrder "
                                + "ORDER BY supplierReturn.number",
                        SupplierReturnEntity.class)
                .setParameter("company", companyId.value())
                .setParameter("purchaseOrder", orderId.value())
                .getResultList().stream().map(this::restore).toList();
    }

    @Override
    public SupplierReturn insert(SupplierReturn supplierReturn) {
        Objects.requireNonNull(supplierReturn, "supplierReturn");
        if (supplierReturn.state() != SupplierReturnState.DRAFT) {
            throw new PurchasingPersistenceException(
                    PurchasingPersistenceCode.IMMUTABLE_DOCUMENT);
        }
        SupplierReturn.Snapshot snapshot = supplierReturn.snapshot();
        try {
            entityManager.persist(SupplierReturnEntity.from(snapshot));
            persistLines(snapshot);
            entityManager.flush();
            return supplierReturn;
        } catch (PersistenceException failure) {
            throw PostgreSqlPurchasingConflictMapper.map(failure);
        }
    }

    @Override
    public SupplierReturn update(SupplierReturn supplierReturn, long expectedPersistedVersion) {
        Objects.requireNonNull(supplierReturn, "supplierReturn");
        SupplierReturn.Snapshot snapshot = supplierReturn.snapshot();
        SupplierReturnEntity entity = requireEntity(
                snapshot.companyId().value(), snapshot.id().value(), expectedPersistedVersion,
                snapshot.version());
        try {
            List<SupplierReturnLineEntity> stored = lines(entity.companyId(), entity.id());
            if (stored.size() != snapshot.lines().size()) {
                throw new PurchasingPersistenceException(
                        PurchasingPersistenceCode.IMMUTABLE_DOCUMENT);
            }
            for (int index = 0; index < stored.size(); index++) {
                SupplierReturnLineEntity line = stored.get(index);
                if (!line.snapshot().equals(snapshot.lines().get(index))) {
                    throw new PurchasingPersistenceException(
                            PurchasingPersistenceCode.IMMUTABLE_DOCUMENT);
                }
                line.applyMovement(Optional.ofNullable(snapshot.stockMovements().get(line.id())));
            }
            entityManager.flush();
            entity.apply(snapshot);
            entityManager.flush();
            return supplierReturn;
        } catch (RuntimeException failure) {
            throw PostgreSqlPurchasingConflictMapper.map(failure);
        }
    }

    private SupplierReturnEntity requireEntity(
            UUID companyId, UUID supplierReturnId, long expectedVersion, long domainVersion) {
        if (expectedVersion < 0 || domainVersion != expectedVersion + 1) {
            throw new PurchasingPersistenceException(PurchasingPersistenceCode.VERSION_CONFLICT);
        }
        SupplierReturnEntity entity = entityManager.find(
                SupplierReturnEntity.class,
                new SupplierReturnEntity.Key(companyId, supplierReturnId));
        if (entity == null) {
            throw new PurchasingPersistenceException(PurchasingPersistenceCode.NOT_FOUND);
        }
        if (entity.version() != expectedVersion) {
            throw new PurchasingPersistenceException(PurchasingPersistenceCode.VERSION_CONFLICT);
        }
        return entity;
    }

    private SupplierReturn restore(SupplierReturnEntity entity) {
        List<SupplierReturnLineEntity> stored = lines(entity.companyId(), entity.id());
        Map<SupplierReturnLineId, StockMovementId> movements = new LinkedHashMap<>();
        stored.forEach(line -> line.movementId().ifPresent(value -> movements.put(line.id(), value)));
        return SupplierReturn.restore(entity.snapshot(
                stored.stream().map(SupplierReturnLineEntity::snapshot).toList(), movements));
    }

    private List<SupplierReturnLineEntity> lines(UUID companyId, UUID supplierReturnId) {
        return entityManager.createQuery(
                        "SELECT line FROM SupplierReturnLineEntity line "
                                + "WHERE line.companyId = :company "
                                + "AND line.supplierReturnId = :supplierReturn "
                                + "ORDER BY line.position",
                        SupplierReturnLineEntity.class)
                .setParameter("company", companyId)
                .setParameter("supplierReturn", supplierReturnId)
                .getResultList();
    }

    private void persistLines(SupplierReturn.Snapshot snapshot) {
        for (int index = 0; index < snapshot.lines().size(); index++) {
            SupplierReturn.Line line = snapshot.lines().get(index);
            entityManager.persist(SupplierReturnLineEntity.from(
                    snapshot.companyId().value(), snapshot.id().value(), snapshot.orderId().value(),
                    index + 1, line,
                    Optional.ofNullable(snapshot.stockMovements().get(line.id()))));
        }
    }
}
