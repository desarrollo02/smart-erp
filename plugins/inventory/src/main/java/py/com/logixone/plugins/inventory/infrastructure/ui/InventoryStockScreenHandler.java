package py.com.logixone.plugins.inventory.infrastructure.ui;

import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.ALL;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.PAGE_SIZE;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.clear;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.context;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.date;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.decimal;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.enumValue;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.error;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.filter;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.first;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.instant;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.longValue;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.option;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.optional;
import static py.com.logixone.plugins.inventory.infrastructure.ui.InventoryScreenSupport.required;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugin.api.SelectorSourceDefinition;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemReference;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.inventory.InventoryScreenContract;
import py.com.logixone.plugins.inventory.api.ExpiryPolicy;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.MovementQuantity;
import py.com.logixone.plugins.inventory.api.StockAvailability;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StockMovementDirection;
import py.com.logixone.plugins.inventory.api.StockMovementLine;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;
import py.com.logixone.plugins.inventory.api.StockMovementType;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.StockReservationReference;
import py.com.logixone.plugins.inventory.api.StockReservationRequest;
import py.com.logixone.plugins.inventory.api.StockSourceReference;
import py.com.logixone.plugins.inventory.api.TrackingMode;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.InventoryOperationResult;
import py.com.logixone.plugins.inventory.application.InventoryPermissions;
import py.com.logixone.plugins.inventory.application.InventoryUseCases;
import py.com.logixone.plugins.inventory.application.command.InventoryCommands;
import py.com.logixone.plugins.inventory.application.query.InventoryDirectoryQueries;
import py.com.logixone.plugins.inventory.domain.InventoryItemSnapshot;
import py.com.logixone.plugins.inventory.domain.StockLocationSnapshot;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;

/** Neutral interaction adapter for availability, movements and reservations. */
@ApplicationScoped
public class InventoryStockScreenHandler implements ScreenInteraction.Handler {
    private static final Logger LOGGER = System.getLogger(
            InventoryStockScreenHandler.class.getName());

    @Inject
    InventoryUseCases useCases;

    @Inject
    CatalogItemDirectory catalog;

    @Inject
    CurrentCompanyAuthorization authorization;

    @Override
    public ScreenId screenId() {
        return InventoryScreenContract.STOCK;
    }

