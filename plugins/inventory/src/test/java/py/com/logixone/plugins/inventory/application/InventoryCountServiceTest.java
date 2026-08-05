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
import py.com.logixone.plugins.inventory.api.StockCondition;
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
import py.com.logixone.plugins.inventory.application.port.StockCountRepository;
import py.com.logixone.plugins.inventory.application.port.StockMovementRepository;
import py.com.logixone.plugins.inventory.application.port.WarehouseRepository;
import py.com.logixone.plugins.inventory.domain.InventoryBalance;
import py.com.logixone.plugins.inventory.domain.InventoryItem;
import py.com.logixone.plugins.inventory.domain.StockCount;
import py.com.logixone.plugins.inventory.domain.StockCountScope;
import py.com.logixone.plugins.inventory.domain.StockMovementSnapshot;
import py.com.logixone.plugins.inventory.domain.Warehouse;

class InventoryCountServiceTest {
    private static final CompanyId COMPANY = new CompanyId(uuid(1));
    private static final InventoryItemId ITEM = new InventoryItemId(uuid(2));
    private static final WarehouseId WAREHOUSE = new WarehouseId(uuid(3));
    private static final StockLocationId LOCATION = new StockLocationId(uuid(4));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-31T16:00:00Z"), ZoneOffset.UTC);

    private MemoryBalances balances;
    private MemoryCounts counts;
    private MemoryMovements movements;
    private InventoryCountService service;

    @BeforeEach
    void setUp() {
        Warehouse warehouse = Warehouse.open(COMPANY, WAREHOUSE, LOCATION, "MAIN", "Central");
        InventoryItem item = InventoryItem.enroll(COMPANY, ITEM, new CatalogItemReference(
                new CatalogItemId(uuid(5)), "SKU-1", "Private product", CatalogItemType.PRODUCT,
                CatalogItemState.ACTIVE, Set.of(CatalogItemScope.SALE), "EA", 3),
                TrackingMode.NONE, ExpiryPolicy.NONE);
        OneWarehouse warehouses = new OneWarehouse(warehouse);
        OneItem items = new OneItem(item);
        balances = new MemoryBalances();
        InventoryBalance balance = InventoryBalance.empty(COMPANY, key(), "EA");
        balance.receive(new BigDecimal("5"), 0);
        balances.values.put(key(), balance);
        counts = new MemoryCounts();
        movements = new MemoryMovements();
        CountingIds ids = new CountingIds();
        InventoryMovementService movementService = new InventoryMovementService(
                warehouses, items, balances, movements, counts,
                (companyId, request) -> Optional.of(new CatalogUnitConversionResult(
                        request.itemId(), request.sourceUnitCode(), request.targetUnitCode(),
                        request.quantity(), BigDecimal.ONE, request.quantity(), 3)),
                ids, event -> { }, CLOCK);
        service = new InventoryCountService(
                warehouses, items, balances, counts, movementService,
                ids, event -> { }, CLOCK);
    }

    @Test
    void closesACountWithOneImmutableDifferenceMovement() {
        StockCountId countId = service.draft(
                context(InventoryPermissions.COUNTS_MANAGE),
                new InventoryCommands.DraftCount(new StockCountScope(WAREHOUSE, Optional.empty())))
                .value().orElseThrow().id();
        service.addLine(context(InventoryPermissions.COUNTS_MANAGE),
                new InventoryCommands.AddCountLine(countId, 0, key()));
        service.start(context(InventoryPermissions.COUNTS_MANAGE),
                new InventoryCommands.CountTransition(countId, 1));
        service.record(context(InventoryPermissions.COUNTS_MANAGE),
                new InventoryCommands.RecordCount(countId, 2, key(), new BigDecimal("3")));
        service.review(context(InventoryPermissions.COUNTS_MANAGE),
                new InventoryCommands.CountTransition(countId, 3));

        var posted = service.post(context(InventoryPermissions.ADJUSTMENTS_POST),
                new InventoryCommands.CountTransition(countId, 4));
        var retry = service.post(context(InventoryPermissions.ADJUSTMENTS_POST),
                new InventoryCommands.CountTransition(countId, 4));

        assertTrue(posted.successful());
        assertEquals(posted.value(), retry.value());
        assertEquals("POSTED", posted.value().orElseThrow().state().name());
        assertEquals(0, new BigDecimal("3").compareTo(
                balances.values.get(key()).physicalQuantity()));
        assertEquals(1, movements.values.size());
    }

    @Test
    void refusesToStartWithAStaleTheoreticalQuantity() {
        StockCountId countId = service.draft(
                context(InventoryPermissions.COUNTS_MANAGE),
                new InventoryCommands.DraftCount(new StockCountScope(WAREHOUSE, Optional.empty())))
                .value().orElseThrow().id();
        service.addLine(context(InventoryPermissions.COUNTS_MANAGE),
                new InventoryCommands.AddCountLine(countId, 0, key()));
        InventoryBalance balance = balances.values.get(key());
        balance.receive(BigDecimal.ONE, balance.version());

        var start = service.start(context(InventoryPermissions.COUNTS_MANAGE),
                new InventoryCommands.CountTransition(countId, 1));

        assertEquals(InventoryResultCode.VERSION_CONFLICT, start.code());
        assertEquals("DRAFT", counts.value.state().name());
    }

