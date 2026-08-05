package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceCode;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceException;
import py.com.logixone.plugins.inventory.application.port.StockCountRepository;
import py.com.logixone.plugins.inventory.domain.StockCount;
import py.com.logixone.plugins.inventory.domain.StockCountLineSnapshot;
import py.com.logixone.plugins.inventory.domain.StockCountScope;
import py.com.logixone.plugins.inventory.domain.StockCountSnapshot;

@ApplicationScoped
@Transactional
public class JpaStockCountRepository implements StockCountRepository {
    @PersistenceContext(unitName = InventoryPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaStockCountRepository() {
    }

    JpaStockCountRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<StockCount> findById(CompanyId companyId, StockCountId countId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(countId, "countId");
        StockCountEntity root = entityManager.find(
                StockCountEntity.class, new StockCountEntity.Key(companyId.value(), countId.value()));
        return Optional.ofNullable(root).map(this::restore);
    }

    @Override
    public boolean blocks(CompanyId companyId, StockKey key) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(key, "key");
        return entityManager.createQuery(
                        "SELECT COUNT(stockCount) FROM StockCountEntity stockCount "
                                + "WHERE stockCount.companyId = :company "
                                + "AND stockCount.warehouseId = :warehouse "
                                + "AND stockCount.state IN :states "
                                + "AND (stockCount.stockLocationId IS NULL "
                                + "OR stockCount.stockLocationId = :location)",
                        Long.class)
                .setParameter("company", companyId.value())
                .setParameter("warehouse", key.warehouseId().value())
                .setParameter("location", key.locationId().value())
                .setParameter("states", List.of(
                        py.com.logixone.plugins.inventory.domain.StockCountState.COUNTING,
                        py.com.logixone.plugins.inventory.domain.StockCountState.REVIEW))
                .getSingleResult() > 0;
    }

    @Override
    public StockCount insert(StockCount count) {
        Objects.requireNonNull(count, "count");
        StockCountSnapshot snapshot = count.snapshot();
        try {
            entityManager.persist(StockCountEntity.from(snapshot));
            snapshot.lines().forEach(line -> entityManager.persist(StockCountLineEntity.from(
                    snapshot.companyId().value(), snapshot.id().value(), line)));
            entityManager.flush();
            return count;
        } catch (PersistenceException failure) {
            throw PostgreSqlInventoryConflictMapper.map(failure);
        }
    }

    @Override
    public StockCount update(StockCount count, long expectedPersistedVersion) {
        Objects.requireNonNull(count, "count");
        StockCountSnapshot snapshot = count.snapshot();
        if (expectedPersistedVersion < 0 || snapshot.version() != expectedPersistedVersion + 1) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.VERSION_CONFLICT);
        }
        StockCountEntity root = entityManager.find(
                StockCountEntity.class,
                new StockCountEntity.Key(snapshot.companyId().value(), snapshot.id().value()));
        if (root == null) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.NOT_FOUND);
        }
        if (root.version() != expectedPersistedVersion) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.VERSION_CONFLICT);
        }
        try {
            root.apply(snapshot);
            entityManager.lock(root, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            synchronizeLines(snapshot);
            entityManager.flush();
            return count;
        } catch (RuntimeException failure) {
            throw PostgreSqlInventoryConflictMapper.map(failure);
        }
    }

    private StockCount restore(StockCountEntity root) {
        List<StockCountLineSnapshot> lines = lines(root.companyId(), root.stockCountId()).stream()
                .map(StockCountLineEntity::snapshot).toList();
        StockCountScope scope = new StockCountScope(
                new WarehouseId(root.warehouseId()),
                Optional.ofNullable(root.stockLocationId()).map(StockLocationId::new));
        return StockCount.restore(new StockCountSnapshot(
                new CompanyId(root.companyId()), new StockCountId(root.stockCountId()),
                scope, root.state(), root.version(), lines));
    }

    private void synchronizeLines(StockCountSnapshot snapshot) {
        Map<Integer, StockCountLineEntity> existing = lines(
                        snapshot.companyId().value(), snapshot.id().value()).stream()
                .collect(Collectors.toMap(StockCountLineEntity::lineNumber, Function.identity()));
        for (StockCountLineSnapshot desired : snapshot.lines()) {
            StockCountLineEntity current = existing.remove(desired.lineNumber());
            if (current == null) {
                entityManager.persist(StockCountLineEntity.from(
                        snapshot.companyId().value(), snapshot.id().value(), desired));
            } else {
                current.apply(desired);
            }
        }
        if (!existing.isEmpty()) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.REFERENCE_CONFLICT);
        }
    }

    private List<StockCountLineEntity> lines(java.util.UUID company, java.util.UUID count) {
        return entityManager.createQuery(
                        "SELECT line FROM StockCountLineEntity line "
                                + "WHERE line.companyId = :company AND line.stockCountId = :count "
                                + "ORDER BY line.lineNumber",
                        StockCountLineEntity.class)
                .setParameter("company", company)
                .setParameter("count", count)
                .getResultList();
    }
}
