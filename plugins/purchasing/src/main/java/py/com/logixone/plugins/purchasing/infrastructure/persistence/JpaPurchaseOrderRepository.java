package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.application.port.PurchaseOrderRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchasingPersistenceCode;
import py.com.logixone.plugins.purchasing.application.port.PurchasingPersistenceException;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;

@ApplicationScoped
@Transactional
public class JpaPurchaseOrderRepository implements PurchaseOrderRepository {
    @PersistenceContext(unitName = PurchasingPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaPurchaseOrderRepository() {
    }

    JpaPurchaseOrderRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<PurchaseOrder> findById(CompanyId companyId, PurchaseOrderId orderId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(orderId, "orderId");
        PurchaseOrderEntity entity = entityManager.find(
                PurchaseOrderEntity.class,
                new PurchaseOrderEntity.Key(companyId.value(), orderId.value()));
        return Optional.ofNullable(entity).map(this::restore);
    }

    @Override
    public Optional<PurchaseOrder> findByNumber(CompanyId companyId, String number) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(number, "number");
        return entityManager.createQuery(
                        "SELECT purchaseOrder FROM PurchaseOrderEntity purchaseOrder "
                                + "WHERE purchaseOrder.companyId = :company "
                                + "AND purchaseOrder.number = :number",
                        PurchaseOrderEntity.class)
                .setParameter("company", companyId.value())
                .setParameter("number", number)
                .getResultStream().findFirst().map(this::restore);
    }

    @Override
    public PurchaseOrder insert(PurchaseOrder order) {
        Objects.requireNonNull(order, "order");
        if (order.state() != PurchaseOrderState.DRAFT) {
            throw new PurchasingPersistenceException(
                    PurchasingPersistenceCode.IMMUTABLE_DOCUMENT);
        }
        PurchaseOrder.Snapshot snapshot = order.snapshot();
        try {
            entityManager.persist(PurchaseOrderEntity.from(snapshot));
            for (int lineIndex = 0; lineIndex < snapshot.lines().size(); lineIndex++) {
                PurchaseOrder.LineSnapshot line = snapshot.lines().get(lineIndex);
                entityManager.persist(PurchaseOrderLineEntity.from(
                        snapshot.companyId().value(), snapshot.id().value(), lineIndex + 1, line));
                for (int allocationIndex = 0;
                        allocationIndex < line.allocations().size(); allocationIndex++) {
                    entityManager.persist(PurchaseOrderAllocationEntity.from(
                            snapshot.companyId().value(), snapshot.id().value(), line.id().value(),
                            allocationIndex + 1, line.allocations().get(allocationIndex)));
                }
            }
            entityManager.flush();
            return order;
        } catch (PersistenceException failure) {
            throw PostgreSqlPurchasingConflictMapper.map(failure);
        }
    }

    @Override
    public PurchaseOrder update(PurchaseOrder order, long expectedPersistedVersion) {
        Objects.requireNonNull(order, "order");
        PurchaseOrder.Snapshot snapshot = order.snapshot();
        PurchaseOrderEntity entity = requireEntity(
                snapshot.companyId().value(), snapshot.id().value(), expectedPersistedVersion,
                snapshot.version());
        try {
            List<PurchaseOrderLineEntity> storedLines = lines(entity.companyId(), entity.id());
            if (storedLines.size() != snapshot.lines().size()) {
                throw new PurchasingPersistenceException(
                        PurchasingPersistenceCode.IMMUTABLE_DOCUMENT);
            }
            for (int index = 0; index < storedLines.size(); index++) {
                PurchaseOrderLineEntity stored = storedLines.get(index);
                PurchaseOrder.LineSnapshot target = snapshot.lines().get(index);
                List<PurchaseOrder.Allocation> allocations = allocations(
                        entity.companyId(), entity.id(), stored.id());
                PurchaseOrder.LineSnapshot current = stored.snapshot(allocations);
                if (!immutablePartsMatch(current, target)) {
                    throw new PurchasingPersistenceException(
                            PurchasingPersistenceCode.IMMUTABLE_DOCUMENT);
                }
                stored.apply(target);
            }
            entity.apply(snapshot);
            entityManager.flush();
            return order;
        } catch (RuntimeException failure) {
            throw PostgreSqlPurchasingConflictMapper.map(failure);
        }
    }

    private PurchaseOrderEntity requireEntity(
            UUID companyId, UUID orderId, long expectedVersion, long domainVersion) {
        if (expectedVersion < 0 || domainVersion != expectedVersion + 1) {
            throw new PurchasingPersistenceException(PurchasingPersistenceCode.VERSION_CONFLICT);
        }
        PurchaseOrderEntity entity = entityManager.find(
                PurchaseOrderEntity.class, new PurchaseOrderEntity.Key(companyId, orderId));
        if (entity == null) {
            throw new PurchasingPersistenceException(PurchasingPersistenceCode.NOT_FOUND);
        }
        if (entity.version() != expectedVersion) {
            throw new PurchasingPersistenceException(PurchasingPersistenceCode.VERSION_CONFLICT);
        }
        return entity;
    }

    private PurchaseOrder restore(PurchaseOrderEntity entity) {
        List<PurchaseOrder.LineSnapshot> snapshots = lines(entity.companyId(), entity.id()).stream()
                .map(line -> line.snapshot(allocations(entity.companyId(), entity.id(), line.id())))
                .toList();
        return PurchaseOrder.restore(entity.snapshot(snapshots));
    }

    private List<PurchaseOrderLineEntity> lines(UUID companyId, UUID orderId) {
        return entityManager.createQuery(
                        "SELECT line FROM PurchaseOrderLineEntity line "
                                + "WHERE line.companyId = :company AND line.purchaseOrderId = :purchaseOrder "
                                + "ORDER BY line.position",
                        PurchaseOrderLineEntity.class)
                .setParameter("company", companyId)
                .setParameter("purchaseOrder", orderId)
                .getResultList();
    }

    private List<PurchaseOrder.Allocation> allocations(
            UUID companyId, UUID orderId, UUID orderLineId) {
        return entityManager.createQuery(
                        "SELECT allocation FROM PurchaseOrderAllocationEntity allocation "
                                + "WHERE allocation.companyId = :company "
                                + "AND allocation.purchaseOrderId = :purchaseOrder "
                                + "AND allocation.purchaseOrderLineId = :purchaseOrderLine "
                                + "ORDER BY allocation.position",
                        PurchaseOrderAllocationEntity.class)
                .setParameter("company", companyId)
                .setParameter("purchaseOrder", orderId)
                .setParameter("purchaseOrderLine", orderLineId)
                .getResultList().stream()
                .map(PurchaseOrderAllocationEntity::snapshot).toList();
    }

    private static boolean immutablePartsMatch(
            PurchaseOrder.LineSnapshot current, PurchaseOrder.LineSnapshot target) {
        if (!current.id().equals(target.id()) || !current.item().equals(target.item())
                || current.orderedQuantity().compareTo(target.orderedQuantity()) != 0
                || current.unitPrice().compareTo(target.unitPrice()) != 0
                || current.allocations().size() != target.allocations().size()) {
            return false;
        }
        for (int index = 0; index < current.allocations().size(); index++) {
            PurchaseOrder.Allocation left = current.allocations().get(index);
            PurchaseOrder.Allocation right = target.allocations().get(index);
            if (!left.requestId().equals(right.requestId())
                    || !left.requestLineId().equals(right.requestLineId())
                    || left.quantity().compareTo(right.quantity()) != 0) {
                return false;
            }
        }
        return true;
    }
}