    @Override
    public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return InventorySelectorSources.STOCK;
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = defaults(request.inputs());
        Optional<String> selectedId = request.selectedResourceId();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();
        try {
            if (request.actionId().isPresent()) {
                ScreenElementId action = request.actionId().orElseThrow();
                if (action.equals(InventoryScreenContract.STOCK_SEARCH)) {
                    selectedId = Optional.empty();
                } else if (action.equals(InventoryScreenContract.CHECK_AVAILABILITY)) {
                    notices.add(availabilityNotice(request, inputs));
                } else if (!action.equals(InventoryScreenContract.SELECT_STOCK_ITEM)) {
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
                    "event=inventory_stock_screen_input_rejected action={0} input_keys={1}",
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
        if (action.equals(InventoryScreenContract.ENROLL_STOCK_ITEM)) {
            var command = new InventoryCommands.EnrollItem(
                    CatalogItemId.parse(required(inputs, InventoryScreenContract.STOCK_NEW_CATALOG_ITEM)),
                    enumValue(inputs, InventoryScreenContract.STOCK_NEW_TRACKING, TrackingMode.class),
                    enumValue(inputs, InventoryScreenContract.STOCK_NEW_EXPIRY, ExpiryPolicy.class));
            return InventoryScreenSupport.mutation(
                    useCases.enrollItem(context(authorization, InventoryPermissions.ITEMS_MANAGE), command),
                    "Artículo incorporado al inventario",
                    snapshot -> snapshot.id().toString(),
                    Optional.empty());
        }

        InventoryItemId itemId = selectedItem(request);
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("A selected inventory item version is required"));
        Optional<String> fallback = Optional.of(itemId.toString());

        if (action.equals(InventoryScreenContract.POST_MOVEMENT)) {
            StockMovementRequest movement = movement(itemId, inputs);
            return InventoryScreenSupport.mutation(
                    useCases.postMovement(
                            context(authorization, InventoryPermissions.MOVEMENTS_POST), movement),
                    "Movimiento de existencias registrado",
                    ignored -> itemId.toString(),
                    fallback);
        }
        if (action.equals(InventoryScreenContract.CREATE_RESERVATION)) {
            StockReservationRequest reservation = new StockReservationRequest(
                    stockKey(
                            itemId,
                            inputs,
                            InventoryScreenContract.RESERVATION_WAREHOUSE,
                            InventoryScreenContract.RESERVATION_LOCATION,
                            InventoryScreenContract.RESERVATION_CONDITION,
                            InventoryScreenContract.RESERVATION_LOT,
                            InventoryScreenContract.RESERVATION_SERIAL,
                            InventoryScreenContract.RESERVATION_EXPIRY_DATE),
                    decimal(inputs, InventoryScreenContract.RESERVATION_QUANTITY),
                    source(
                            inputs,
                            InventoryScreenContract.RESERVATION_SOURCE_TYPE,
                            InventoryScreenContract.RESERVATION_SOURCE_ID),
                    instant(inputs, InventoryScreenContract.RESERVATION_EXPIRES_AT),
                    required(inputs, InventoryScreenContract.RESERVATION_IDEMPOTENCY));
            return InventoryScreenSupport.mutation(
                    useCases.reserve(
                            context(authorization, InventoryPermissions.RESERVATIONS_MANAGE), reservation),
                    "Reserva creada",
                    ignored -> itemId.toString(),
                    fallback);
        }
        if (action.equals(InventoryScreenContract.CONSUME_RESERVATION)
                || action.equals(InventoryScreenContract.RELEASE_RESERVATION)
                || action.equals(InventoryScreenContract.EXPIRE_RESERVATION)) {
            StockReservationReference reservation = reservationForSelected(itemId, inputs);
            String key = required(inputs, InventoryScreenContract.MANAGE_RESERVATION_IDEMPOTENCY);
            InventoryOperationResult<StockReservationReference> result;
            String summary;
            if (action.equals(InventoryScreenContract.CONSUME_RESERVATION)) {
                result = useCases.consume(
                        context(authorization, InventoryPermissions.RESERVATIONS_MANAGE),
                        new InventoryCommands.ConsumeReservation(
                                reservation.id(), reservation.version(),
                                decimal(inputs, InventoryScreenContract.MANAGE_RESERVATION_QUANTITY), key));
                summary = "Reserva consumida";
            } else if (action.equals(InventoryScreenContract.RELEASE_RESERVATION)) {
                result = useCases.release(
                        context(authorization, InventoryPermissions.RESERVATIONS_MANAGE),
                        new InventoryCommands.ReleaseReservation(
                                reservation.id(), reservation.version(),
                                decimal(inputs, InventoryScreenContract.MANAGE_RESERVATION_QUANTITY), key));
                summary = "Reserva liberada";
            } else {
                result = useCases.expire(
                        context(authorization, InventoryPermissions.RESERVATIONS_MANAGE),
                        new InventoryCommands.ExpireReservation(
                                reservation.id(), reservation.version(), key));
                summary = "Reserva vencida";
            }
            return InventoryScreenSupport.mutation(
                    result, summary, ignored -> itemId.toString(), fallback);
        }
        if (action.equals(InventoryScreenContract.REFRESH_STOCK_ITEM)) {
            return InventoryScreenSupport.mutation(
                    useCases.refreshItem(
                            context(authorization, InventoryPermissions.ITEMS_MANAGE),
                            new InventoryCommands.RefreshItem(itemId, version)),
                    "Referencia del catálogo actualizada",
                    snapshot -> snapshot.id().toString(),
                    fallback);
        }
        if (action.equals(InventoryScreenContract.INACTIVATE_STOCK_ITEM)) {
            return InventoryScreenSupport.mutation(
                    useCases.inactivateItem(
                            context(authorization, InventoryPermissions.ITEMS_MANAGE),
                            new InventoryCommands.InactivateItem(itemId, version)),
                    "Artículo de inventario inactivado",
                    snapshot -> snapshot.id().toString(),
                    fallback);
        }
        throw new IllegalArgumentException("Unsupported inventory stock screen action");
    }

    private ScreenInteraction.Notice availabilityNotice(
            ScreenInteraction.Request request, Map<ScreenElementId, String> inputs) {
        InventoryItemId itemId = selectedItem(request);
        StockKey key = stockKey(
                itemId,
                inputs,
                InventoryScreenContract.AVAILABILITY_WAREHOUSE,
                InventoryScreenContract.AVAILABILITY_LOCATION,
                InventoryScreenContract.AVAILABILITY_CONDITION,
                InventoryScreenContract.AVAILABILITY_LOT,
                InventoryScreenContract.AVAILABILITY_SERIAL,
                InventoryScreenContract.AVAILABILITY_EXPIRY);
        var found = useCases.availability(context(authorization, InventoryPermissions.VIEW), key);
        if (!found.successful()) {
            return error("No se pudo consultar la disponibilidad",
                    InventoryScreenSupport.failureMessage(found.code()));
        }
        StockAvailability value = found.value().orElseThrow();
        return new ScreenInteraction.Notice(
                ScreenInteraction.NoticeLevel.INFO,
                "Disponibilidad consultada",
                "Físico: " + value.physicalQuantity().toPlainString()
                        + " · Reservado: " + value.reservedQuantity().toPlainString()
                        + " · Disponible: " + value.availableQuantity().toPlainString()
                        + " " + value.baseUnitCode());
    }

    private StockMovementRequest movement(
            InventoryItemId itemId, Map<ScreenElementId, String> inputs) {
        InventoryItemSnapshot item = selectedItemSnapshot(itemId);
        StockMovementType type = enumValue(
                inputs, InventoryScreenContract.MOVEMENT_TYPE, StockMovementType.class);
        if (type == StockMovementType.ADJUSTMENT || type == StockMovementType.REVERSAL) {
            throw new IllegalArgumentException("Unsupported manual movement type");
        }
        BigDecimal quantity = decimal(inputs, InventoryScreenContract.MOVEMENT_QUANTITY);
        MovementQuantity movementQuantity = new MovementQuantity(
                item.baseUnitCode(), quantity, item.baseUnitCode(), BigDecimal.ONE,
                quantity, item.catalogItemVersion());
        StockKey sourceKey = stockKey(
                itemId,
                inputs,
                InventoryScreenContract.MOVEMENT_WAREHOUSE,
                InventoryScreenContract.MOVEMENT_LOCATION,
                InventoryScreenContract.MOVEMENT_CONDITION,
                InventoryScreenContract.MOVEMENT_LOT,
                InventoryScreenContract.MOVEMENT_SERIAL,
                InventoryScreenContract.MOVEMENT_EXPIRY);
        List<StockMovementLine> lines;
        if (type == StockMovementType.TRANSFER) {
            StockKey targetKey = new StockKey(
                    itemId,
                    WarehouseId.parse(required(inputs, InventoryScreenContract.MOVEMENT_TARGET_WAREHOUSE)),
                    StockLocationId.parse(required(inputs, InventoryScreenContract.MOVEMENT_TARGET_LOCATION)),
                    sourceKey.lotCode(), sourceKey.serialNumber(), sourceKey.expiryDate(), sourceKey.condition());
            lines = List.of(
                    new StockMovementLine(sourceKey, StockMovementDirection.DECREASE, movementQuantity),
                    new StockMovementLine(targetKey, StockMovementDirection.INCREASE, movementQuantity));
        } else {
            lines = List.of(new StockMovementLine(
                    sourceKey,
                    type == StockMovementType.RECEIPT
                            ? StockMovementDirection.INCREASE : StockMovementDirection.DECREASE,
                    movementQuantity));
        }
        return new StockMovementRequest(
                type,
                required(inputs, InventoryScreenContract.MOVEMENT_REASON),
                source(
                        inputs,
                        InventoryScreenContract.MOVEMENT_SOURCE_TYPE,
                        InventoryScreenContract.MOVEMENT_SOURCE_ID),
                required(inputs, InventoryScreenContract.MOVEMENT_IDEMPOTENCY),
                lines,
                Optional.empty());
    }

    private StockReservationReference reservationForSelected(
            InventoryItemId itemId, Map<ScreenElementId, String> inputs) {
        StockReservationId reservationId = StockReservationId.parse(
                required(inputs, InventoryScreenContract.MANAGE_RESERVATION_ID));
        long submittedVersion = longValue(inputs, InventoryScreenContract.MANAGE_RESERVATION_VERSION);
        var found = useCases.reservation(
                context(authorization, InventoryPermissions.VIEW), reservationId);
        if (!found.successful()
                || !found.value().orElseThrow().key().inventoryItemId().equals(itemId)
                || found.value().orElseThrow().version() != submittedVersion) {
            throw new IllegalArgumentException("Reservation does not match selected item and version");
        }
        return found.value().orElseThrow();
    }

    private InventoryItemSnapshot selectedItemSnapshot(InventoryItemId itemId) {
        var found = useCases.item(context(authorization, InventoryPermissions.VIEW), itemId);
        if (!found.successful()) {
            throw new IllegalArgumentException("Inventory item is no longer available");
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
            var search = useCases.searchItems(
                    viewContext,
                    new InventoryDirectoryQueries.Criteria(
                            filter(inputs, InventoryScreenContract.STOCK_SEARCH_TEXT),
                            activeFilter(inputs, InventoryScreenContract.STOCK_SEARCH_STATE),
                            0,
                            PAGE_SIZE));
            if (!search.successful()) {
                throw new IllegalStateException("Authorized inventory item search failed");
            }
            stage = "warehouses";
            var warehouseSearch = useCases.searchWarehouses(
                    viewContext,
                    new InventoryDirectoryQueries.Criteria(Optional.empty(), Optional.of(true), 0, 100));
            if (!warehouseSearch.successful()) {
                throw new IllegalStateException("Authorized warehouse option search failed");
            }
            List<WarehouseSnapshot> warehouses = warehouseSearch.value().orElseThrow().items();

            stage = "catalog";
            List<CatalogItemReference> catalogItems = catalog.search(
                    viewContext.companyContext().companyId(),
                    new CatalogSearchCriteria(
                            "", Set.of(CatalogItemType.PRODUCT), Set.of(CatalogItemState.ACTIVE), 0, 100))
                    .items();

            Optional<ScreenInteraction.Detail> detail = Optional.empty();
            Optional<Long> selectedVersion = Optional.empty();
            if (selectedId.isPresent()) {
                InventoryItemId id = InventoryItemId.parse(selectedId.orElseThrow());
                var found = useCases.itemSummary(viewContext, id);
                if (found.successful()) {
                    InventoryDirectoryQueries.ItemSummary summary = found.value().orElseThrow();
                    detail = Optional.of(detail(summary));
                    selectedVersion = Optional.of(summary.item().version());
                } else {
                    selectedId = Optional.empty();
                    notices.add(error(
                            "Artículo de inventario no disponible",
                            "El registro ya no existe o no pertenece a la empresa activa."));
                }
            }

            Map<ScreenElementId, List<ScreenInteraction.Option>> options =
                    options(catalogItems, warehouses);
            applyOptionDefaults(inputs, options);
            if (catalogItems.isEmpty()) {
                notices.add(new ScreenInteraction.Notice(
                        ScreenInteraction.NoticeLevel.INFO,
                        "Catálogo pendiente",
                        "Necesitas al menos un producto activo antes de incorporarlo al inventario."));
            }
            if (warehouses.isEmpty()) {
                notices.add(new ScreenInteraction.Notice(
                        ScreenInteraction.NoticeLevel.INFO,
                        "Depósito pendiente",
                        "Crea un depósito activo antes de consultar o modificar existencias."));
            }

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
                    "event=inventory_stock_screen_load_failed stage={0} type={1}",
                    stage, failure.getClass().getName());
            throw failure;
        }
    }

