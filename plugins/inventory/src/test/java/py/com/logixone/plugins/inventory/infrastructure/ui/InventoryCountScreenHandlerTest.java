package py.com.logixone.plugins.inventory.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.inventory.InventoryScreenContract;
import py.com.logixone.plugins.inventory.api.ExpiryPolicy;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.TrackingMode;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.InventoryOperationResult;
import py.com.logixone.plugins.inventory.application.InventoryPermissions;
import py.com.logixone.plugins.inventory.application.InventoryUseCases;
import py.com.logixone.plugins.inventory.application.command.InventoryCommands;
import py.com.logixone.plugins.inventory.application.query.InventoryDirectoryQueries;
import py.com.logixone.plugins.inventory.domain.InventoryItemSnapshot;
import py.com.logixone.plugins.inventory.domain.StockCountLineSnapshot;
import py.com.logixone.plugins.inventory.domain.StockCountScope;
import py.com.logixone.plugins.inventory.domain.StockCountSnapshot;
import py.com.logixone.plugins.inventory.domain.StockCountState;
import py.com.logixone.plugins.inventory.domain.StockLocationSnapshot;
import py.com.logixone.plugins.inventory.domain.StockLocationType;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;

class InventoryCountScreenHandlerTest {
    private static final CompanyId COMPANY = new CompanyId(
            UUID.fromString("00000000-0000-0000-0000-000000000101"));
    private static final StockCountId COUNT_ID = StockCountId.parse(
            "00000000-0000-0000-0000-000000000201");
    private static final InventoryItemId ITEM_ID = InventoryItemId.parse(
            "00000000-0000-0000-0000-000000000301");
    private static final WarehouseId WAREHOUSE_ID = WarehouseId.parse(
            "00000000-0000-0000-0000-000000000401");
    private static final StockLocationId LOCATION_ID = StockLocationId.parse(
            "00000000-0000-0000-0000-000000000501");

    private RecordingAuthorization authorization;
    private RecordingUseCases recording;
    private InventoryCountScreenHandler handler;

    @BeforeEach
    void setUp() {
        authorization = new RecordingAuthorization();
        recording = new RecordingUseCases();
        handler = new InventoryCountScreenHandler();
        handler.authorization = authorization;
        handler.useCases = recording.proxy();
    }

