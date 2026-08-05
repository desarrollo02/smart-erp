package py.com.logixone.plugins.inventory.infrastructure.ui;

import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.ALL;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.PAGE_SIZE;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.clear;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.context;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.enumValue;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.error;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.filter;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.first;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.option;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.required;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugin.api.SelectorSourceDefinition;
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

/** Neutral interaction adapter for warehouses and their owned locations. */
@ApplicationScoped
public class InventoryWarehouseScreenHandler implements ScreenInteraction.Handler {
    private static final Logger LOGGER = System.getLogger(
            InventoryWarehouseScreenHandler.class.getName());

    @Inject
    InventoryUseCases useCases;

    @Inject
    CurrentCompanyAuthorization authorization;

    @Override
    public ScreenId screenId() {
        return InventoryScreenContract.WAREHOUSES;
    }

    @Override
    public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return InventorySelectorSources.WAREHOUSES;
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = defaults(request.inputs());
        Optional<String> selectedId = request.selectedResourceId();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();
        try {
            if (request.actionId().isPresent()) {
                ScreenElementId action = request.actionId().orElseThrow();
                if (action.equals(InventoryScreenContract.WAREHOUSE_SEARCH)) {
                    selectedId = Optional.empty();
                } else if (!action.equals(InventoryScreenContract.SELECT_WAREHOUSE)) {
                    InventoryScreenSupport.Mutation mutation = execute(action, request, inputs);
                    selectedId = mutation.selectedResourceId();
                    notices.addAll(mutation.notices());
                    if (mutation.successful()) {
                        clearMutationInputs(action, inputs);
                    }
                }
            }
        } catch (IllegalArgumentException invalidInput) {
            LOGGER.log(Level.WARNING,
                    "event=inventory_warehouse_screen_input_rejected action={0} input_keys={1}",
                    request.actionId().map(ScreenElementId::value).orElse("none"),
                    inputs.keySet().stream().map(ScreenElementId::value).sorted().toList());
            notices.add(error(
                    "Revisa los datos ingresados",
                    "Uno o más valores no cumplen el formato permitido."));
        }
        return load(inputs, selectedId, notices);
    }

    private InventoryScreenSupport.Mutation execute(
            ScreenElementId action,
            ScreenInteraction.Request request,
            Map<ScreenElementId, String> inputs) {
        if (action.equals(InventoryScreenContract.OPEN_WAREHOUSE)) {
            var command = new InventoryCommands.OpenWarehouse(
                    required(inputs, InventoryScreenContract.WAREHOUSE_NEW_CODE),
                    required(inputs, InventoryScreenContract.WAREHOUSE_NEW_NAME));
            return InventoryScreenSupport.mutation(
                    useCases.openWarehouse(context(authorization, InventoryPermissions.STORAGE_MANAGE), command),
                    "Depósito creado", snapshot -> snapshot.id().toString(), Optional.empty());
        }

        WarehouseId warehouseId = WarehouseId.parse(request.selectedResourceId().orElseThrow(
                () -> new IllegalArgumentException("A selected warehouse is required")));
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("A selected warehouse version is required"));
        Optional<String> fallback = Optional.of(warehouseId.toString());

        if (action.equals(InventoryScreenContract.RENAME_WAREHOUSE)) {
            var command = new InventoryCommands.RenameWarehouse(
                    warehouseId, version,
                    required(inputs, InventoryScreenContract.WAREHOUSE_EDIT_NAME));
            return InventoryScreenSupport.mutation(
                    useCases.renameWarehouse(context(authorization, InventoryPermissions.STORAGE_MANAGE), command),
                    "Nombre del depósito actualizado", snapshot -> snapshot.id().toString(), fallback);
        }
        if (action.equals(InventoryScreenContract.ADD_LOCATION)) {
            var command = new InventoryCommands.AddLocation(
                    warehouseId,
                    version,
                    required(inputs, InventoryScreenContract.LOCATION_NEW_CODE),
                    required(inputs, InventoryScreenContract.LOCATION_NEW_NAME),
                    enumValue(inputs, InventoryScreenContract.LOCATION_NEW_TYPE, StockLocationType.class));
            return InventoryScreenSupport.mutation(
                    useCases.addLocation(context(authorization, InventoryPermissions.STORAGE_MANAGE), command),
                    "Ubicación agregada", snapshot -> snapshot.id().toString(), fallback);
        }
        if (action.equals(InventoryScreenContract.RENAME_LOCATION)) {
            StockLocationSnapshot location = selectedLocation(
                    warehouseId, inputs, InventoryScreenContract.LOCATION_TO_RENAME);
            var command = new InventoryCommands.RenameLocation(
                    warehouseId, location.id(), version, location.version(),
                    required(inputs, InventoryScreenContract.LOCATION_EDIT_NAME));
            return InventoryScreenSupport.mutation(
                    useCases.renameLocation(context(authorization, InventoryPermissions.STORAGE_MANAGE), command),
                    "Ubicación renombrada", snapshot -> snapshot.id().toString(), fallback);
        }
        if (action.equals(InventoryScreenContract.INACTIVATE_LOCATION)) {
            StockLocationSnapshot location = selectedLocation(
                    warehouseId, inputs, InventoryScreenContract.LOCATION_TO_INACTIVATE);
            var command = new InventoryCommands.InactivateLocation(
                    warehouseId, location.id(), version, location.version());
            return InventoryScreenSupport.mutation(
                    useCases.inactivateLocation(context(authorization, InventoryPermissions.STORAGE_MANAGE), command),
                    "Ubicación inactivada", snapshot -> snapshot.id().toString(), fallback);
        }
        if (action.equals(InventoryScreenContract.INACTIVATE_WAREHOUSE)) {
            var command = new InventoryCommands.InactivateWarehouse(warehouseId, version);
            return InventoryScreenSupport.mutation(
                    useCases.inactivateWarehouse(context(authorization, InventoryPermissions.STORAGE_MANAGE), command),
                    "Depósito inactivado", snapshot -> snapshot.id().toString(), fallback);
        }
        throw new IllegalArgumentException("Unsupported inventory warehouse screen action");
    }

    private StockLocationSnapshot selectedLocation(
            WarehouseId warehouseId,
            Map<ScreenElementId, String> inputs,
            ScreenElementId field) {
        StockLocationId locationId = StockLocationId.parse(required(inputs, field));
        InventoryOperationResult<WarehouseSnapshot> found = useCases.warehouse(
                context(authorization, InventoryPermissions.VIEW), warehouseId);
        if (!found.successful()) {
            throw new IllegalArgumentException("Warehouse is no longer available");
        }
        return found.value().orElseThrow().locations().stream()
                .filter(location -> location.id().equals(locationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Location does not belong to warehouse"));
    }

    private ScreenInteraction.Result load(
            Map<ScreenElementId, String> inputs,
            Optional<String> selectedId,
            List<ScreenInteraction.Notice> notices) {
        String stage = "authorization";
        try {
            var viewContext = context(authorization, InventoryPermissions.VIEW);
            stage = "search";
            var criteria = new InventoryDirectoryQueries.Criteria(
                    filter(inputs, InventoryScreenContract.WAREHOUSE_SEARCH_TEXT),
                    activeFilter(inputs, InventoryScreenContract.WAREHOUSE_SEARCH_STATE),
                    0,
                    PAGE_SIZE);
            var search = useCases.searchWarehouses(viewContext, criteria);
            if (!search.successful()) {
                throw new IllegalStateException("Authorized warehouse search failed");
            }

            Optional<WarehouseSnapshot> selected = Optional.empty();
            Optional<ScreenInteraction.Detail> detail = Optional.empty();
            Optional<Long> selectedVersion = Optional.empty();
            if (selectedId.isPresent()) {
                WarehouseId id = WarehouseId.parse(selectedId.orElseThrow());
                var found = useCases.warehouse(viewContext, id);
                if (found.successful()) {
                    selected = found.value();
                    WarehouseSnapshot snapshot = selected.orElseThrow();
                    inputs.put(InventoryScreenContract.WAREHOUSE_EDIT_NAME, snapshot.name());
                    detail = Optional.of(detail(snapshot));
                    selectedVersion = Optional.of(snapshot.version());
                } else {
                    selectedId = Optional.empty();
                    notices.add(error(
                            "Depósito no disponible",
                            "El depósito ya no existe o no pertenece a la empresa activa."));
                }
            }

            Map<ScreenElementId, List<ScreenInteraction.Option>> options = options(selected);
            applyOptionDefaults(inputs, options);
            return new ScreenInteraction.Result(
                    inputs,
                    options,
                    Optional.of(table(search.value().orElseThrow())),
                    detail,
                    notices,
                    selectedId,
                    selectedVersion);
        } catch (RuntimeException failure) {
            LOGGER.log(Level.ERROR,
                    "event=inventory_warehouse_screen_load_failed stage={0} type={1}",
                    stage, failure.getClass().getName());
            throw failure;
        }
    }

    private static ScreenInteraction.Table table(
            InventoryDirectoryQueries.Page<WarehouseSnapshot> page) {
        return new ScreenInteraction.Table(
                InventoryScreenContract.WAREHOUSE_RESULTS,
                List.of(
                        new ScreenInteraction.Column("code", "Código"),
                        new ScreenInteraction.Column("name", "Nombre"),
                        new ScreenInteraction.Column("locations", "Ubicaciones"),
                        new ScreenInteraction.Column("state", "Estado")),
                page.items().stream().map(snapshot -> new ScreenInteraction.Row(
                        snapshot.id().toString(),
                        List.of(
                                snapshot.code(),
                                snapshot.name(),
                                Integer.toString(snapshot.locations().size()),
                                stateLabel(snapshot.active())))).toList(),
                page.total(),
                "No encontramos depósitos",
                "Ajusta los filtros o crea el primer depósito de esta empresa.");
    }

    private static ScreenInteraction.Detail detail(WarehouseSnapshot snapshot) {
        List<ScreenInteraction.DetailItem> items = new ArrayList<>();
        items.add(new ScreenInteraction.DetailItem("Código", snapshot.code()));
        items.add(new ScreenInteraction.DetailItem("Estado", stateLabel(snapshot.active())));
        items.add(new ScreenInteraction.DetailItem(
                "Ubicaciones activas",
                Long.toString(snapshot.locations().stream().filter(StockLocationSnapshot::active).count())));
        items.add(new ScreenInteraction.DetailItem(
                "Ubicaciones totales", Integer.toString(snapshot.locations().size())));
        int index = 1;
        for (StockLocationSnapshot location : snapshot.locations().stream().limit(20).toList()) {
            items.add(new ScreenInteraction.DetailItem(
                    "Ubicación " + index++,
                    location.code() + " · " + location.name() + " · "
                            + locationTypeLabel(location.type()) + " · " + stateLabel(location.active())));
        }
        items.add(new ScreenInteraction.DetailItem("Versión", Long.toString(snapshot.version())));
        return new ScreenInteraction.Detail(snapshot.id().toString(), snapshot.name(), items);
    }

    private static Map<ScreenElementId, List<ScreenInteraction.Option>> options(
            Optional<WarehouseSnapshot> selected) {
        Map<ScreenElementId, List<ScreenInteraction.Option>> options = new LinkedHashMap<>();
        options.put(InventoryScreenContract.WAREHOUSE_SEARCH_STATE, List.of(
                option(ALL, "Todos los estados"),
                option("ACTIVE", "Activo"),
                option("INACTIVE", "Inactivo")));
        options.put(InventoryScreenContract.LOCATION_NEW_TYPE, List.of(
                option(StockLocationType.STORAGE.name(), "Almacenamiento"),
                option(StockLocationType.RECEIVING.name(), "Recepción"),
                option(StockLocationType.DISPATCH.name(), "Despacho")));
        options.put(InventoryScreenContract.LOCATION_TO_RENAME,
                selected.stream().flatMap(snapshot -> snapshot.locations().stream())
                        .filter(StockLocationSnapshot::active)
                        .map(location -> option(location.id().toString(), location.code() + " · " + location.name()))
                        .toList());
        options.put(InventoryScreenContract.LOCATION_TO_INACTIVATE,
                selected.stream().flatMap(snapshot -> snapshot.locations().stream())
                        .filter(StockLocationSnapshot::active)
                        .filter(location -> location.type() != StockLocationType.GENERAL)
                        .map(location -> option(location.id().toString(), location.code() + " · " + location.name()))
                        .toList());
        return Map.copyOf(options);
    }

    private static Map<ScreenElementId, String> defaults(Map<ScreenElementId, String> submitted) {
        Map<ScreenElementId, String> inputs = InventoryScreenSupport.copy(submitted);
        inputs.putIfAbsent(InventoryScreenContract.WAREHOUSE_SEARCH_STATE, ALL);
        inputs.putIfAbsent(InventoryScreenContract.LOCATION_NEW_TYPE, StockLocationType.STORAGE.name());
        return inputs;
    }

    private static void applyOptionDefaults(
            Map<ScreenElementId, String> inputs,
            Map<ScreenElementId, List<ScreenInteraction.Option>> options) {
        first(options, InventoryScreenContract.LOCATION_TO_RENAME).ifPresent(value ->
                inputs.putIfAbsent(InventoryScreenContract.LOCATION_TO_RENAME, value));
        first(options, InventoryScreenContract.LOCATION_TO_INACTIVATE).ifPresent(value ->
                inputs.putIfAbsent(InventoryScreenContract.LOCATION_TO_INACTIVATE, value));
    }

    private static Optional<Boolean> activeFilter(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return filter(inputs, field).map("ACTIVE"::equals);
    }

    private static void clearMutationInputs(
            ScreenElementId action, Map<ScreenElementId, String> inputs) {
        if (action.equals(InventoryScreenContract.OPEN_WAREHOUSE)) {
            clear(inputs,
                    InventoryScreenContract.WAREHOUSE_NEW_CODE,
                    InventoryScreenContract.WAREHOUSE_NEW_NAME);
        } else if (action.equals(InventoryScreenContract.ADD_LOCATION)) {
            clear(inputs,
                    InventoryScreenContract.LOCATION_NEW_CODE,
                    InventoryScreenContract.LOCATION_NEW_NAME);
        } else if (action.equals(InventoryScreenContract.RENAME_LOCATION)) {
            clear(inputs, InventoryScreenContract.LOCATION_EDIT_NAME);
        }
    }

    private static String stateLabel(boolean active) {
        return active ? "Activo" : "Inactivo";
    }

    private static String locationTypeLabel(StockLocationType type) {
        return switch (type) {
            case GENERAL -> "General";
            case STORAGE -> "Almacenamiento";
            case RECEIVING -> "Recepción";
            case DISPATCH -> "Despacho";
        };
    }
}