    private static StockKey stockKey(
            InventoryItemId itemId,
            Map<ScreenElementId, String> inputs,
            ScreenElementId warehouse,
            ScreenElementId location,
            ScreenElementId condition,
            ScreenElementId lot,
            ScreenElementId serial,
            ScreenElementId expiry) {
        return new StockKey(
                itemId,
                WarehouseId.parse(required(inputs, warehouse)),
                StockLocationId.parse(required(inputs, location)),
                optional(inputs, lot),
                optional(inputs, serial),
                date(inputs, expiry),
                enumValue(inputs, condition, StockCondition.class));
    }

    private static StockSourceReference source(
            Map<ScreenElementId, String> inputs, ScreenElementId type, ScreenElementId id) {
        return new StockSourceReference(required(inputs, type), required(inputs, id));
    }

    private static InventoryItemId selectedItem(ScreenInteraction.Request request) {
        return InventoryItemId.parse(request.selectedResourceId().orElseThrow(
                () -> new IllegalArgumentException("A selected inventory item is required")));
    }

    private static ScreenInteraction.Table table(
            InventoryDirectoryQueries.Page<InventoryDirectoryQueries.ItemSummary> page) {
        return new ScreenInteraction.Table(
                InventoryScreenContract.STOCK_RESULTS,
                List.of(
                        new ScreenInteraction.Column("code", "Código"),
                        new ScreenInteraction.Column("name", "Nombre"),
                        new ScreenInteraction.Column("available", "Disponible"),
                        new ScreenInteraction.Column("reserved", "Reservado"),
                        new ScreenInteraction.Column("unit", "Unidad"),
                        new ScreenInteraction.Column("state", "Estado")),
                page.items().stream().map(summary -> new ScreenInteraction.Row(
                        summary.id().toString(),
                        List.of(
                                summary.item().catalogCode(),
                                summary.item().catalogName(),
                                summary.availableQuantity().toPlainString(),
                                summary.reservedQuantity().toPlainString(),
                                summary.item().baseUnitCode(),
                                stateLabel(summary.item().active())))).toList(),
                page.total(),
                "No encontramos artículos de inventario",
                "Ajusta los filtros o incorpora el primer producto activo del catálogo.");
    }

