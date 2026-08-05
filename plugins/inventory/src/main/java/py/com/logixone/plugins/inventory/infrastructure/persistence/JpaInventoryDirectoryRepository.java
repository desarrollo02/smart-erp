package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.port.InventoryDirectoryRepository;
import py.com.logixone.plugins.inventory.application.query.InventoryDirectoryQueries;
import py.com.logixone.plugins.inventory.domain.StockCountScope;
import py.com.logixone.plugins.inventory.domain.StockLocationSnapshot;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;

/** JPA read model for compact inventory directories. It never mutates aggregates. */
@ApplicationScoped
@Transactional(TxType.SUPPORTS)
public class JpaInventoryDirectoryRepository implements InventoryDirectoryRepository {
    @PersistenceContext(unitName = InventoryPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaInventoryDirectoryRepository() {
    }

    JpaInventoryDirectoryRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public InventoryDirectoryQueries.Page<WarehouseSnapshot> warehouses(
            CompanyId companyId, InventoryDirectoryQueries.Criteria criteria) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(criteria, "criteria");
        String predicate = warehousePredicate(criteria);
        var pageQuery = entityManager.createQuery(
                "SELECT warehouse FROM WarehouseEntity warehouse WHERE " + predicate
                        + " ORDER BY LOWER(warehouse.name), warehouse.warehouseId",
                WarehouseEntity.class);
        var countQuery = entityManager.createQuery(
                "SELECT COUNT(warehouse) FROM WarehouseEntity warehouse WHERE " + predicate,
                Long.class);
        bindDirectory(pageQuery, companyId, criteria);
        bindDirectory(countQuery, companyId, criteria);
        List<WarehouseSnapshot> items = pageQuery
                .setFirstResult(criteria.offset())
                .setMaxResults(criteria.limit())
                .getResultList().stream()
                .map(this::warehouse)
                .toList();
        return new InventoryDirectoryQueries.Page<>(
                items, countQuery.getSingleResult(), criteria.offset(), criteria.limit());
    }

