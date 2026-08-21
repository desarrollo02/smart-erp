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
import py.com.logixone.plugins.inventory.api.ExpiryPolicy;
import py.com.logixone.plugins.inventory.api.CatalogStockReservationRequest;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.StockReservationRequest;
import py.com.logixone.plugins.inventory.api.StockSourceReference;
import py.com.logixone.plugins.inventory.api.TrackingMode;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.command.InventoryCommands;
import py.com.logixone.plugins.inventory.application.port.InventoryBalanceRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryIdGenerator;
import py.com.logixone.plugins.inventory.application.port.InventoryItemRepository;
import py.com.logixone.plugins.inventory.application.port.ReservationOperationRepository;
import py.com.logixone.plugins.inventory.application.port.StockCountRepository;
import py.com.logixone.plugins.inventory.application.port.StockMovementRepository;
import py.com.logixone.plugins.inventory.application.port.StockReservationRepository;
import py.com.logixone.plugins.inventory.application.port.WarehouseRepository;
import py.com.logixone.plugins.inventory.domain.InventoryBalance;
import py.com.logixone.plugins.inventory.domain.InventoryItem;
import py.com.logixone.plugins.inventory.domain.ReservationOperation;
import py.com.logixone.plugins.inventory.domain.StockCount;
import py.com.logixone.plugins.inventory.domain.StockMovementSnapshot;
import py.com.logixone.plugins.inventory.domain.StockReservation;
import py.com.logixone.plugins.inventory.domain.Warehouse;

class InventoryReservationServiceTest {
    private static final CompanyId COMPANY = new CompanyId(uuid(1));
    private static final InventoryItemId ITEM = new InventoryItemId(uuid(2));
    private static final WarehouseId WAREHOUSE = new WarehouseId(uuid(3));
    private static final StockLocationId LOCATION = new StockLocationId(uuid(4));
    private static final CatalogItemId CATALOG_ITEM = new CatalogItemId(uuid(5));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-31T15:00:00Z"), ZoneOffset.UTC);

    private MemoryBalances balances;
    private MemoryReservations reservations;
    private MemoryOperations operations;
    private MemoryMovements movements;
    private CountingIds ids;
    private List<TechnicalAuditEvent> audit;
    private InventoryReservationService service;

    @BeforeEach
    void setUp() {
        Warehouse warehouse = Warehouse.open(COMPANY, WAREHOUSE, LOCATION, "MAIN", "Central");
        InventoryItem item = InventoryItem.enroll(COMPANY, ITEM, new CatalogItemReference(
                CATALOG_ITEM, "SKU-1", "Private product", CatalogItemType.PRODUCT,
                CatalogItemState.ACTIVE, Set.of(CatalogItemScope.SALE), "EA", 3),
                TrackingMode.NONE, ExpiryPolicy.NONE);
        InventoryBalance balance = InventoryBalance.empty(COMPANY, key(), "EA");
        balance.receive(new BigDecimal("10"), 0);
        balances = new MemoryBalances();
        balances.value = balance;
        reservations = new MemoryReservations();
        operations = new MemoryOperations();
        movements = new MemoryMovements();
        ids = new CountingIds();
        audit = new ArrayList<>();
        service = new InventoryReservationService(
                new OneWarehouse(warehouse), new OneItem(item), balances,
                reservations, operations, movements, new UnlockedCounts(),
                ids, audit::add, CLOCK);
    }