    private static ScreenInteraction.Detail detail(InventoryDirectoryQueries.ItemSummary summary) {
        InventoryItemSnapshot item = summary.item();
        return new ScreenInteraction.Detail(
                item.id().toString(),
                item.catalogName(),
                List.of(
                        new ScreenInteraction.DetailItem("Código", item.catalogCode()),
                        new ScreenInteraction.DetailItem("Estado", stateLabel(item.active())),
                        new ScreenInteraction.DetailItem("Unidad base", item.baseUnitCode()),
                        new ScreenInteraction.DetailItem("Seguimiento", trackingLabel(item.trackingMode())),
                        new ScreenInteraction.DetailItem("Vencimiento", expiryLabel(item.expiryPolicy())),
                        new ScreenInteraction.DetailItem(
                                "Existencia física", summary.physicalQuantity().toPlainString()),
                        new ScreenInteraction.DetailItem(
                                "Reservado", summary.reservedQuantity().toPlainString()),
                        new ScreenInteraction.DetailItem(
                                "Disponible", summary.availableQuantity().toPlainString()),
                        new ScreenInteraction.DetailItem(
                                "Posiciones con saldo", Long.toString(summary.balanceBuckets())),
                        new ScreenInteraction.DetailItem(
                                "Versión de catálogo", Long.toString(item.catalogItemVersion())),
                        new ScreenInteraction.DetailItem("Versión", Long.toString(item.version()))));
    }

