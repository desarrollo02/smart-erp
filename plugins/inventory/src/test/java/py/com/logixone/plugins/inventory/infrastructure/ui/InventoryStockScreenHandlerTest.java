package py.com.logixone.plugins.inventory.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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
import py.com.logixone.kernel.api.security.AuthorizedCompanyOperation;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemReference;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchPage;
import py.com.logixone.plugins.inventory.InventoryScreenContract;
import py.com.logixone.plugins.inventory.api.ExpiryPolicy;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockAvailability;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockMovementReference;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;
import py.com.logixone.plugins.inventory.api.TrackingMode;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.InventoryOperationResult;
import py.com.logixone.plugins.inventory.application.InventoryPermissions;
import py.com.logixone.plugins.inventory.application.InventoryUseCases;
import py.com.logixone.plugins.inventory.application.command.InventoryCommands;
import py.com.logixone.plugins.inventory.application.query.InventoryDirectoryQueries;
import py.com.logixone.plugins.inventory.domain.InventoryItemSnapshot;
import py.com.logixone.plugins.inventory.domain.StockLocationSnapshot;
import py.com.logixone.plugins.inventory.domain.StockLocationType;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;

class InventoryStockScreenHandlerTest {
    private static final CompanyId COMPANY = new CompanyId(
            UUID.fromString("00000000-0000-0000-0000-000000000101"));
    private static final InventoryItemId ITEM_ID = InventoryItemId.parse(
            "00000000-0000-0000-0000-000000000201");
    private static final CatalogItemId CATALOG_ID = CatalogItemId.parse(
            "00000000-0000-0000-0000-000000000301");
    private static final WarehouseId WAREHOUSE_ID = WarehouseId.parse(
            "00000000-0000-0000-0000-000000000401");
    private static final StockLocationId LOCATION_ID = StockLocationId.parse(
            "00000000-0000-0000-0000-000000000501");

    private RecordingAuthorization authorization;
    private RecordingUseCases recording;
    private InventoryStockScreenHandler handler;

    @BeforeEach
    void setUp() {
        authorization = new RecordingAuthorization();
        recording = new RecordingUseCases();
        handler = new InventoryStockScreenHandler();
        handler.authorization = authorization;
        handler.useCases = recording.proxy();
        handler.catalog = new DemoCatalog();
    }