    private static StockKey key() {
        return new StockKey(ITEM, WAREHOUSE, LOCATION, Optional.empty(), Optional.empty(),
                Optional.empty(), StockCondition.AVAILABLE);
    }

    private static InventoryOperationContext context(ContributionId permission) {
        return new InventoryOperationContext(
                new AuthenticatedCompanyContext(
                        new AuthenticatedActor(new AppUserId(uuid(99))), COMPANY),
                InventoryIdentity.PLUGIN_ID, permission, "request:count");
    }

    private static UUID uuid(long suffix) { return new UUID(0, suffix); }

    private record OneWarehouse(Warehouse value) implements WarehouseRepository {
        @Override public Optional<Warehouse> findById(CompanyId companyId, WarehouseId id) {
            return value.companyId().equals(companyId) && value.id().equals(id)
                    ? Optional.of(value) : Optional.empty();
        }
        @Override public Warehouse insert(Warehouse warehouse) { throw new UnsupportedOperationException(); }
        @Override public Warehouse update(Warehouse warehouse, long version) { throw new UnsupportedOperationException(); }
    }

    private record OneItem(InventoryItem value) implements InventoryItemRepository {
        @Override public Optional<InventoryItem> findById(CompanyId companyId, InventoryItemId id) {
            return value.companyId().equals(companyId) && value.id().equals(id)
                    ? Optional.of(value) : Optional.empty();
        }
        @Override public Optional<InventoryItem> findByCatalogItemId(CompanyId companyId, CatalogItemId id) {
            return value.catalogItemId().equals(id) ? Optional.of(value) : Optional.empty();
        }
        @Override public InventoryItem insert(InventoryItem item) { throw new UnsupportedOperationException(); }
        @Override public InventoryItem update(InventoryItem item, long version) { throw new UnsupportedOperationException(); }
    }

    private static final class MemoryBalances implements InventoryBalanceRepository {
        private final Map<StockKey, InventoryBalance> values = new HashMap<>();
        @Override public Optional<InventoryBalance> find(CompanyId companyId, StockKey key) {
            return Optional.ofNullable(values.get(key));
        }
        @Override public InventoryBalance insert(InventoryBalance balance) { values.put(balance.key(), balance); return balance; }
        @Override public InventoryBalance update(InventoryBalance balance, long version) { values.put(balance.key(), balance); return balance; }
        @Override public boolean hasQuantity(CompanyId companyId, WarehouseId id, Optional<StockLocationId> location) { return false; }
        @Override public boolean hasQuantity(CompanyId companyId, InventoryItemId id) { return false; }
    }

    private static final class MemoryCounts implements StockCountRepository {
        private StockCount value;
        @Override public Optional<StockCount> findById(CompanyId companyId, StockCountId id) {
            return value != null && value.companyId().equals(companyId) && value.id().equals(id)
                    ? Optional.of(value) : Optional.empty();
        }
        @Override public boolean blocks(CompanyId companyId, StockKey key) {
            return value != null && value.companyId().equals(companyId) && value.blocks(key);
        }
        @Override public StockCount insert(StockCount count) { value = count; return count; }
        @Override public StockCount update(StockCount count, long version) { value = count; return count; }
    }

    private static final class MemoryMovements implements StockMovementRepository {
        private final List<StockMovementSnapshot> values = new ArrayList<>();
        @Override public Optional<StockMovementSnapshot> findById(CompanyId companyId, StockMovementId id) {
            return values.stream().filter(value -> value.id().equals(id)).findFirst();
        }
        @Override public Optional<StockMovementSnapshot> findByIdempotencyKey(
                CompanyId companyId, String sourceType, String key) {
            return values.stream().filter(value -> value.companyId().equals(companyId)
                    && value.request().source().sourceType().equals(sourceType)
                    && value.request().idempotencyKey().equals(key)).findFirst();
        }
        @Override public StockMovementSnapshot append(StockMovementSnapshot value) { values.add(value); return value; }
    }

    private static final class CountingIds implements InventoryIdGenerator {
        @Override public WarehouseId nextWarehouseId() { return new WarehouseId(uuid(10)); }
        @Override public StockLocationId nextLocationId() { return new StockLocationId(uuid(11)); }
        @Override public InventoryItemId nextItemId() { return new InventoryItemId(uuid(12)); }
        @Override public StockMovementId nextMovementId() { return new StockMovementId(uuid(13)); }
        @Override public StockReservationId nextReservationId() { return new StockReservationId(uuid(14)); }
        @Override public StockCountId nextCountId() { return new StockCountId(uuid(15)); }
    }
}
