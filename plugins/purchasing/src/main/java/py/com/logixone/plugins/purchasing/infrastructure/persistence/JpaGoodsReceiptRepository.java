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
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptLineId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptState;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.application.port.GoodsReceiptRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchasingPersistenceCode;
import py.com.logixone.plugins.purchasing.application.port.PurchasingPersistenceException;
import py.com.logixone.plugins.purchasing.domain.GoodsReceipt;

@ApplicationScoped
@Transactional
public class JpaGoodsReceiptRepository implements GoodsReceiptRepository {
    @PersistenceContext(unitName = PurchasingPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaGoodsReceiptRepository() {
    }

    JpaGoodsReceiptRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<GoodsReceipt> findById(CompanyId companyId, GoodsReceiptId receiptId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(receiptId, "receiptId");
        GoodsReceiptEntity entity = entityManager.find(
                GoodsReceiptEntity.class,
                new GoodsReceiptEntity.Key(companyId.value(), receiptId.value()));
        return Optional.ofNullable(entity).map(this::restore);
    }

    @Override
    public Optional<GoodsReceipt> findByNumber(CompanyId companyId, String number) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(number, "number");
        return entityManager.createQuery(
                        "SELECT receipt FROM GoodsReceiptEntity receipt "
                                + "WHERE receipt.companyId = :company AND receipt.number = :number",
                        GoodsReceiptEntity.class)
                .setParameter("company", companyId.value())
                .setParameter("number", number)
                .getResultStream().findFirst().map(this::restore);
    }

    @Override
    public List<GoodsReceipt> findByOrderId(CompanyId companyId, PurchaseOrderId orderId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(orderId, "orderId");
        return entityManager.createQuery(
                        "SELECT receipt FROM GoodsReceiptEntity receipt "
                                + "WHERE receipt.companyId = :company "
                                + "AND receipt.purchaseOrderId = :purchaseOrder "
                                + "ORDER BY receipt.number",
                        GoodsReceiptEntity.class)
                .setParameter("company", companyId.value())
                .setParameter("purchaseOrder", orderId.value())
                .getResultList().stream().map(this::restore).toList();
    }

    @Override
    public GoodsReceipt insert(GoodsReceipt receipt) {
        Objects.requireNonNull(receipt, "receipt");
        if (receipt.state() != GoodsReceiptState.DRAFT) {
            throw new PurchasingPersistenceException(
                    PurchasingPersistenceCode.IMMUTABLE_DOCUMENT);
        }
        GoodsReceipt.Snapshot snapshot = receipt.snapshot();
        try {
            entityManager.persist(GoodsReceiptEntity.from(snapshot));
            persistLines(snapshot);
            entityManager.flush();
            return receipt;
        } catch (PersistenceException failure) {
            throw PostgreSqlPurchasingConflictMapper.map(failure);
        }
    }

    @Override
    public GoodsReceipt update(GoodsReceipt receipt, long expectedPersistedVersion) {
        Objects.requireNonNull(receipt, "receipt");
        GoodsReceipt.Snapshot snapshot = receipt.snapshot();
        GoodsReceiptEntity entity = requireEntity(
                snapshot.companyId().value(), snapshot.id().value(), expectedPersistedVersion,
                snapshot.version());
        try {
            List<GoodsReceiptLineEntity> stored = lines(entity.companyId(), entity.id());
            if (stored.size() != snapshot.lines().size()) {
                throw new PurchasingPersistenceException(
                        PurchasingPersistenceCode.IMMUTABLE_DOCUMENT);
            }
            for (int index = 0; index < stored.size(); index++) {
                GoodsReceiptLineEntity line = stored.get(index);
                if (!line.snapshot().equals(snapshot.lines().get(index))) {
                    throw new PurchasingPersistenceException(
                            PurchasingPersistenceCode.IMMUTABLE_DOCUMENT);
                }
                line.applyMovement(Optional.ofNullable(snapshot.stockMovements().get(line.id())));
            }
            entityManager.flush();
            entity.apply(snapshot);
            entityManager.flush();
            return receipt;
        } catch (RuntimeException failure) {
            throw PostgreSqlPurchasingConflictMapper.map(failure);
        }
    }

    private GoodsReceiptEntity requireEntity(
            UUID companyId, UUID receiptId, long expectedVersion, long domainVersion) {
        if (expectedVersion < 0 || domainVersion != expectedVersion + 1) {
            throw new PurchasingPersistenceException(PurchasingPersistenceCode.VERSION_CONFLICT);
        }
        GoodsReceiptEntity entity = entityManager.find(
                GoodsReceiptEntity.class, new GoodsReceiptEntity.Key(companyId, receiptId));
        if (entity == null) {
            throw new PurchasingPersistenceException(PurchasingPersistenceCode.NOT_FOUND);
        }
        if (entity.version() != expectedVersion) {
            throw new PurchasingPersistenceException(PurchasingPersistenceCode.VERSION_CONFLICT);
        }
        return entity;
    }

    private GoodsReceipt restore(GoodsReceiptEntity entity) {
        List<GoodsReceiptLineEntity> stored = lines(entity.companyId(), entity.id());
        Map<GoodsReceiptLineId, StockMovementId> movements = new LinkedHashMap<>();
        stored.forEach(line -> line.movementId().ifPresent(value -> movements.put(line.id(), value)));
        return GoodsReceipt.restore(entity.snapshot(
                stored.stream().map(GoodsReceiptLineEntity::snapshot).toList(), movements));
    }

    private List<GoodsReceiptLineEntity> lines(UUID companyId, UUID receiptId) {
        return entityManager.createQuery(
                        "SELECT line FROM GoodsReceiptLineEntity line "
                                + "WHERE line.companyId = :company AND line.goodsReceiptId = :receipt "
                                + "ORDER BY line.position",
                        GoodsReceiptLineEntity.class)
                .setParameter("company", companyId)
                .setParameter("receipt", receiptId)
                .getResultList();
    }

    private void persistLines(GoodsReceipt.Snapshot snapshot) {
        for (int index = 0; index < snapshot.lines().size(); index++) {
            GoodsReceipt.Line line = snapshot.lines().get(index);
            entityManager.persist(GoodsReceiptLineEntity.from(
                    snapshot.companyId().value(), snapshot.id().value(), snapshot.orderId().value(),
                    index + 1, line,
                    Optional.ofNullable(snapshot.stockMovements().get(line.id()))));
        }
    }
}
