package py.com.logixone.plugins.inventory.infrastructure.ui;

import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.ALL;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.PAGE_SIZE;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.clear;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.context;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.date;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.decimal;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.enumValue;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.error;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.filterEnum;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.first;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.option;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.optional;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.required;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugin.api.SelectorSourceDefinition;
import py.com.logixone.plugins.inventory.InventoryScreenContract;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.InventoryOperationResult;
import py.com.logixone.plugins.inventory.application.InventoryPermissions;
import py.com.logixone.plugins.inventory.application.InventoryUseCases;
import py.com.logixone.plugins.inventory.application.command.InventoryCommands;
import py.com.logixone.plugins.inventory.application.query.InventoryDirectoryQueries;
import py.com.logixone.plugins.inventory.domain.StockCountLineSnapshot;
import py.com.logixone.plugins.inventory.domain.StockCountScope;
import py.com.logixone.plugins.inventory.domain.StockCountSnapshot;
import py.com.logixone.plugins.inventory.domain.StockCountState;
import py.com.logixone.plugins.inventory.domain.StockLocationSnapshot;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;

/** Neutral interaction adapter for the controlled physical-count workflow. */
@ApplicationScoped
public class InventoryCountScreenHandler implements ScreenInteraction.Handler {
    private static final Logger LOGGER = System.getLogger(
            InventoryCountScreenHandler.class.getName());
    private static final String NONE = "NONE";

    @Inject
    InventoryUseCases useCases;

    @Inject
    CurrentCompanyAuthorization authorization;

    @Override
    public ScreenId screenId() {
        return InventoryScreenContract.COUNTS;
    }

