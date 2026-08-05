package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.port.InventoryBalanceRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceCode;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceException;
import py.com.logixone.plugins.inventory.domain.InventoryBalance;

@ApplicationScoped
@Transactional
public class JpaInventoryBalanceRepository implements InventoryBalanceRepository {
    @PersistenceContext(unitName = InventoryPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaInventoryBalanceRepository() {
    }

    JpaInventoryBalanceRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<InventoryBalance> find(CompanyId companyId, StockKey key) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(key, "key");
        return balanceRows(companyId, key).stream().findFirst()
                .map(InventoryBalanceEntity::snapshot)
                .map(InventoryBalance::restore);
    }

    @Override
    public InventoryBalance insert(InventoryBalance balance) {
        Objects.requireNonNull(balance, "balance");
        try {
            entityManager.persist(InventoryBalanceEntity.from(UUID.randomUUID(), balance.snapshot()));
            entityManager.flush();
            return balance;
        } catch (PersistenceException failure) {
            throw PostgreSqlInventoryConflictMapper.map(failure);
        }
    }

    @Override
    public InventoryBalance update(InventoryBalance balance, long expectedPersistedVersion) {
        Objects.requireNonNull(balance, "balance");
        if (expectedPersistedVersion < 0 || balance.version() != expectedPersistedVersion + 1) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.VERSION_CONFLICT);
        }
        InventoryBalanceEntity entity = balanceRows(balance.companyId(), balance.key()).stream()
                .findFirst().orElseThrow(() -> new InventoryPersistenceException(InventoryPersistenceCode.NOT_FOUND));
        if (entity.version() != expectedPersistedVersion) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.VERSION_CONFLICT);
        }
        try {
            entity.apply(balance.snapshot());
            entityManager.flush();
            return balance;
        } catch (RuntimeException failure) {
            throw PostgreSqlInventoryConflictMapper.map(failure);
        }
    }

    @Override
    public boolean hasQuantity(
            CompanyId companyId,
            WarehouseId warehouseId,
            Optional<StockLocationId> locationId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(warehouseId, "warehouseId");
        Objects.requireNonNull(locationId, "locationId");
        String locationClause = locationId.isPresent()
                ? " AND balance.stockLocationId = :location" : "";
        var query = entityManager.createQuery(
                        "SELECT COUNT(balance) FROM InventoryBalanceEntity balance "
                                + "WHERE balance.companyId = :company "
                                + "AND balance.warehouseId = :warehouse "
                                + "AND (balance.physicalQuantity > 0 OR balance.reservedQuantity > 0)"
                                + locationClause,
                        Long.class)
                .setParameter("company", companyId.value())
                .setParameter("warehouse", warehouseId.value());
        locationId.ifPresent(value -> query.setParameter("location", value.value()));
        return query.getSingleResult() > 0;
    }

    @Override
    public boolean hasQuantity(CompanyId companyId, InventoryItemId inventoryItemId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(inventoryItemId, "inventoryItemId");
        return entityManager.createQuery(
                        "SELECT COUNT(balance) FROM InventoryBalanceEntity balance "
                                + "WHERE balance.companyId = :company "
                                + "AND balance.inventoryItemId = :item "
                                + "AND (balance.physicalQuantity > 0 OR balance.reservedQuantity > 0)",
                        Long.class)
                .setParameter("company", companyId.value())
                .setParameter("item", inventoryItemId.value())
                .getSingleResult() > 0;
    }

    private List<InventoryBalanceEntity> balanceRows(CompanyId companyId, StockKey key) {
        return entityManager.createQuery("""
                        SELECT balance FROM InventoryBalanceEntity balance
                        WHERE balance.companyId = :company
                          AND balance.inventoryItemId = :item
                          AND balance.warehouseId = :warehouse
                          AND balance.stockLocationId = :location
                          AND ((:lot IS NULL AND balance.lotCode IS NULL) OR balance.lotCode = :lot)
                          AND ((:serial IS NULL AND balance.serialNumber IS NULL) OR balance.serialNumber = :serial)
                          AND ((:expiry IS NULL AND balance.expiryDate IS NULL) OR balance.expiryDate = :expiry)
                          AND balance.condition = :condition
                        """, InventoryBalanceEntity.class)
                .setParameter("company", companyId.value())
                .setParameter("item", key.inventoryItemId().value())
                .setParameter("warehouse", key.warehouseId().value())
                .setParameter("location", key.locationId().value())
                .setParameter("lot", key.lotCode().orElse(null))
                .setParameter("serial", key.serialNumber().orElse(null))
                .setParameter("expiry", key.expiryDate().orElse(null))
                .setParameter("condition", key.condition())
                .setMaxResults(2)
                .getResultList();
    }
}