    @Test
    void loadsCountDirectoryOptionsAndDetailUsingOnlyViewPermission() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.empty(), Map.of(), Optional.of(COUNT_ID.toString()), Optional.of(0L)));

        assertEquals(List.of(InventoryPermissions.VIEW.value()), authorization.permissions);
        assertEquals(1, result.table().orElseThrow().rows().size());
        assertTrue(result.detail().orElseThrow().title().startsWith("Conteo "));
        assertEquals(1, result.options().get(InventoryScreenContract.COUNT_CAPTURE_LINE).size());
        assertEquals(6, result.options().get(InventoryScreenContract.COUNT_SEARCH_STATE).size());
    }

    @Test
    void draftingUsesCountsPermissionThenRefreshesThroughView() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(InventoryScreenContract.DRAFT_COUNT),
                Map.of(
                        InventoryScreenContract.COUNT_NEW_WAREHOUSE, WAREHOUSE_ID.toString(),
                        InventoryScreenContract.COUNT_NEW_LOCATION, "NONE"),
                Optional.empty(), Optional.empty()));

        assertEquals(List.of(
                InventoryPermissions.COUNTS_MANAGE.value(),
                InventoryPermissions.VIEW.value()), authorization.permissions);
        assertTrue(recording.invocations.contains("draftCount"));
        assertTrue(result.notices().stream().anyMatch(
                notice -> notice.level() == ScreenInteraction.NoticeLevel.SUCCESS));
    }

    @Test
    void startingUsesCountsPermissionAndReturnsNewState() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(InventoryScreenContract.START_COUNT), Map.of(),
                Optional.of(COUNT_ID.toString()), Optional.of(0L)));

        assertEquals(List.of(
                InventoryPermissions.COUNTS_MANAGE.value(),
                InventoryPermissions.VIEW.value()), authorization.permissions);
        assertEquals(1L, result.selectedResourceVersion().orElseThrow());
        assertTrue(result.detail().orElseThrow().items().stream().anyMatch(
                item -> item.label().equals("Estado") && item.value().equals("En conteo")));
    }

    @Test
    void postingRequiresSpecificAdjustmentPermission() {
        recording.count = snapshot(StockCountState.REVIEW, 2);
        handler.interact(new ScreenInteraction.Request(
                Optional.of(InventoryScreenContract.POST_COUNT), Map.of(),
                Optional.of(COUNT_ID.toString()), Optional.of(2L)));

        assertEquals(List.of(
                InventoryPermissions.ADJUSTMENTS_POST.value(),
                InventoryPermissions.VIEW.value()), authorization.permissions);
        assertTrue(recording.invocations.contains("postCount"));
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
        private StockCountSnapshot count = snapshot(StockCountState.DRAFT, 0);

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
                case "searchCounts" -> InventoryOperationResult.success(
                        new InventoryDirectoryQueries.Page<>(
                                List.of(new InventoryDirectoryQueries.CountSummary(
                                        count.id(), count.scope(), count.state(),
                                        count.version(), count.lines().size())),
                                1, 0, 20));
                case "searchWarehouses" -> InventoryOperationResult.success(
                        new InventoryDirectoryQueries.Page<>(List.of(warehouse()), 1, 0, 100));
                case "searchItems" -> InventoryOperationResult.success(
                        new InventoryDirectoryQueries.Page<>(List.of(itemSummary()), 1, 0, 100));
                case "count" -> InventoryOperationResult.success(count);
                case "draftCount" -> draft((InventoryCommands.DraftCount) args[1]);
                case "startCount" -> transition(StockCountState.COUNTING);
                case "postCount" -> transition(StockCountState.POSTED);
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private InventoryOperationResult<StockCountSnapshot> draft(
                InventoryCommands.DraftCount command) {
            count = new StockCountSnapshot(
                    COMPANY,
                    StockCountId.parse("00000000-0000-0000-0000-000000000202"),
                    command.scope(), StockCountState.DRAFT, 0, List.of());
            return InventoryOperationResult.success(count);
        }

        private InventoryOperationResult<StockCountSnapshot> transition(StockCountState state) {
            count = new StockCountSnapshot(
                    count.companyId(), count.id(), count.scope(), state,
                    count.version() + 1, count.lines());
            return InventoryOperationResult.success(count);
        }
    }

    private static StockCountSnapshot snapshot(StockCountState state, long version) {
        StockKey key = new StockKey(
                ITEM_ID, WAREHOUSE_ID, LOCATION_ID,
                Optional.empty(), Optional.empty(), Optional.empty(), StockCondition.AVAILABLE);
        return new StockCountSnapshot(
                COMPANY, COUNT_ID, new StockCountScope(WAREHOUSE_ID, Optional.empty()),
                state, version,
                List.of(new StockCountLineSnapshot(
                        1, key, new BigDecimal("10"), Optional.of(new BigDecimal("9")))));
    }

    private static WarehouseSnapshot warehouse() {
        return new WarehouseSnapshot(
                COMPANY, WAREHOUSE_ID, "CENTRAL", "Depósito central", true, 0,
                List.of(new StockLocationSnapshot(
                        COMPANY, WAREHOUSE_ID, LOCATION_ID, "GENERAL", "General",
                        StockLocationType.GENERAL, true, 0)));
    }

    private static InventoryDirectoryQueries.ItemSummary itemSummary() {
        InventoryItemSnapshot item = new InventoryItemSnapshot(
                COMPANY, ITEM_ID,
                CatalogItemId.parse("00000000-0000-0000-0000-000000000701"),
                "ITEM-1", "Producto demo", "EA", 4,
                TrackingMode.NONE, ExpiryPolicy.NONE, true, 0);
        return new InventoryDirectoryQueries.ItemSummary(
                item, new BigDecimal("10"), BigDecimal.ZERO, 1);
    }
}
