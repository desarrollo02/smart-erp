package py.com.logixone.plugins.inventory.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemReference;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionResult;
import py.com.logixone.plugins.inventory.api.ExpiryPolicy;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.MovementQuantity;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StockMovementDirection;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockMovementLine;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;
import py.com.logixone.plugins.inventory.api.StockMovementType;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.StockSourceReference;
import py.com.logixone.plugins.inventory.api.TrackingMode;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.port.InventoryBalanceRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryIdGenerator;
import py.com.logixone.plugins.inventory.application.port.InventoryItemRepository;
import py.com.logixone.plugins.inventory.application.port.StockCountRepository;
import py.com.logixone.plugins.inventory.application.port.StockMovementRepository;
import py.com.logixone.plugins.inventory.application.port.WarehouseRepository;
import py.com.logixone.plugins.inventory.domain.InventoryBalance;
import py.com.logixone.plugins.inventory.domain.InventoryItem;
import py.com.logixone.plugins.inventory.domain.StockCount;
import py.com.logixone.plugins.inventory.domain.StockMovementSnapshot;
import py.com.logixone.plugins.inventory.domain.Warehouse;

class InventoryMovementServiceTest {
    private static final CompanyId COMPANY = new CompanyId(uuid(1));
    private static final CatalogItemId CATALOG_ITEM = new CatalogItemId(uuid(2));
    private static final InventoryItemId ITEM = new InventoryItemId(uuid(3));
    private static final WarehouseId WAREHOUSE = new WarehouseId(uuid(4));
    private static final StockLocationId LOCATION = new StockLocationId(uuid(5));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-31T15:00:00Z"), ZoneOffset.UTC);

    private MemoryWarehouses warehouses;
    private MemoryItems items;
    private MemoryBalances balances;
    private MemoryMovements movements;
    private CountingIds ids;
    private List<TechnicalAuditEvent> audit;
    private InventoryMovementService service;

    @BeforeEach
    void setUp() {
        warehouses = new MemoryWarehouses();
        items = new MemoryItems();
        balances = new MemoryBalances();
        movements = new MemoryMovements();
        ids = new CountingIds();
        audit = new ArrayList<>();
        Warehouse warehouse = Warehouse.open(COMPANY, WAREHOUSE, LOCATION, "MAIN", "Central");
        warehouses.value = warehouse;
        items.value = InventoryItem.enroll(COMPANY, ITEM, new CatalogItemReference(
                CATALOG_ITEM, "SKU-1", "Private product", CatalogItemType.PRODUCT,
                CatalogItemState.ACTIVE, Set.of(CatalogItemScope.PURCHASE), "EA", 3),
                TrackingMode.NONE, ExpiryPolicy.NONE);
        service = new InventoryMovementService(
                warehouses, items, balances, movements, new UnlockedCounts(),
                (companyId, request) -> Optional.of(new CatalogUnitConversionResult(
                        request.itemId(), request.sourceUnitCode(), request.targetUnitCode(),
                        request.quantity(), BigDecimal.ONE, request.quantity(), 3)),
                ids, audit::add, CLOCK);
    }

    @Test
    void rejectsBeforeReadingBusinessStateWhenPermissionIsMissing() {
        var result = service.post(context(InventoryPermissions.VIEW), receipt("req-1", "5"));

        assertEquals(InventoryResultCode.ACCESS_DENIED, result.code());
        assertEquals(0, movements.lookups);
        assertEquals(0, ids.calls);
        assertEquals("ACCESS_DENIED", audit.getFirst().resultCode());
    }

    @Test
    void postsReceiptAndReturnsTheOriginalResultOnAnExactRetry() {
        StockMovementRequest request = receipt("req-2", "5");

        var first = service.post(context(InventoryPermissions.MOVEMENTS_POST), request);
        var retry = service.post(context(InventoryPermissions.MOVEMENTS_POST), request);

        assertTrue(first.successful());
        assertEquals(first.value(), retry.value());
        assertEquals(1, movements.values.size());
        assertEquals(1, ids.calls);
        assertEquals(0, new BigDecimal("5").compareTo(
                balances.find(COMPANY, key()).orElseThrow().physicalQuantity()));
        assertEquals("UNCHANGED", audit.getLast().outcome().name());
    }

    @Test
    void refusesIssueThatWouldConsumeReservedOrMissingStock() {
        var result = service.post(
                context(InventoryPermissions.MOVEMENTS_POST), issue("req-3", "1"));

        assertEquals(InventoryResultCode.INSUFFICIENT_STOCK, result.code());
        assertTrue(movements.values.isEmpty());
        assertTrue(balances.values.isEmpty());
    }

    private static StockMovementRequest receipt(String idempotencyKey, String quantity) {
        return movement(StockMovementType.RECEIPT, StockMovementDirection.INCREASE,
                idempotencyKey, quantity);
    }

    private static StockMovementRequest issue(String idempotencyKey, String quantity) {
        return movement(StockMovementType.ISSUE, StockMovementDirection.DECREASE,
                idempotencyKey, quantity);
    }