    @Override
    public InventoryDirectoryQueries.Page<InventoryDirectoryQueries.ItemSummary> items(
            CompanyId companyId, InventoryDirectoryQueries.Criteria criteria) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(criteria, "criteria");
        String predicate = itemPredicate(criteria);
        var pageQuery = entityManager.createQuery(
                "SELECT item FROM InventoryItemEntity item WHERE " + predicate
                        + " ORDER BY LOWER(item.catalogName), item.inventoryItemId",
                InventoryItemEntity.class);
        var countQuery = entityManager.createQuery(
                "SELECT COUNT(item) FROM InventoryItemEntity item WHERE " + predicate,
                Long.class);
        bindDirectory(pageQuery, companyId, criteria);
        bindDirectory(countQuery, companyId, criteria);
        List<InventoryDirectoryQueries.ItemSummary> items = pageQuery
                .setFirstResult(criteria.offset())
                .setMaxResults(criteria.limit())
                .getResultList().stream()
                .map(this::itemSummary)
                .toList();
        return new InventoryDirectoryQueries.Page<>(
                items, countQuery.getSingleResult(), criteria.offset(), criteria.limit());
    }

    @Override
    public Optional<InventoryDirectoryQueries.ItemSummary> item(
            CompanyId companyId, InventoryItemId inventoryItemId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(inventoryItemId, "inventoryItemId");
        InventoryItemEntity entity = entityManager.find(
                InventoryItemEntity.class,
                new InventoryItemEntity.Key(companyId.value(), inventoryItemId.value()));
        return Optional.ofNullable(entity).map(this::itemSummary);
    }

    @Override
    public InventoryDirectoryQueries.Page<InventoryDirectoryQueries.CountSummary> counts(
            CompanyId companyId, InventoryDirectoryQueries.CountCriteria criteria) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(criteria, "criteria");
        String stateClause = criteria.state().isPresent() ? " AND stockCount.state = :state" : "";
        var pageQuery = entityManager.createQuery(
                "SELECT stockCount FROM StockCountEntity stockCount WHERE stockCount.companyId = :company"
                        + stateClause
                        + " ORDER BY stockCount.stockCountId DESC",
                StockCountEntity.class)
                .setParameter("company", companyId.value());
        var countQuery = entityManager.createQuery(
                "SELECT COUNT(stockCount) FROM StockCountEntity stockCount WHERE stockCount.companyId = :company"
                        + stateClause,
                Long.class)
                .setParameter("company", companyId.value());
        criteria.state().ifPresent(state -> {
            pageQuery.setParameter("state", state);
            countQuery.setParameter("state", state);
        });
        List<InventoryDirectoryQueries.CountSummary> items = pageQuery
                .setFirstResult(criteria.offset())
                .setMaxResults(criteria.limit())
                .getResultList().stream()
                .map(this::countSummary)
                .toList();
        return new InventoryDirectoryQueries.Page<>(
                items, countQuery.getSingleResult(), criteria.offset(), criteria.limit());
    }

    private WarehouseSnapshot warehouse(WarehouseEntity root) {
        List<StockLocationSnapshot> locations = entityManager.createQuery(
                "SELECT location FROM StockLocationEntity location "
                        + "WHERE location.companyId = :company AND location.warehouseId = :warehouse "
                        + "ORDER BY location.code",
                StockLocationEntity.class)
                .setParameter("company", root.companyId())
                .setParameter("warehouse", root.warehouseId())
                .getResultList().stream()
                .map(StockLocationEntity::snapshot)
                .toList();
        return new WarehouseSnapshot(
                new CompanyId(root.companyId()), new WarehouseId(root.warehouseId()),
                root.code(), root.name(), root.active(), root.version(), locations);
    }

    private InventoryDirectoryQueries.ItemSummary itemSummary(InventoryItemEntity item) {
        var snapshot = item.snapshot();
        Object[] totals = entityManager.createQuery("""
                        SELECT COALESCE(SUM(balance.physicalQuantity), 0),
                               COALESCE(SUM(balance.reservedQuantity), 0),
                               COUNT(balance)
                        FROM InventoryBalanceEntity balance
                        WHERE balance.companyId = :company
                          AND balance.inventoryItemId = :item
                        """, Object[].class)
                .setParameter("company", snapshot.companyId().value())
                .setParameter("item", snapshot.id().value())
                .getSingleResult();
        return new InventoryDirectoryQueries.ItemSummary(
                snapshot, decimal(totals[0]), decimal(totals[1]), ((Number) totals[2]).longValue());
    }

    private InventoryDirectoryQueries.CountSummary countSummary(StockCountEntity count) {
        long lines = entityManager.createQuery(
                        "SELECT COUNT(line) FROM StockCountLineEntity line "
                                + "WHERE line.companyId = :company AND line.stockCountId = :count",
                        Long.class)
                .setParameter("company", count.companyId())
                .setParameter("count", count.stockCountId())
                .getSingleResult();
        return new InventoryDirectoryQueries.CountSummary(
                new StockCountId(count.stockCountId()),
                new StockCountScope(
                        new WarehouseId(count.warehouseId()),
                        Optional.ofNullable(count.stockLocationId()).map(StockLocationId::new)),
                count.state(), count.version(), lines);
    }

    private static String warehousePredicate(InventoryDirectoryQueries.Criteria criteria) {
        StringBuilder result = new StringBuilder("warehouse.companyId = :company");
        if (criteria.text().isPresent()) {
            result.append(" AND (LOWER(warehouse.code) LIKE :pattern OR LOWER(warehouse.name) LIKE :pattern)");
        }
        if (criteria.active().isPresent()) result.append(" AND warehouse.active = :active");
        return result.toString();
    }

    private static String itemPredicate(InventoryDirectoryQueries.Criteria criteria) {
        StringBuilder result = new StringBuilder("item.companyId = :company");
        if (criteria.text().isPresent()) {
            result.append(" AND (LOWER(item.catalogCode) LIKE :pattern OR LOWER(item.catalogName) LIKE :pattern)");
        }
        if (criteria.active().isPresent()) result.append(" AND item.active = :active");
        return result.toString();
    }

    private static void bindDirectory(
            Query query, CompanyId companyId, InventoryDirectoryQueries.Criteria criteria) {
        query.setParameter("company", companyId.value());
        criteria.text().ifPresent(text -> query.setParameter("pattern", "%" + text + "%"));
        criteria.active().ifPresent(active -> query.setParameter("active", active));
    }

    private static BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : (BigDecimal) value;
    }
}