    private static Map<ScreenElementId, List<ScreenInteraction.Option>> options(
            List<CatalogItemReference> catalogItems, List<WarehouseSnapshot> warehouses) {
        Map<ScreenElementId, List<ScreenInteraction.Option>> options = new LinkedHashMap<>();
        options.put(InventoryScreenContract.STOCK_SEARCH_STATE, List.of(
                option(ALL, "Todos los estados"),
                option("ACTIVE", "Activo"),
                option("INACTIVE", "Inactivo")));
        options.put(InventoryScreenContract.STOCK_NEW_CATALOG_ITEM,
                catalogItems.stream().map(item -> option(
                        item.id().toString(), item.code() + " · " + item.displayName())).toList());
        options.put(InventoryScreenContract.STOCK_NEW_TRACKING, List.of(
                option(TrackingMode.NONE.name(), "Sin lote ni serie"),
                option(TrackingMode.LOT.name(), "Por lote"),
                option(TrackingMode.SERIAL.name(), "Por número de serie")));
        options.put(InventoryScreenContract.STOCK_NEW_EXPIRY, List.of(
                option(ExpiryPolicy.NONE.name(), "Sin vencimiento"),
                option(ExpiryPolicy.OPTIONAL.name(), "Vencimiento opcional"),
                option(ExpiryPolicy.REQUIRED.name(), "Vencimiento obligatorio")));
        options.put(InventoryScreenContract.MOVEMENT_TYPE, List.of(
                option(StockMovementType.RECEIPT.name(), "Entrada"),
                option(StockMovementType.ISSUE.name(), "Salida"),
                option(StockMovementType.TRANSFER.name(), "Transferencia")));
        List<ScreenInteraction.Option> warehouseOptions = warehouses.stream()
                .filter(WarehouseSnapshot::active)
                .map(warehouse -> option(
                        warehouse.id().toString(), warehouse.code() + " · " + warehouse.name()))
                .toList();
        List<ScreenInteraction.Option> locationOptions = warehouses.stream()
                .filter(WarehouseSnapshot::active)
                .flatMap(warehouse -> warehouse.locations().stream()
                        .filter(StockLocationSnapshot::active)
                        .map(location -> option(
                                location.id().toString(),
                                warehouse.code() + " · " + location.code() + " · " + location.name())))
                .toList();
        put(options, warehouseOptions,
                InventoryScreenContract.AVAILABILITY_WAREHOUSE,
                InventoryScreenContract.MOVEMENT_WAREHOUSE,
                InventoryScreenContract.MOVEMENT_TARGET_WAREHOUSE,
                InventoryScreenContract.RESERVATION_WAREHOUSE);
        put(options, locationOptions,
                InventoryScreenContract.AVAILABILITY_LOCATION,
                InventoryScreenContract.MOVEMENT_LOCATION,
                InventoryScreenContract.MOVEMENT_TARGET_LOCATION,
                InventoryScreenContract.RESERVATION_LOCATION);
        List<ScreenInteraction.Option> conditions = List.of(
                option(StockCondition.AVAILABLE.name(), "Disponible"),
                option(StockCondition.QUARANTINED.name(), "En cuarentena"),
                option(StockCondition.DAMAGED.name(), "Dañado"));
        put(options, conditions,
                InventoryScreenContract.AVAILABILITY_CONDITION,
                InventoryScreenContract.MOVEMENT_CONDITION,
                InventoryScreenContract.RESERVATION_CONDITION);
        return Map.copyOf(options);
    }

