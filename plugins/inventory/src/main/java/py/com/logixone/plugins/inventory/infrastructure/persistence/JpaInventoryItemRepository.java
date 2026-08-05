package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.application.port.InventoryItemRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceCode;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceException;
import py.com.logixone.plugins.inventory.domain.InventoryItem;

@ApplicationScoped
@Transactional
public class JpaInventoryItemRepository implements InventoryItemRepository {
    @PersistenceContext(unitName = InventoryPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaInventoryItemRepository() {
    }

    JpaInventoryItemRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<InventoryItem> findById(CompanyId companyId, InventoryItemId inventoryItemId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(inventoryItemId, "inventoryItemId");
        InventoryItemEntity entity = entityManager.find(
                InventoryItemEntity.class,
                new InventoryItemEntity.Key(companyId.value(), inventoryItemId.value()));
        return Optional.ofNullable(entity).map(value -> InventoryItem.restore(value.snapshot()));
    }

    @Override
    public Optional<InventoryItem> findByCatalogItemId(
            CompanyId companyId, CatalogItemId catalogItemId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(catalogItemId, "catalogItemId");
        return entityManager.createQuery(
                        "SELECT item FROM InventoryItemEntity item "
                                + "WHERE item.companyId = :company AND item.catalogItemId = :catalogItem",
                        InventoryItemEntity.class)
                .setParameter("company", companyId.value())
                .setParameter("catalogItem", catalogItemId.value())
                .getResultStream().findFirst()
                .map(InventoryItemEntity::snapshot)
                .map(InventoryItem::restore);
    }

    @Override
    public InventoryItem insert(InventoryItem item) {
        Objects.requireNonNull(item, "item");
        try {
            entityManager.persist(InventoryItemEntity.from(item.snapshot()));
            entityManager.flush();
            return item;
        } catch (PersistenceException failure) {
            throw PostgreSqlInventoryConflictMapper.map(failure);
        }
    }

    @Override
    public InventoryItem update(InventoryItem item, long expectedPersistedVersion) {
        Objects.requireNonNull(item, "item");
        if (expectedPersistedVersion < 0 || item.version() != expectedPersistedVersion + 1) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.VERSION_CONFLICT);
        }
        InventoryItemEntity entity = entityManager.find(
                InventoryItemEntity.class,
                new InventoryItemEntity.Key(item.companyId().value(), item.id().value()));
        if (entity == null) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.NOT_FOUND);
        }
        if (entity.version() != expectedPersistedVersion) {
            throw new InventoryPersistenceException(InventoryPersistenceCode.VERSION_CONFLICT);
        }
        try {
            entity.apply(item.snapshot());
            entityManager.flush();
            return item;
        } catch (RuntimeException failure) {
            throw PostgreSqlInventoryConflictMapper.map(failure);
        }
    }
}