    @Test
    void loadsStockDirectoryOptionsAndDetailUsingOnlyViewPermission() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.empty(), Map.of(), Optional.of(ITEM_ID.toString()), Optional.of(0L)));

        assertEquals(List.of(InventoryPermissions.VIEW.value()), authorization.permissions);
        assertEquals(1, result.table().orElseThrow().rows().size());
        assertEquals("Producto demo", result.detail().orElseThrow().title());
        assertEquals(CATALOG_ID.toString(), result.inputs().get(
                InventoryScreenContract.STOCK_NEW_CATALOG_ITEM));
        assertEquals(3, result.options().get(InventoryScreenContract.MOVEMENT_TYPE).size());
    }

    @Test
    void receiptUsesViewToFreezeBaseUnitThenMovementPermissionAndRefreshes() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(InventoryScreenContract.POST_MOVEMENT),
                Map.of(
                        InventoryScreenContract.MOVEMENT_TYPE, "RECEIPT",
                        InventoryScreenContract.MOVEMENT_WAREHOUSE, WAREHOUSE_ID.toString(),
                        InventoryScreenContract.MOVEMENT_LOCATION, LOCATION_ID.toString(),
                        InventoryScreenContract.MOVEMENT_QUANTITY, "5",
                        InventoryScreenContract.MOVEMENT_REASON, "INITIAL_RECEIPT",
                        InventoryScreenContract.MOVEMENT_SOURCE_ID, "DEMO-1",
                        InventoryScreenContract.MOVEMENT_IDEMPOTENCY, "demo-receipt-1"),
                Optional.of(ITEM_ID.toString()), Optional.of(0L)));

        assertEquals(List.of(
                InventoryPermissions.VIEW.value(),
                InventoryPermissions.MOVEMENTS_POST.value(),
                InventoryPermissions.VIEW.value()), authorization.permissions);
        assertEquals("EA", recording.lastMovement.lines().getFirst().quantity().baseUnitCode());
        assertEquals(new BigDecimal("5"),
                recording.lastMovement.lines().getFirst().quantity().baseQuantity());
        assertTrue(result.notices().stream().anyMatch(
                notice -> notice.level() == ScreenInteraction.NoticeLevel.SUCCESS));
    }

    @Test
    void availabilityUsesViewPermissionAndReturnsQuantitiesAsNotice() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(InventoryScreenContract.CHECK_AVAILABILITY),
                Map.of(
                        InventoryScreenContract.AVAILABILITY_WAREHOUSE, WAREHOUSE_ID.toString(),
                        InventoryScreenContract.AVAILABILITY_LOCATION, LOCATION_ID.toString()),
                Optional.of(ITEM_ID.toString()), Optional.of(0L)));

        assertEquals(List.of(
                InventoryPermissions.VIEW.value(),
                InventoryPermissions.VIEW.value()), authorization.permissions);
        assertTrue(result.notices().stream().anyMatch(notice ->
                notice.summary().equals("Disponibilidad consultada")
                        && notice.detail().contains("Disponible: 8 EA")));
    }

    @Test
    void invalidEnrollmentIsRejectedBeforeItemsAuthorization() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(InventoryScreenContract.ENROLL_STOCK_ITEM),
                Map.of(), Optional.empty(), Optional.empty()));

        assertEquals(List.of(InventoryPermissions.VIEW.value()), authorization.permissions);
        assertEquals(ScreenInteraction.NoticeLevel.ERROR, result.notices().getFirst().level());
        assertTrue(recording.invocations.stream().noneMatch("enrollItem"::equals));
    }

    private static final class RecordingAuthorization implements CurrentCompanyAuthorization {
        private final List<String> permissions = new ArrayList<>();

        @Override
        public AuthorizedCompanyOperation require(String pluginId, String permissionId) {
            permissions.add(permissionId);
            return new AuthorizedCompanyOperation(
                    new AuthenticatedCompanyContext(
                            new AuthenticatedActor(new AppUserId(UUID.fromString(
                                    "00000000-0000-0000-0000-000000000601"))),
                            COMPANY),
                    pluginId,
                    permissionId,
                    "ui:test");
        }
    }

    private static final class RecordingUseCases implements InvocationHandler {
        private final List<String> invocations = new ArrayList<>();
        private InventoryItemSnapshot item = item(ITEM_ID, CATALOG_ID, 0);
        private StockMovementRequest lastMovement;

        InventoryUseCases proxy() {
            return (InventoryUseCases) Proxy.newProxyInstance(
                    InventoryUseCases.class.getClassLoader(),
                    new Class<?>[]{InventoryUseCases.class},
                    this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            invocations.add(method.getName());
            return switch (method.getName()) {
                case "searchItems" -> InventoryOperationResult.success(
                        new InventoryDirectoryQueries.Page<>(List.of(summary()), 1, 0, 20));
                case "itemSummary" -> InventoryOperationResult.success(summary());
                case "item" -> InventoryOperationResult.success(item);
                case "searchWarehouses" -> InventoryOperationResult.success(
                        new InventoryDirectoryQueries.Page<>(List.of(warehouse()), 1, 0, 100));
                case "availability" -> availability((StockKey) args[1]);
                case "postMovement" -> post((StockMovementRequest) args[1]);
                case "enrollItem" -> enroll((InventoryCommands.EnrollItem) args[1]);
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private InventoryDirectoryQueries.ItemSummary summary() {
            return new InventoryDirectoryQueries.ItemSummary(
                    item, new BigDecimal("10"), new BigDecimal("2"), 1);
        }

        private InventoryOperationResult<StockAvailability> availability(StockKey key) {
            return InventoryOperationResult.success(new StockAvailability(
                    key, "EA", new BigDecimal("10"), new BigDecimal("2"),
                    new BigDecimal("8"), 0));
        }

        private InventoryOperationResult<StockMovementReference> post(StockMovementRequest request) {
            lastMovement = request;
            return InventoryOperationResult.success(new StockMovementReference(
                    StockMovementId.parse("00000000-0000-0000-0000-000000000701"),
                    request.type(), Instant.parse("2026-07-31T12:00:00Z"),
                    request.lines(), Optional.empty()));
        }

        private InventoryOperationResult<InventoryItemSnapshot> enroll(
                InventoryCommands.EnrollItem command) {
            item = item(
                    InventoryItemId.parse("00000000-0000-0000-0000-000000000202"),
                    command.catalogItemId(), 0);
            return InventoryOperationResult.success(item);
        }
    }

    private static final class DemoCatalog implements CatalogItemDirectory {
        @Override
        public Optional<CatalogItemReference> findById(CompanyId companyId, CatalogItemId itemId) {
            return COMPANY.equals(companyId) && CATALOG_ID.equals(itemId)
                    ? Optional.of(reference()) : Optional.empty();
        }

        @Override
        public CatalogSearchPage search(CompanyId companyId, CatalogSearchCriteria criteria) {
            assertEquals(COMPANY, companyId);
            return new CatalogSearchPage(List.of(reference()), 1, criteria.offset(), criteria.limit());
        }
    }

    private static InventoryItemSnapshot item(
            InventoryItemId id, CatalogItemId catalogId, long version) {
        return new InventoryItemSnapshot(
                COMPANY, id, catalogId, "ITEM-1", "Producto demo", "EA", 4,
                TrackingMode.NONE, ExpiryPolicy.NONE, true, version);
    }

    private static WarehouseSnapshot warehouse() {
        return new WarehouseSnapshot(
                COMPANY, WAREHOUSE_ID, "CENTRAL", "Depósito central", true, 0,
                List.of(new StockLocationSnapshot(
                        COMPANY, WAREHOUSE_ID, LOCATION_ID, "GENERAL", "General",
                        StockLocationType.GENERAL, true, 0)));
    }

    private static CatalogItemReference reference() {
        return new CatalogItemReference(
                CATALOG_ID, "ITEM-1", "Producto demo", CatalogItemType.PRODUCT,
                CatalogItemState.ACTIVE,
                Set.of(CatalogItemScope.PURCHASE, CatalogItemScope.SALE),
                "EA", 4);
    }
}