    private static void put(
            Map<ScreenElementId, List<ScreenInteraction.Option>> options,
            List<ScreenInteraction.Option> values,
            ScreenElementId... fields) {
        for (ScreenElementId field : fields) {
            options.put(field, values);
        }
    }

    private static Map<ScreenElementId, String> defaults(Map<ScreenElementId, String> submitted) {
        Map<ScreenElementId, String> inputs = InventoryScreenSupport.copy(submitted);
        inputs.putIfAbsent(InventoryScreenContract.STOCK_SEARCH_STATE, ALL);
        inputs.putIfAbsent(InventoryScreenContract.STOCK_NEW_TRACKING, TrackingMode.NONE.name());
        inputs.putIfAbsent(InventoryScreenContract.STOCK_NEW_EXPIRY, ExpiryPolicy.NONE.name());
        inputs.putIfAbsent(InventoryScreenContract.MOVEMENT_TYPE, StockMovementType.RECEIPT.name());
        inputs.putIfAbsent(InventoryScreenContract.MOVEMENT_CONDITION, StockCondition.AVAILABLE.name());
        inputs.putIfAbsent(InventoryScreenContract.AVAILABILITY_CONDITION, StockCondition.AVAILABLE.name());
        inputs.putIfAbsent(InventoryScreenContract.RESERVATION_CONDITION, StockCondition.AVAILABLE.name());
        inputs.putIfAbsent(InventoryScreenContract.MOVEMENT_SOURCE_TYPE, "MANUAL_UI");
        inputs.putIfAbsent(InventoryScreenContract.RESERVATION_SOURCE_TYPE, "MANUAL_UI");
        return inputs;
    }

