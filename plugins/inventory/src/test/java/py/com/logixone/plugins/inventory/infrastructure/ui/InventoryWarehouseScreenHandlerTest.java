package py.com.logixone.plugins.inventory.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import py.com.logixone.plugins.inventory.InventoryScreenContract;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.InventoryOperationResult;
import py.com.logixone.plugins.inventory.application.InventoryPermissions;
import py.com.logixone.plugins.inventory.application.InventoryUseCases;
import py.com.logixone.plugins.inventory.application.command.InventoryCommands;
import py.com.logixone.plugins.inventory.application.query.InventoryDirectoryQueries;
import py.com.logixone.plugins.inventory.domain.StockLocationSnapshot;
import py.com.logixone.plugins.inventory.domain.StockLocationType;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;

class InventoryWarehouseScreenHandlerTest {
    private static final CompanyId COMPANY = new CompanyId(
            UUID.fromString("00000000-0000-0000-0000-000000000101"));
    private static final WarehouseId WAREHOUSE_ID = WarehouseId.parse(
            "00000000-0000-0000-0000-000000000201");
    private static final StockLocationId GENERAL_ID = StockLocationId.parse(
            "00000000-0000-0000-0000-000000000301");

    private RecordingAuthorization authorization;
    private RecordingUseCases recording;
    private InventoryWarehouseScreenHandler handler;

    @BeforeEach
    void setUp() {
        authorization = new RecordingAuthorization();
        recording = new RecordingUseCases();
        handler = new InventoryWarehouseScreenHandler();
        handler.authorization = authorization;
        handler.useCases = recording.proxy();
    }

    @Test
    void loadsCompanyScopedDirectoryAndDetailUsingOnlyViewPermission() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.empty(), Map.of(), Optional.of(WAREHOUSE_ID.toString()), Optional.of(0L)));

        assertEquals(List.of(InventoryPermissions.VIEW.value()), authorization.permissions);
        assertEquals(1, result.table().orElseThrow().rows().size());
        assertEquals("Depósito central", result.detail().orElseThrow().title());
        assertEquals(1, result.options().get(
                InventoryScreenContract.LOCATION_TO_RENAME).size());
    }

    @Test
    void openingWarehouseUsesStoragePermissionThenRefreshesThroughView() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(InventoryScreenContract.OPEN_WAREHOUSE),
                Map.of(
                        InventoryScreenContract.WAREHOUSE_NEW_CODE, "suc-2",
                        InventoryScreenContract.WAREHOUSE_NEW_NAME, "Sucursal 2"),
                Optional.empty(), Optional.empty()));

        assertEquals(List.of(
                InventoryPermissions.STORAGE_MANAGE.value(),
                InventoryPermissions.VIEW.value()), authorization.permissions);
        assertTrue(recording.invocations.contains("openWarehouse"));
        assertEquals("Sucursal 2", result.detail().orElseThrow().title());
        assertTrue(result.notices().stream().anyMatch(
                notice -> notice.level() == ScreenInteraction.NoticeLevel.SUCCESS));
    }

    @Test
    void inactivationUsesStoragePermissionAndReturnsNewVersion() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(InventoryScreenContract.INACTIVATE_WAREHOUSE),
                Map.of(), Optional.of(WAREHOUSE_ID.toString()), Optional.of(0L)));

        assertEquals(List.of(
                InventoryPermissions.STORAGE_MANAGE.value(),
                InventoryPermissions.VIEW.value()), authorization.permissions);
        assertEquals(1L, result.selectedResourceVersion().orElseThrow());
        assertTrue(result.detail().orElseThrow().items().stream().anyMatch(
                item -> item.label().equals("Estado") && item.value().equals("Inactivo")));
    }

    @Test
    void invalidOpenIsRejectedBeforeStorageAuthorization() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(InventoryScreenContract.OPEN_WAREHOUSE),
                Map.of(InventoryScreenContract.WAREHOUSE_NEW_CODE, "SUC-2"),
                Optional.empty(), Optional.empty()));

        assertEquals(List.of(InventoryPermissions.VIEW.value()), authorization.permissions);
        assertEquals(ScreenInteraction.NoticeLevel.ERROR, result.notices().getFirst().level());
        assertTrue(recording.invocations.stream().noneMatch("openWarehouse"::equals));
    }

    private static final class RecordingAuthorization implements CurrentCompanyAuthorization {
        private final List<String> permissions = new ArrayList<>();

        @Override
        public AuthorizedCompanyOperation require(String pluginId, String permissionId) {
            permissions.add(permissionId);
            return new AuthorizedCompanyOperation(
                    new AuthenticatedCompanyContext(
                            new AuthenticatedActor(new AppUserId(UUID.fromString(
                                    "00000000-0000-0000-0000-000000000501"))),
                            COMPANY),
                    pluginId,
                    permissionId,
                    "ui:test");
        }
    }

    private static final class RecordingUseCases implements InvocationHandler {
        private final List<String> invocations = new ArrayList<>();
        private WarehouseSnapshot warehouse = snapshot(
                WAREHOUSE_ID, "CENTRAL", "Depósito central", true, 0);

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
                case "searchWarehouses" -> InventoryOperationResult.success(
                        new InventoryDirectoryQueries.Page<>(List.of(warehouse), 1, 0, 20));
                case "warehouse" -> InventoryOperationResult.success(warehouse);
                case "openWarehouse" -> open((InventoryCommands.OpenWarehouse) args[1]);
                case "inactivateWarehouse" -> inactivate();
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private InventoryOperationResult<WarehouseSnapshot> open(
                InventoryCommands.OpenWarehouse command) {
            warehouse = snapshot(
                    WarehouseId.parse("00000000-0000-0000-0000-000000000202"),
                    command.code(), command.name(), true, 0);
            return InventoryOperationResult.success(warehouse);
        }

        private InventoryOperationResult<WarehouseSnapshot> inactivate() {
            warehouse = new WarehouseSnapshot(
                    warehouse.companyId(), warehouse.id(), warehouse.code(), warehouse.name(),
                    false, warehouse.version() + 1, warehouse.locations());
            return InventoryOperationResult.success(warehouse);
        }
    }

    private static WarehouseSnapshot snapshot(
            WarehouseId id, String code, String name, boolean active, long version) {
        StockLocationSnapshot general = new StockLocationSnapshot(
                COMPANY, id, GENERAL_ID, "GENERAL", "General",
                StockLocationType.GENERAL, active, 0);
        return new WarehouseSnapshot(COMPANY, id, code, name, active, version, List.of(general));
    }
}