    @Test
    void reservesConsumesAndReturnsTheCapturedResultOnRetry() {
        var reserved = service.reserve(context(InventoryPermissions.RESERVATIONS_MANAGE),
                request("reserve-1", "4"));
        StockReservationId reservationId = reserved.value().orElseThrow().id();

        var consumed = service.consume(context(InventoryPermissions.RESERVATIONS_MANAGE),
                new InventoryCommands.ConsumeReservation(
                        reservationId, 0, new BigDecimal("3"), "consume-1"));
        var retry = service.consume(context(InventoryPermissions.RESERVATIONS_MANAGE),
                new InventoryCommands.ConsumeReservation(
                        reservationId, 0, new BigDecimal("3"), "consume-1"));

        assertTrue(reserved.successful());
        assertTrue(consumed.successful());
        assertEquals(consumed.value(), retry.value());
        assertEquals(0, new BigDecimal("7").compareTo(balances.value.physicalQuantity()));
        assertEquals(0, BigDecimal.ONE.compareTo(balances.value.reservedQuantity()));
        assertEquals(1, movements.values.size());
        assertEquals(1, operations.values.size());
        assertEquals("UNCHANGED", audit.getLast().outcome().name());
    }

    @Test
    void rejectsAReusedReservationOperationKeyWithDifferentQuantity() {
        StockReservationId reservationId = service.reserve(
                context(InventoryPermissions.RESERVATIONS_MANAGE), request("reserve-2", "4"))
                .value().orElseThrow().id();
        service.release(context(InventoryPermissions.RESERVATIONS_MANAGE),
                new InventoryCommands.ReleaseReservation(
                        reservationId, 0, BigDecimal.ONE, "release-1"));

        var conflict = service.release(context(InventoryPermissions.RESERVATIONS_MANAGE),
                new InventoryCommands.ReleaseReservation(
                        reservationId, 0, new BigDecimal("2"), "release-1"));

        assertEquals(InventoryResultCode.IDEMPOTENCY_CONFLICT, conflict.code());
        assertEquals(1, operations.values.size());
    }

    @Test
    void deniesBeforeAnyReservationLookup() {
        var result = service.reserve(context(InventoryPermissions.VIEW), request("reserve-3", "1"));

        assertEquals(InventoryResultCode.ACCESS_DENIED, result.code());
        assertEquals(0, reservations.lookups);
        assertEquals(0, ids.reservationCalls);
    }

    @Test
    void resolvesCatalogIdentityInsideInventoryBeforeReserving() {
        var request = new CatalogStockReservationRequest(
                CATALOG_ITEM.value(), WAREHOUSE, LOCATION, Optional.empty(), Optional.empty(),
                Optional.empty(), StockCondition.AVAILABLE, new BigDecimal("2"),
                new StockSourceReference("SALES_ORDER", "order-1"),
                Instant.parse("2026-08-01T15:00:00Z"), "sales-order-1-line-1");

        var result = service.reserveCatalogItem(
                context(InventoryPermissions.RESERVATIONS_MANAGE), request);

        assertTrue(result.successful());
        assertEquals(ITEM, result.value().orElseThrow().key().inventoryItemId());
        assertEquals(0, new BigDecimal("2").compareTo(balances.value.reservedQuantity()));
    }

    private static StockReservationRequest request(String key, String quantity) {
        return new StockReservationRequest(
                key(), new BigDecimal(quantity), new StockSourceReference("ORDER", "order-1"),
                Instant.parse("2026-08-01T15:00:00Z"), key);
    }

    private static StockKey key() {
        return new StockKey(ITEM, WAREHOUSE, LOCATION, Optional.empty(), Optional.empty(),
                Optional.empty(), StockCondition.AVAILABLE);
    }

