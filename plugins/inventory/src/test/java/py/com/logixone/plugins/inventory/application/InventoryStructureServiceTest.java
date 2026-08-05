package py.com.logixone.plugins.inventory.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemReference;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchPage;
import py.com.logixone.plugins.inventory.api.ExpiryPolicy;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.TrackingMode;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.command.InventoryCommands;
import py.com.logixone.plugins.inventory.application.port.InventoryBalanceRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryIdGenerator;
import py.com.logixone.plugins.inventory.application.port.InventoryItemRepository;
import py.com.logixone.plugins.inventory.application.port.WarehouseRepository;
import py.com.logixone.plugins.inventory.domain.InventoryBalance;
import py.com.logixone.plugins.inventory.domain.InventoryItem;
import py.com.logixone.plugins.inventory.domain.Warehouse;

class InventoryStructureServiceTest {
    private static final CompanyId COMPANY = new CompanyId(uuid(1));
    private static final CatalogItemId CATALOG_ITEM = new CatalogItemId(uuid(20));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-31T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void rejectsWrongPermissionBeforeCatalogIdentityOrRepositories() {
        MemoryWarehouses warehouses = new MemoryWarehouses();
        MemoryItems items = new MemoryItems();
        CountingIds ids = new CountingIds();
        CountingCatalog catalog = new CountingCatalog();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        InventoryStructureService service = service(
                warehouses, items, new MemoryBalances(), catalog, ids, audit);

        var result = service.enrollItem(
                context(InventoryPermissions.VIEW),
                new InventoryCommands.EnrollItem(
                        CATALOG_ITEM, TrackingMode.NONE, ExpiryPolicy.NONE));

        assertEquals(InventoryResultCode.ACCESS_DENIED, result.code());
        assertEquals(0, ids.calls);
        assertEquals(0, catalog.calls);
        assertEquals(0, items.insertions);
        assertEquals("ACCESS_DENIED", audit.getFirst().resultCode());
    }

    @Test
    void opensWarehouseWithGeneralLocationAndTechnicalAudit() {
        MemoryWarehouses warehouses = new MemoryWarehouses();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        InventoryStructureService service = service(
                warehouses, new MemoryItems(), new MemoryBalances(),
                new CountingCatalog(), new CountingIds(), audit);

        var result = service.openWarehouse(
                context(InventoryPermissions.STORAGE_MANAGE),
                new InventoryCommands.OpenWarehouse(" main ", "Central privado"));

        assertTrue(result.successful());
        assertEquals("MAIN", result.value().orElseThrow().code());
        assertEquals("GENERAL", result.value().orElseThrow().locations().getFirst().code());
        assertEquals(1, warehouses.insertions);
        assertEquals("OPEN_WAREHOUSE", audit.getFirst().operation());
        assertFalse(audit.getFirst().toString().contains("Central privado"));
    }

    @Test
    void enrollsOnlyTheCatalogReferenceResolvedForTheTrustedCompany() {
        MemoryItems items = new MemoryItems();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        InventoryStructureService service = service(
                new MemoryWarehouses(), items, new MemoryBalances(),
                new CountingCatalog(), new CountingIds(), audit);

        var result = service.enrollItem(
                context(InventoryPermissions.ITEMS_MANAGE),
                new InventoryCommands.EnrollItem(
                        CATALOG_ITEM, TrackingMode.NONE, ExpiryPolicy.NONE));

        assertTrue(result.successful());
        assertEquals(CATALOG_ITEM, result.value().orElseThrow().catalogItemId());
        assertEquals(1, items.insertions);
        assertEquals("ENROLL_INVENTORY_ITEM", audit.getFirst().operation());
    }

    @Test
    void refusesToInactivateAnItemThatStillOwnsQuantity() {
        MemoryItems items = new MemoryItems();
        InventoryItem item = InventoryItem.enroll(
                COMPANY, new InventoryItemId(uuid(12)), catalogReference(),
                TrackingMode.NONE, ExpiryPolicy.NONE);
        items.insert(item);
        MemoryBalances balances = new MemoryBalances();
        balances.hasItemQuantity = true;
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        InventoryStructureService service = service(
                new MemoryWarehouses(), items, balances,
                new CountingCatalog(), new CountingIds(), audit);

        var result = service.inactivateItem(
                context(InventoryPermissions.ITEMS_MANAGE),
                new InventoryCommands.InactivateItem(item.id(), 0));

        assertEquals(InventoryResultCode.INVALID_OPERATION, result.code());
        assertTrue(items.findById(COMPANY, item.id()).orElseThrow().active());
        assertEquals("REJECTED", audit.getLast().outcome().name());
    }

