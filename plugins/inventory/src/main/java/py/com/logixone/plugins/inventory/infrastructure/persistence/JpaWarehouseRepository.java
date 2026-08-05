package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceCode;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceException;
import py.com.logixone.plugins.inventory.application.port.WarehouseRepository;
import py.com.logixone.plugins.inventory.domain.StockLocationSnapshot;
import py.com.logixone.plugins.inventory.domain.Warehouse;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;

@ApplicationScoped
@Transactional
public class JpaWarehouseRepository implements WarehouseRepository {
    @PersistenceContext(unitName = InventoryPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaWarehouseRepository() {
    }

    JpaWarehouseRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<Warehouse> findById(CompanyId companyId, WarehouseId warehouseId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(warehouseId, "warehouseId");
        WarehouseEntity root = entityManager.find(
                WarehouseEntity.class, new WarehouseEntity.Key(companyId.value(), warehouseId.value()));
        return Optional.ofNullable(root).map(this::restore);
    }

    @Override
    public Warehouse insert(Warehouse warehouse) {
        Objects.requireNonNull(warehouse, "warehouse");
        WarehouseSnapshot snapshot = warehouse.snapshot();
        try {
            entityManager.persist(WarehouseEntity.from(snapshot));
            snapshot.locations().forEach(location -> entityManager.persist(StockLocationEntity.from(location)));
            entityManager.flush();
            return warehouse;
        } catch (PersistenceException failure) {
            throw PostgreSqlInventoryConflictMapper.map(failure);
        }
    }

    @Override
    public Warehouse update(Warehouse warehouse, long expectedPersistedVersion) {
        Objects.requireNonNull(warehouse, "warehouse");
        WarehouseSnapshot snapshot = warehouse.snapshot();
        requireNextVersion(snapshot.version(), expectedPersistedVersion);
        WarehouseEntity root = entityManager.find(
                WarehouseEntity.class, new WarehouseEntity.Key(
                        snapshot.companyId().value(), snapshot.id().value()));
        if (root == null) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.NOT_FOUND);
        }
        if (root.version() != expectedPersistedVersion) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.VERSION_CONFLICT);
        }
        try {
            root.apply(snapshot);
            entityManager.lock(root, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            synchronizeLocations(snapshot);
            entityManager.flush();
            return warehouse;
        } catch (RuntimeException failure) {
            throw PostgreSqlInventoryConflictMapper.map(failure);
        }
    }

    private Warehouse restore(WarehouseEntity root) {
        List<StockLocationSnapshot> locations = entityManager.createQuery(
                        "SELECT location FROM StockLocationEntity location "
                                + "WHERE location.companyId = :company AND location.warehouseId = :warehouse "
                                + "ORDER BY location.code",
                        StockLocationEntity.class)
                .setParameter("company", root.companyId())
                .setParameter("warehouse", root.warehouseId())
                .getResultList().stream().map(StockLocationEntity::snapshot).toList();
        return Warehouse.restore(new WarehouseSnapshot(
                new CompanyId(root.companyId()), new WarehouseId(root.warehouseId()),
                root.code(), root.name(), root.active(), root.version(), locations));
    }

    private void synchronizeLocations(WarehouseSnapshot snapshot) {
        UUID company = snapshot.companyId().value();
        UUID warehouse = snapshot.id().value();
        Map<UUID, StockLocationEntity> existing = entityManager.createQuery(
                        "SELECT location FROM StockLocationEntity location "
                                + "WHERE location.companyId = :company AND location.warehouseId = :warehouse",
                        StockLocationEntity.class)
                .setParameter("company", company)
                .setParameter("warehouse", warehouse)
                .getResultList().stream().collect(Collectors.toMap(
                        StockLocationEntity::stockLocationId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
        for (StockLocationSnapshot desired : snapshot.locations()) {
            StockLocationEntity current = existing.remove(desired.id().value());
            if (current == null) {
                entityManager.persist(StockLocationEntity.from(desired));
            } else if (desired.version() == current.version() + 1) {
                current.apply(desired);
            } else if (desired.version() != current.version()) {
                throw new InventoryPersistenceException(InventoryPersistenceCode.VERSION_CONFLICT);
            }
        }
        if (!existing.isEmpty()) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.REFERENCE_CONFLICT);
        }
    }

    private static void requireNextVersion(long current, long expected) {
        if (expected < 0 || current != expected + 1) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.VERSION_CONFLICT);
        }
    }
}