    private static InventoryOperationContext context(ContributionId permission) {
        return new InventoryOperationContext(
                new AuthenticatedCompanyContext(
                        new AuthenticatedActor(new AppUserId(uuid(99))), COMPANY),
                InventoryIdentity.PLUGIN_ID, permission, "request:reservation");
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
            return value.companyId().equals(companyId) && value.catalogItemId().equals(id)
                    ? Optional.of(value) : Optional.empty();
        }
        @Override public InventoryItem insert(InventoryItem item) { throw new UnsupportedOperationException(); }
        @Override public InventoryItem update(InventoryItem item, long version) { throw new UnsupportedOperationException(); }
    }

    private static final class MemoryBalances implements InventoryBalanceRepository {
        private InventoryBalance value;
        @Override public Optional<InventoryBalance> find(CompanyId companyId, StockKey key) {
            return value != null && value.companyId().equals(companyId) && value.key().equals(key)
                    ? Optional.of(value) : Optional.empty();
        }
        @Override public InventoryBalance insert(InventoryBalance balance) { value = balance; return balance; }
        @Override public InventoryBalance update(InventoryBalance balance, long version) { value = balance; return balance; }
        @Override public boolean hasQuantity(CompanyId companyId, WarehouseId id, Optional<StockLocationId> location) { return false; }
        @Override public boolean hasQuantity(CompanyId companyId, InventoryItemId id) { return false; }
    }

    private static final class MemoryReservations implements StockReservationRepository {
        private final Map<StockReservationId, StockReservation> values = new HashMap<>();
        private int lookups;
        @Override public Optional<StockReservation> findById(CompanyId companyId, StockReservationId id) {
            return Optional.ofNullable(values.get(id)).filter(value -> value.companyId().equals(companyId));
        }
        @Override public Optional<StockReservation> findByIdempotencyKey(
                CompanyId companyId, String sourceType, String key) {
            lookups++;
            return values.values().stream().filter(value -> value.companyId().equals(companyId)
                    && value.snapshot().request().source().sourceType().equals(sourceType)
                    && value.snapshot().request().idempotencyKey().equals(key)).findFirst();
        }
        @Override public StockReservation insert(StockReservation value) { values.put(value.id(), value); return value; }
        @Override public StockReservation update(StockReservation value, long version) { values.put(value.id(), value); return value; }
    }

    private static final class MemoryOperations implements ReservationOperationRepository {
        private final Map<String, ReservationOperation> values = new HashMap<>();
        @Override public Optional<ReservationOperation> findByIdempotencyKey(CompanyId companyId, String key) {
            return Optional.ofNullable(values.get(ReservationOperation.canonicalIdempotencyKey(key)))
                    .filter(value -> value.companyId().equals(companyId));
        }
        @Override public ReservationOperation append(ReservationOperation value) {
            values.put(value.idempotencyKey(), value); return value;
        }
    }

    private static final class MemoryMovements implements StockMovementRepository {
        private final List<StockMovementSnapshot> values = new ArrayList<>();
        @Override public Optional<StockMovementSnapshot> findById(CompanyId companyId, StockMovementId id) {
            return values.stream().filter(value -> value.companyId().equals(companyId)
                    && value.id().equals(id)).findFirst();
        }
        @Override public Optional<StockMovementSnapshot> findByIdempotencyKey(
                CompanyId companyId, String sourceType, String key) {
            return values.stream().filter(value -> value.companyId().equals(companyId)
                    && value.request().source().sourceType().equals(sourceType)
                    && value.request().idempotencyKey().equals(key)).findFirst();
        }
        @Override public StockMovementSnapshot append(StockMovementSnapshot value) { values.add(value); return value; }
    }

    private static final class UnlockedCounts implements StockCountRepository {
        @Override public Optional<StockCount> findById(CompanyId companyId, StockCountId id) { return Optional.empty(); }
        @Override public boolean blocks(CompanyId companyId, StockKey key) { return false; }
        @Override public StockCount insert(StockCount count) { return count; }
        @Override public StockCount update(StockCount count, long version) { return count; }
    }

    private static final class CountingIds implements InventoryIdGenerator {
        private int reservationCalls;
        @Override public WarehouseId nextWarehouseId() { return new WarehouseId(uuid(10)); }
        @Override public StockLocationId nextLocationId() { return new StockLocationId(uuid(11)); }
        @Override public InventoryItemId nextItemId() { return new InventoryItemId(uuid(12)); }
        @Override public StockMovementId nextMovementId() { return new StockMovementId(uuid(13)); }
        @Override public StockReservationId nextReservationId() { reservationCalls++; return new StockReservationId(uuid(14)); }
        @Override public StockCountId nextCountId() { return new StockCountId(uuid(15)); }
    }
}