    private static void applyOptionDefaults(
            Map<ScreenElementId, String> inputs,
            Map<ScreenElementId, List<ScreenInteraction.Option>> options) {
        for (ScreenElementId field : List.of(
                InventoryScreenContract.STOCK_NEW_CATALOG_ITEM,
                InventoryScreenContract.AVAILABILITY_WAREHOUSE,
                InventoryScreenContract.AVAILABILITY_LOCATION,
                InventoryScreenContract.MOVEMENT_WAREHOUSE,
                InventoryScreenContract.MOVEMENT_LOCATION,
                InventoryScreenContract.MOVEMENT_TARGET_WAREHOUSE,
                InventoryScreenContract.MOVEMENT_TARGET_LOCATION,
                InventoryScreenContract.RESERVATION_WAREHOUSE,
                InventoryScreenContract.RESERVATION_LOCATION)) {
            first(options, field).ifPresent(value -> inputs.putIfAbsent(field, value));
        }
    }

    private static Optional<Boolean> activeFilter(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return filter(inputs, field).map("ACTIVE"::equals);
    }

    private static void clearMutationInputs(
            ScreenElementId action, Map<ScreenElementId, String> inputs) {
        if (action.equals(InventoryScreenContract.POST_MOVEMENT)) {
            clear(inputs,
                    InventoryScreenContract.MOVEMENT_QUANTITY,
                    InventoryScreenContract.MOVEMENT_REASON,
                    InventoryScreenContract.MOVEMENT_SOURCE_ID,
                    InventoryScreenContract.MOVEMENT_IDEMPOTENCY);
        } else if (action.equals(InventoryScreenContract.CREATE_RESERVATION)) {
            clear(inputs,
                    InventoryScreenContract.RESERVATION_QUANTITY,
                    InventoryScreenContract.RESERVATION_EXPIRES_AT,
                    InventoryScreenContract.RESERVATION_SOURCE_ID,
                    InventoryScreenContract.RESERVATION_IDEMPOTENCY);
        } else if (action.equals(InventoryScreenContract.CONSUME_RESERVATION)
                || action.equals(InventoryScreenContract.RELEASE_RESERVATION)
                || action.equals(InventoryScreenContract.EXPIRE_RESERVATION)) {
            clear(inputs,
                    InventoryScreenContract.MANAGE_RESERVATION_ID,
                    InventoryScreenContract.MANAGE_RESERVATION_VERSION,
                    InventoryScreenContract.MANAGE_RESERVATION_QUANTITY,
                    InventoryScreenContract.MANAGE_RESERVATION_IDEMPOTENCY);
        }
    }

    private static String stateLabel(boolean active) {
        return active ? "Activo" : "Inactivo";
    }

    private static String trackingLabel(TrackingMode mode) {
        return switch (mode) {
            case NONE -> "Sin lote ni serie";
            case LOT -> "Por lote";
            case SERIAL -> "Por número de serie";
        };
    }

    private static String expiryLabel(ExpiryPolicy policy) {
        return switch (policy) {
            case NONE -> "Sin vencimiento";
            case OPTIONAL -> "Opcional";
            case REQUIRED -> "Obligatorio";
        };
    }
}