    private static InventoryStructureService service(
            MemoryWarehouses warehouses,
            MemoryItems items,
            MemoryBalances balances,
            CountingCatalog catalog,
            CountingIds ids,
            List<TechnicalAuditEvent> audit) {
        return new InventoryStructureService(
                warehouses, items, balances, catalog, ids, audit::add, CLOCK);
    }

    private static InventoryOperationContext context(ContributionId permission) {
        return new InventoryOperationContext(
                new AuthenticatedCompanyContext(
                        new AuthenticatedActor(new AppUserId(uuid(99))), COMPANY),
                InventoryIdentity.PLUGIN_ID, permission, "request:inventory-structure");
    }

    private static CatalogItemReference catalogReference() {
        return new CatalogItemReference(
                CATALOG_ITEM, "SKU-1", "Private product", CatalogItemType.PRODUCT,
                CatalogItemState.ACTIVE, Set.of(CatalogItemScope.PURCHASE), "EA", 3);
    }

    private static UUID uuid(long suffix) { return new UUID(0, suffix); }

    private static final class CountingCatalog implements CatalogItemDirectory {
        private int calls;

        @Override
        public Optional<CatalogItemReference> findById(CompanyId companyId, CatalogItemId itemId) {
            calls++;
            return companyId.equals(COMPANY) && itemId.equals(CATALOG_ITEM)
                    ? Optional.of(catalogReference()) : Optional.empty();
        }

        @Override
        public CatalogSearchPage search(CompanyId companyId, CatalogSearchCriteria criteria) {
            calls++;
            return new CatalogSearchPage(List.of(), 0, criteria.offset(), criteria.limit());
        }
    }

    private static final class CountingIds implements InventoryIdGenerator {
        private int calls;
        @Override public WarehouseId nextWarehouseId() { calls++; return new WarehouseId(uuid(10)); }
        @Override public StockLocationId nextLocationId() { calls++; return new StockLocationId(uuid(11)); }
        @Override public InventoryItemId nextItemId() { calls++; return new InventoryItemId(uuid(12)); }
        @Override public StockMovementId nextMovementId() { calls++; return new StockMovementId(uuid(13)); }
        @Override public StockReservationId nextReservationId() { calls++; return new StockReservationId(uuid(14)); }
        @Override public StockCountId nextCountId() { calls++; return new StockCountId(uuid(15)); }
    }

    private static final class MemoryWarehouses implements WarehouseRepository {
        private final Map<String, Warehouse> values = new HashMap<>();
        private int insertions;
        @Override public Optional<Warehouse> findById(CompanyId companyId, WarehouseId warehouseId) {
            return Optional.ofNullable(values.get(companyId + ":" + warehouseId));
        }
        @Override public Warehouse insert(Warehouse warehouse) {
            insertions++;
            values.put(warehouse.companyId() + ":" + warehouse.id(), warehouse);
            return warehouse;
        }
        @Override public Warehouse update(Warehouse warehouse, long expectedPersistedVersion) {
            values.put(warehouse.companyId() + ":" + warehouse.id(), warehouse);
            return warehouse;
        }
    }

    private static final class MemoryItems implements InventoryItemRepository {
        private final Map<String, InventoryItem> values = new HashMap<>();
        private int insertions;
        @Override public Optional<InventoryItem> findById(CompanyId companyId, InventoryItemId itemId) {
            return Optional.ofNullable(values.get(companyId + ":" + itemId));
        }
        @Override public Optional<InventoryItem> findByCatalogItemId(
                CompanyId companyId, CatalogItemId catalogItemId) {
            return values.values().stream().filter(item -> item.companyId().equals(companyId)
                    && item.catalogItemId().equals(catalogItemId)).findFirst();
        }
        @Override public InventoryItem insert(InventoryItem item) {
            insertions++;
            values.put(item.companyId() + ":" + item.id(), item);
            return item;
        }
        @Override public InventoryItem update(InventoryItem item, long expectedPersistedVersion) {
            values.put(item.companyId() + ":" + item.id(), item);
            return item;
        }
    }

    private static final class MemoryBalances implements InventoryBalanceRepository {
        private boolean hasItemQuantity;
        @Override public Optional<InventoryBalance> find(CompanyId companyId, StockKey key) {
            return Optional.empty();
        }
        @Override public InventoryBalance insert(InventoryBalance balance) { return balance; }
        @Override public InventoryBalance update(InventoryBalance balance, long expected) { return balance; }
        @Override public boolean hasQuantity(
                CompanyId companyId, WarehouseId warehouseId, Optional<StockLocationId> locationId) {
            return false;
        }
        @Override public boolean hasQuantity(CompanyId companyId, InventoryItemId itemId) {
            return hasItemQuantity;
        }
    }
}