    @Override
    public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return InventorySelectorSources.COUNTS;
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = defaults(request.inputs());
        Optional<String> selectedId = request.selectedResourceId();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();
        try {
            if (request.actionId().isPresent()) {
                ScreenElementId action = request.actionId().orElseThrow();
                if (action.equals(InventoryScreenContract.COUNT_SEARCH)) {
                    selectedId = Optional.empty();
                } else if (!action.equals(InventoryScreenContract.SELECT_COUNT)) {
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
                    "event=inventory_count_screen_input_rejected action={0} input_keys={1}",
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
        if (action.equals(InventoryScreenContract.DRAFT_COUNT)) {
            WarehouseId warehouseId = WarehouseId.parse(
                    required(inputs, InventoryScreenContract.COUNT_NEW_WAREHOUSE));
            Optional<StockLocationId> locationId = optional(
                    inputs, InventoryScreenContract.COUNT_NEW_LOCATION)
                    .filter(value -> !NONE.equals(value))
                    .map(StockLocationId::parse);
            var command = new InventoryCommands.DraftCount(
                    new StockCountScope(warehouseId, locationId));
            return InventoryScreenSupport.mutation(
                    useCases.draftCount(
                            context(authorization, InventoryPermissions.COUNTS_MANAGE), command),
                    "Conteo físico preparado",
                    snapshot -> snapshot.id().toString(),
                    Optional.empty());
        }

        StockCountId countId = selectedCount(request);
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("A selected stock count version is required"));
        Optional<String> fallback = Optional.of(countId.toString());

        if (action.equals(InventoryScreenContract.ADD_COUNT_LINE)) {
            StockCountSnapshot count = selectedCountSnapshot(countId, version);
            StockKey key = countKey(count.scope(), inputs);
            return InventoryScreenSupport.mutation(
                    useCases.addCountLine(
                            context(authorization, InventoryPermissions.COUNTS_MANAGE),
                            new InventoryCommands.AddCountLine(countId, version, key)),
                    "Línea agregada al conteo",
                    snapshot -> snapshot.id().toString(),
                    fallback);
        }
        if (action.equals(InventoryScreenContract.RECORD_COUNT)) {
            StockCountSnapshot count = selectedCountSnapshot(countId, version);
            int lineNumber = Integer.parseInt(
                    required(inputs, InventoryScreenContract.COUNT_CAPTURE_LINE));
            StockCountLineSnapshot line = count.lines().stream()
                    .filter(candidate -> candidate.lineNumber() == lineNumber)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Count line is no longer available"));
            return InventoryScreenSupport.mutation(
                    useCases.recordCount(
                            context(authorization, InventoryPermissions.COUNTS_MANAGE),
                            new InventoryCommands.RecordCount(
                                    countId, version, line.key(),
                                    decimal(inputs, InventoryScreenContract.COUNT_CAPTURE_QUANTITY))),
                    "Cantidad contada registrada",
                    snapshot -> snapshot.id().toString(),
                    fallback);
        }
        if (action.equals(InventoryScreenContract.START_COUNT)) {
            return transition(countId, version, InventoryPermissions.COUNTS_MANAGE,
                    useCases::startCount, "Conteo iniciado");
        }
        if (action.equals(InventoryScreenContract.REVIEW_COUNT)) {
            return transition(countId, version, InventoryPermissions.COUNTS_MANAGE,
                    useCases::reviewCount, "Conteo enviado a revisión");
        }
        if (action.equals(InventoryScreenContract.POST_COUNT)) {
            return transition(countId, version, InventoryPermissions.ADJUSTMENTS_POST,
                    useCases::postCount, "Conteo contabilizado y ajustes registrados");
        }
        if (action.equals(InventoryScreenContract.CANCEL_COUNT)) {
            return transition(countId, version, InventoryPermissions.COUNTS_MANAGE,
                    useCases::cancelCount, "Conteo cancelado");
        }
        throw new IllegalArgumentException("Unsupported inventory count screen action");
    }

    private InventoryScreenSupport.Mutation transition(
            StockCountId countId,
            long version,
            ContributionId permission,
            CountTransition operation,
            String summary) {
        InventoryOperationResult<StockCountSnapshot> result = operation.apply(
                context(authorization, permission),
                new InventoryCommands.CountTransition(countId, version));
        return InventoryScreenSupport.mutation(
                result, summary, snapshot -> snapshot.id().toString(), Optional.of(countId.toString()));
    }

    private StockCountSnapshot selectedCountSnapshot(StockCountId countId, long version) {
        var found = useCases.count(context(authorization, InventoryPermissions.VIEW), countId);
        if (!found.successful() || found.value().orElseThrow().version() != version) {
            throw new IllegalArgumentException("Stock count is no longer available at submitted version");
        }
        return found.value().orElseThrow();
    }

    private ScreenInteraction.Result load(
            Map<ScreenElementId, String> inputs,
            Optional<String> selectedId,
            List<ScreenInteraction.Notice> notices) {
        String stage = "authorization";
        try {
            var viewContext = context(authorization, InventoryPermissions.VIEW);
            stage = "search";
            var search = useCases.searchCounts(
                    viewContext,
                    new InventoryDirectoryQueries.CountCriteria(
                            filterEnum(
                                    inputs, InventoryScreenContract.COUNT_SEARCH_STATE,
                                    StockCountState.class),
                            0,
                            PAGE_SIZE));
            if (!search.successful()) {
                throw new IllegalStateException("Authorized stock count search failed");
            }
            stage = "warehouses";
            var warehouseSearch = useCases.searchWarehouses(
                    viewContext,
                    new InventoryDirectoryQueries.Criteria(Optional.empty(), Optional.of(true), 0, 100));
            if (!warehouseSearch.successful()) {
                throw new IllegalStateException("Authorized warehouse option search failed");
            }
            List<WarehouseSnapshot> warehouses = warehouseSearch.value().orElseThrow().items();
            stage = "items";
            var itemSearch = useCases.searchItems(
                    viewContext,
                    new InventoryDirectoryQueries.Criteria(Optional.empty(), Optional.of(true), 0, 100));
            if (!itemSearch.successful()) {
                throw new IllegalStateException("Authorized inventory item option search failed");
            }
            List<InventoryDirectoryQueries.ItemSummary> inventoryItems =
                    itemSearch.value().orElseThrow().items();

            Optional<StockCountSnapshot> selected = Optional.empty();
            Optional<ScreenInteraction.Detail> detail = Optional.empty();
            Optional<Long> selectedVersion = Optional.empty();
            if (selectedId.isPresent()) {
                StockCountId id = StockCountId.parse(selectedId.orElseThrow());
                var found = useCases.count(viewContext, id);
                if (found.successful()) {
                    selected = found.value();
                    detail = Optional.of(detail(
                            selected.orElseThrow(), warehouses, inventoryItems));
                    selectedVersion = Optional.of(selected.orElseThrow().version());
                } else {
                    selectedId = Optional.empty();
                    notices.add(error(
                            "Conteo físico no disponible",
                            "El conteo ya no existe o no pertenece a la empresa activa."));
                }
            }

            Map<ScreenElementId, List<ScreenInteraction.Option>> options =
                    options(warehouses, inventoryItems, selected);
            applyOptionDefaults(inputs, options);
            if (warehouses.isEmpty() || inventoryItems.isEmpty()) {
                notices.add(new ScreenInteraction.Notice(
                        ScreenInteraction.NoticeLevel.INFO,
                        "Estructura pendiente",
                        "Necesitas un depósito y un artículo de inventario activos para preparar un conteo."));
            }
            return new ScreenInteraction.Result(
                    inputs,
                    options,
                    Optional.of(table(search.value().orElseThrow(), warehouses)),
                    detail,
                    notices,
                    selectedId,
                    selectedVersion);
        } catch (RuntimeException failure) {
            LOGGER.log(Level.ERROR,
                    "event=inventory_count_screen_load_failed stage={0} type={1}",
                    stage, failure.getClass().getName());
            throw failure;
        }
    }

    private static StockKey countKey(
            StockCountScope scope, Map<ScreenElementId, String> inputs) {
        return new StockKey(
                InventoryItemId.parse(required(inputs, InventoryScreenContract.COUNT_LINE_ITEM)),
                scope.warehouseId(),
                StockLocationId.parse(required(inputs, InventoryScreenContract.COUNT_LINE_LOCATION)),
                optional(inputs, InventoryScreenContract.COUNT_LINE_LOT),
                optional(inputs, InventoryScreenContract.COUNT_LINE_SERIAL),
                date(inputs, InventoryScreenContract.COUNT_LINE_EXPIRY),
                enumValue(inputs, InventoryScreenContract.COUNT_LINE_CONDITION, StockCondition.class));
    }

    private static StockCountId selectedCount(ScreenInteraction.Request request) {
        return StockCountId.parse(request.selectedResourceId().orElseThrow(
                () -> new IllegalArgumentException("A selected stock count is required")));
    }

    private static ScreenInteraction.Table table(
            InventoryDirectoryQueries.Page<InventoryDirectoryQueries.CountSummary> page,
            List<WarehouseSnapshot> warehouses) {
        Map<WarehouseId, String> names = warehouseNames(warehouses);
        return new ScreenInteraction.Table(
                InventoryScreenContract.COUNT_RESULTS,
                List.of(
                        new ScreenInteraction.Column("id", "Conteo"),
                        new ScreenInteraction.Column("scope", "Alcance"),
                        new ScreenInteraction.Column("state", "Estado"),
                        new ScreenInteraction.Column("lines", "Líneas"),
                        new ScreenInteraction.Column("version", "Versión")),
                page.items().stream().map(summary -> new ScreenInteraction.Row(
                        summary.id().toString(),
                        List.of(
                                shortId(summary.id().toString()),
                                scopeLabel(summary.scope(), names),
                                stateLabel(summary.state()),
                                Long.toString(summary.lineCount()),
                                Long.toString(summary.version())))).toList(),
                page.total(),
                "No encontramos conteos físicos",
                "Ajusta el estado o prepara el primer conteo controlado.");
    }

    private static ScreenInteraction.Detail detail(
            StockCountSnapshot count,
            List<WarehouseSnapshot> warehouses,
            List<InventoryDirectoryQueries.ItemSummary> inventoryItems) {
        Map<WarehouseId, String> warehouseNames = warehouseNames(warehouses);
        Map<InventoryItemId, String> itemNames = new HashMap<>();
        inventoryItems.forEach(summary -> itemNames.put(summary.id(), summary.item().catalogName()));
        List<ScreenInteraction.DetailItem> details = new ArrayList<>();
        details.add(new ScreenInteraction.DetailItem("Alcance", scopeLabel(count.scope(), warehouseNames)));
        details.add(new ScreenInteraction.DetailItem("Estado", stateLabel(count.state())));
        details.add(new ScreenInteraction.DetailItem("Líneas", Integer.toString(count.lines().size())));
        details.add(new ScreenInteraction.DetailItem(
                "Líneas contadas",
                Long.toString(count.lines().stream()
                        .filter(line -> line.countedQuantity().isPresent()).count())));
        details.add(new ScreenInteraction.DetailItem(
                "Diferencias",
                Long.toString(count.lines().stream()
                        .filter(line -> line.countedQuantity().isPresent()
                                && line.countedQuantity().orElseThrow()
                                        .compareTo(line.theoreticalQuantity()) != 0)
                        .count())));
        for (StockCountLineSnapshot line : count.lines().stream().limit(20).toList()) {
            String item = itemNames.getOrDefault(
                    line.key().inventoryItemId(), shortId(line.key().inventoryItemId().toString()));
            details.add(new ScreenInteraction.DetailItem(
                    "Línea " + line.lineNumber(),
                    item + " · teórico " + line.theoreticalQuantity().toPlainString()
                            + " · contado " + line.countedQuantity()
                                    .map(java.math.BigDecimal::toPlainString).orElse("pendiente")));
        }
        details.add(new ScreenInteraction.DetailItem("Versión", Long.toString(count.version())));
        return new ScreenInteraction.Detail(
                count.id().toString(), "Conteo " + shortId(count.id().toString()), details);
    }

    private static Map<ScreenElementId, List<ScreenInteraction.Option>> options(
            List<WarehouseSnapshot> warehouses,
            List<InventoryDirectoryQueries.ItemSummary> inventoryItems,
            Optional<StockCountSnapshot> selected) {
        Map<ScreenElementId, List<ScreenInteraction.Option>> options = new LinkedHashMap<>();
        List<ScreenInteraction.Option> states = new ArrayList<>();
        states.add(option(ALL, "Todos los estados"));
        for (StockCountState state : StockCountState.values()) {
            states.add(option(state.name(), stateLabel(state)));
        }
        options.put(InventoryScreenContract.COUNT_SEARCH_STATE, states);
        options.put(InventoryScreenContract.COUNT_NEW_WAREHOUSE,
                warehouses.stream().filter(WarehouseSnapshot::active)
                        .map(warehouse -> option(
                                warehouse.id().toString(), warehouse.code() + " · " + warehouse.name()))
                        .toList());
        List<ScreenInteraction.Option> countLocations = new ArrayList<>();
        countLocations.add(option(NONE, "Todo el depósito"));
        countLocations.addAll(warehouses.stream()
                .filter(WarehouseSnapshot::active)
                .flatMap(warehouse -> warehouse.locations().stream()
                        .filter(StockLocationSnapshot::active)
                        .map(location -> option(
                                location.id().toString(),
                                warehouse.code() + " · " + location.code() + " · " + location.name())))
                .toList());
        options.put(InventoryScreenContract.COUNT_NEW_LOCATION, countLocations);
        options.put(InventoryScreenContract.COUNT_LINE_ITEM,
                inventoryItems.stream()
                        .filter(summary -> summary.item().active())
                        .map(summary -> option(
                                summary.id().toString(),
                                summary.item().catalogCode() + " · " + summary.item().catalogName()))
                        .toList());
        options.put(InventoryScreenContract.COUNT_LINE_LOCATION,
                selected.stream().flatMap(count -> warehouses.stream()
                        .filter(warehouse -> warehouse.id().equals(count.scope().warehouseId()))
                        .flatMap(warehouse -> warehouse.locations().stream())
                        .filter(StockLocationSnapshot::active)
                        .filter(location -> count.scope().locationId()
                                .map(location.id()::equals).orElse(true)))
                        .map(location -> option(
                                location.id().toString(), location.code() + " · " + location.name()))
                        .toList());
        options.put(InventoryScreenContract.COUNT_LINE_CONDITION, List.of(
                option(StockCondition.AVAILABLE.name(), "Disponible"),
                option(StockCondition.QUARANTINED.name(), "En cuarentena"),
                option(StockCondition.DAMAGED.name(), "Dañado")));
        options.put(InventoryScreenContract.COUNT_CAPTURE_LINE,
                selected.stream().flatMap(count -> count.lines().stream())
                        .map(line -> option(
                                Integer.toString(line.lineNumber()),
                                "Línea " + line.lineNumber() + " · teórico "
                                        + line.theoreticalQuantity().toPlainString()))
                        .toList());
        return Map.copyOf(options);
    }

    private static Map<ScreenElementId, String> defaults(Map<ScreenElementId, String> submitted) {
        Map<ScreenElementId, String> inputs = InventoryScreenSupport.copy(submitted);
        inputs.putIfAbsent(InventoryScreenContract.COUNT_SEARCH_STATE, ALL);
        inputs.putIfAbsent(InventoryScreenContract.COUNT_NEW_LOCATION, NONE);
        inputs.putIfAbsent(InventoryScreenContract.COUNT_LINE_CONDITION, StockCondition.AVAILABLE.name());
        return inputs;
    }

    private static void applyOptionDefaults(
            Map<ScreenElementId, String> inputs,
            Map<ScreenElementId, List<ScreenInteraction.Option>> options) {
        for (ScreenElementId field : List.of(
                InventoryScreenContract.COUNT_NEW_WAREHOUSE,
                InventoryScreenContract.COUNT_LINE_ITEM,
                InventoryScreenContract.COUNT_LINE_LOCATION,
                InventoryScreenContract.COUNT_CAPTURE_LINE)) {
            first(options, field).ifPresent(value -> inputs.putIfAbsent(field, value));
        }
    }

    private static void clearMutationInputs(
            ScreenElementId action, Map<ScreenElementId, String> inputs) {
        if (action.equals(InventoryScreenContract.ADD_COUNT_LINE)) {
            clear(inputs,
                    InventoryScreenContract.COUNT_LINE_LOT,
                    InventoryScreenContract.COUNT_LINE_SERIAL,
                    InventoryScreenContract.COUNT_LINE_EXPIRY);
        } else if (action.equals(InventoryScreenContract.RECORD_COUNT)) {
            clear(inputs, InventoryScreenContract.COUNT_CAPTURE_QUANTITY);
        }
    }

    private static Map<WarehouseId, String> warehouseNames(List<WarehouseSnapshot> warehouses) {
        Map<WarehouseId, String> result = new HashMap<>();
        warehouses.forEach(warehouse -> result.put(
                warehouse.id(), warehouse.code() + " · " + warehouse.name()));
        return result;
    }

    private static String scopeLabel(
            StockCountScope scope, Map<WarehouseId, String> warehouses) {
        String warehouse = warehouses.getOrDefault(
                scope.warehouseId(), shortId(scope.warehouseId().toString()));
        return scope.locationId()
                .map(location -> warehouse + " · ubicación " + shortId(location.toString()))
                .orElse(warehouse + " · todo el depósito");
    }

    private static String stateLabel(StockCountState state) {
        return switch (state) {
            case DRAFT -> "Borrador";
            case COUNTING -> "En conteo";
            case REVIEW -> "En revisión";
            case POSTED -> "Contabilizado";
            case CANCELLED -> "Cancelado";
        };
    }

    private static String shortId(String value) {
        return value.substring(0, Math.min(8, value.length())).toUpperCase(java.util.Locale.ROOT);
    }

    @FunctionalInterface
    private interface CountTransition {
        InventoryOperationResult<StockCountSnapshot> apply(
                py.com.logixone.plugins.inventory.application.InventoryOperationContext context,
                InventoryCommands.CountTransition command);
    }
}