    private static StockMovementRequest movement(
            StockMovementType type,
            StockMovementDirection direction,
            String idempotencyKey,
            String quantity) {
        BigDecimal amount = new BigDecimal(quantity);
        return new StockMovementRequest(
                type, "MANUAL", new StockSourceReference("TEST", "source-1"),
                idempotencyKey, List.of(new StockMovementLine(
                        key(), direction,
                        new MovementQuantity("EA", amount, "EA", BigDecimal.ONE, amount, 3))),
                Optional.empty());
    }

    private static StockKey key() {
        return new StockKey(ITEM, WAREHOUSE, LOCATION, Optional.empty(), Optional.empty(),
                Optional.empty(), StockCondition.AVAILABLE);
    }

    private static InventoryOperationContext context(ContributionId permission) {
        return new InventoryOperationContext(
                new AuthenticatedCompanyContext(
                        new AuthenticatedActor(new AppUserId(uuid(99))), COMPANY),
                InventoryIdentity.PLUGIN_ID, permission, "request:movement");
    }

    private static UUID uuid(long suffix) { return new UUID(0, suffix); }

    private static final class MemoryWarehouses implements WarehouseRepository {
        private Warehouse value;
        @Override public Optional<Warehouse> findById(CompanyId companyId, WarehouseId id) {
            return value != null && value.companyId().equals(companyId) && value.id().equals(id)
                    ? Optional.of(value) : Optional.empty();
        }
        @Override public Warehouse insert(Warehouse warehouse) { value = warehouse; return warehouse; }
        @Override public Warehouse update(Warehouse warehouse, long version) { value = warehouse; return warehouse; }
    }

    private static final class MemoryItems implements InventoryItemRepository {
        private InventoryItem value;
        @Override public Optional<InventoryItem> findById(CompanyId companyId, InventoryItemId id) {
            return value != null && value.companyId().equals(companyId) && value.id().equals(id)
                    ? Optional.of(value) : Optional.empty();
        }
        @Override public Optional<InventoryItem> findByCatalogItemId(CompanyId companyId, CatalogItemId id) {
            return value != null && value.companyId().equals(companyId) && value.catalogItemId().equals(id)
                    ? Optional.of(value) : Optional.empty();
        }
        @Override public InventoryItem insert(InventoryItem item) { value = item; return item; }
        @Override public InventoryItem update(InventoryItem item, long version) { value = item; return item; }
    }

    private static final class MemoryBalances implements InventoryBalanceRepository {
        private final Map<StockKey, InventoryBalance> values = new HashMap<>();
        @Override public Optional<InventoryBalance> find(CompanyId companyId, StockKey key) {
            return Optional.ofNullable(values.get(key));
        }
        @Override public InventoryBalance insert(InventoryBalance balance) {
            values.put(balance.key(), balance); return balance;
        }
        @Override public InventoryBalance update(InventoryBalance balance, long version) {
            values.put(balance.key(), balance); return balance;
        }
        @Override public boolean hasQuantity(CompanyId companyId, WarehouseId id, Optional<StockLocationId> location) { return false; }
        @Override public boolean hasQuantity(CompanyId companyId, InventoryItemId id) { return false; }
    }

    private static final class MemoryMovements implements StockMovementRepository {
        private final List<StockMovementSnapshot> values = new ArrayList<>();
        private int lookups;
        @Override public Optional<StockMovementSnapshot> findById(CompanyId companyId, StockMovementId id) {
            return values.stream().filter(value -> value.companyId().equals(companyId)
                    && value.id().equals(id)).findFirst();
        }
        @Override public Optional<StockMovementSnapshot> findByIdempotencyKey(
                CompanyId companyId, String sourceType, String key) {
            lookups++;
            return values.stream().filter(value -> value.companyId().equals(companyId)
                    && value.request().source().sourceType().equals(sourceType)
                    && value.request().idempotencyKey().equals(key)).findFirst();
        }
        @Override public StockMovementSnapshot append(StockMovementSnapshot movement) {
            values.add(movement); return movement;
        }
    }

    private static final class UnlockedCounts implements StockCountRepository {
        @Override public Optional<StockCount> findById(CompanyId companyId, StockCountId id) { return Optional.empty(); }
        @Override public boolean blocks(CompanyId companyId, StockKey key) { return false; }
        @Override public StockCount insert(StockCount count) { return count; }
        @Override public StockCount update(StockCount count, long version) { return count; }
    }

    private static final class CountingIds implements InventoryIdGenerator {
        private int calls;
        @Override public WarehouseId nextWarehouseId() { return new WarehouseId(uuid(10)); }
        @Override public StockLocationId nextLocationId() { return new StockLocationId(uuid(11)); }
        @Override public InventoryItemId nextItemId() { return new InventoryItemId(uuid(12)); }
        @Override public StockMovementId nextMovementId() { calls++; return new StockMovementId(uuid(13)); }
        @Override public StockReservationId nextReservationId() { return new StockReservationId(uuid(14)); }
        @Override public StockCountId nextCountId() { return new StockCountId(uuid(15)); }
    }
}
