package py.com.logixone.plugins.purchasing.infrastructure.ui;

import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.ALL;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.PAGE_SIZE;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.context;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.decimal;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.filter;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.filterEnum;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.mutation;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.operationKey;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.optional;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.optionalDate;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.option;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.required;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.stableId;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugin.api.SelectorSourceDefinition;
import py.com.logixone.plugins.inventory.api.InventoryStorageDirectory;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StorageSearchQuery;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.api.WarehouseReference;
import py.com.logixone.plugins.purchasing.PurchasingScreenContract;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptLineId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptState;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.application.PurchasingOperationContext;
import py.com.logixone.plugins.purchasing.application.PurchasingPermissions;
import py.com.logixone.plugins.purchasing.application.PurchasingUseCases;
import py.com.logixone.plugins.purchasing.application.command.PurchasingCommands;
import py.com.logixone.plugins.purchasing.application.query.PurchasingDirectoryQueries;
import py.com.logixone.plugins.purchasing.domain.GoodsReceipt;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;

@ApplicationScoped
public class PurchasingReceiptScreenHandler implements ScreenInteraction.Handler {
    @Inject PurchasingUseCases useCases;
    @Inject CurrentCompanyAuthorization authorization;
    @Inject InventoryStorageDirectory storage;

    @Override public ScreenId screenId() { return PurchasingScreenContract.RECEIPTS; }

    @Override public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return PurchasingSelectorSources.RECEIPTS;
    }

    @Override
    public ScreenInteraction.SelectorOptionPage searchOptions(
            ScreenInteraction.SelectorOptionRequest request) {
        PurchasingOperationContext view = context(authorization, PurchasingPermissions.VIEW);
        if (request.elementId().equals(PurchasingScreenContract.RECEIPT_ORDER)) {
            var page = useCases.searchOrders(view, new PurchasingDirectoryQueries.OrderCriteria(
                    Optional.of(request.query()).filter(value -> !value.isBlank()),
                    Optional.of(PurchaseOrderState.ISSUED), request.offset(), request.limit()))
                    .value().orElseThrow();
            return new ScreenInteraction.SelectorOptionPage(
                    page.items().stream().map(value -> option(value.id().toString(),
                            value.number() + " · " + value.supplierName())).toList(),
                    page.total(), page.offset(), page.limit());
        }
        if (request.elementId().equals(PurchasingScreenContract.RECEIPT_WAREHOUSE)) {
            var page = storage.searchWarehouses(view.companyContext().companyId(),
                    new StorageSearchQuery(
                            Optional.of(request.query()).filter(value -> !value.isBlank()),
                            true, request.offset(), request.limit()));
            return new ScreenInteraction.SelectorOptionPage(
                    page.items().stream().map(value -> option(value.id().toString(),
                            value.code() + " · " + value.name())).toList(),
                    page.total(), page.offset(), page.limit());
        }
        throw new IllegalArgumentException("Unsupported receipt selector");
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = defaults(request.inputs());
        Optional<String> selected = request.selectedResourceId();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();
        try {
            if (request.actionId().isPresent()) {
                ScreenElementId action = request.actionId().orElseThrow();
                if (action.equals(PurchasingScreenContract.RECEIPT_SEARCH)) {
                    selected = Optional.empty();
                } else if (!action.equals(PurchasingScreenContract.SELECT_RECEIPT)) {
                    var changed = execute(action, request, inputs);
                    selected = changed.selectedResourceId();
                    notices.addAll(changed.notices());
                }
            }
        } catch (IllegalArgumentException | DateTimeException failure) {
            notices.add(PurchasingScreenSupport.error(
                    "Revisa los datos ingresados",
                    "La orden, línea, cantidad o trazabilidad no es válida."));
        }
        return load(request, inputs, selected, notices);
    }

    private PurchasingScreenSupport.Mutation execute(
            ScreenElementId action, ScreenInteraction.Request request,
            Map<ScreenElementId, String> inputs) {
        if (action.equals(PurchasingScreenContract.CREATE_RECEIPT)) {
            PurchasingOperationContext operation = context(
                    authorization, PurchasingPermissions.RECEIPTS_CREATE);
            PurchaseOrderId orderId = PurchaseOrderId.parse(
                    required(inputs, PurchasingScreenContract.RECEIPT_ORDER));
            PurchaseOrder order = useCases.orderDetail(
                    context(authorization, PurchasingPermissions.VIEW), orderId)
                    .value().orElseThrow();
            PurchaseOrderLineId lineId = PurchaseOrderLineId.parse(
                    required(inputs, PurchasingScreenContract.RECEIPT_ORDER_LINE));
            PurchaseOrder.LineSnapshot orderLine = order.lines().stream()
                    .filter(line -> line.id().equals(lineId)).findFirst().orElseThrow();
            String key = operationKey(operation, action,
                    required(inputs, PurchasingScreenContract.RECEIPT_NUMBER),
                    orderId.toString(), lineId.toString());
            boolean stock = orderLine.item().kind() == PurchaseLineKind.STOCK;
            var line = new PurchasingCommands.ReceiptLineInput(
                    new GoodsReceiptLineId(stableId(key, "line")), lineId,
                    decimal(inputs, PurchasingScreenContract.RECEIPT_QUANTITY),
                    stock ? Optional.of(WarehouseId.parse(required(
                            inputs, PurchasingScreenContract.RECEIPT_WAREHOUSE))) : Optional.empty(),
                    stock ? Optional.of(StockLocationId.parse(required(
                            inputs, PurchasingScreenContract.RECEIPT_LOCATION))) : Optional.empty(),
                    stock ? optional(inputs, PurchasingScreenContract.RECEIPT_LOT) : Optional.empty(),
                    stock ? optional(inputs, PurchasingScreenContract.RECEIPT_SERIAL) : Optional.empty(),
                    stock ? optionalDate(inputs, PurchasingScreenContract.RECEIPT_EXPIRY) : Optional.empty(),
                    stock ? Optional.of(StockCondition.valueOf(required(
                            inputs, PurchasingScreenContract.RECEIPT_CONDITION))) : Optional.empty());
            return mutation(useCases.createReceipt(operation,
                            new PurchasingCommands.CreateReceipt(key,
                                    required(inputs, PurchasingScreenContract.RECEIPT_NUMBER),
                                    orderId, List.of(line))),
                    "Recepción preparada", value -> value.id().toString(), Optional.empty());
        }

        if (action.equals(PurchasingScreenContract.CONFIRM_RECEIPT)) {
            GoodsReceiptId id = GoodsReceiptId.parse(request.selectedResourceId().orElseThrow());
            long version = request.selectedResourceVersion().orElseThrow();
            GoodsReceipt receipt = useCases.receiptDetail(
                    context(authorization, PurchasingPermissions.VIEW), id)
                    .value().orElseThrow();
            PurchaseOrder order = useCases.orderDetail(
                    context(authorization, PurchasingPermissions.VIEW), receipt.orderId())
                    .value().orElseThrow();
            PurchasingOperationContext operation = context(
                    authorization, PurchasingPermissions.RECEIPTS_CONFIRM);
            String key = operationKey(operation, action, id.toString(), Long.toString(version));
            return mutation(useCases.confirmReceipt(operation,
                            new PurchasingCommands.ConfirmReceipt(
                                    key, id, version, order.version())),
                    "Recepción confirmada", value -> value.id().toString(),
                    Optional.of(id.toString()));
        }
        throw new IllegalArgumentException("Unsupported receipt action");
    }

    private ScreenInteraction.Result load(
            ScreenInteraction.Request request, Map<ScreenElementId, String> inputs,
            Optional<String> selected, List<ScreenInteraction.Notice> notices) {
        PurchasingOperationContext view = context(authorization, PurchasingPermissions.VIEW);
        int offset = request.tablePage().map(ScreenInteraction.TablePageRequest::offset).orElse(0);
        int limit = request.tablePage().map(ScreenInteraction.TablePageRequest::limit)
                .orElse(PAGE_SIZE);
        var page = useCases.searchReceipts(view,
                new PurchasingDirectoryQueries.ReceiptCriteria(
                        filter(inputs, PurchasingScreenContract.RECEIPT_SEARCH_TEXT),
                        filterEnum(inputs, PurchasingScreenContract.RECEIPT_SEARCH_STATE,
                                GoodsReceiptState.class), offset, limit)).value().orElseThrow();
        Optional<ScreenInteraction.Detail> detail = Optional.empty();
        Optional<Long> version = Optional.empty();
        if (selected.isPresent()) {
            var result = useCases.receiptDetail(view, GoodsReceiptId.parse(selected.orElseThrow()));
            if (result.successful()) {
                GoodsReceipt value = result.value().orElseThrow();
                detail = Optional.of(detail(value));
                version = Optional.of(value.version());
            } else {
                selected = Optional.empty();
                notices.add(PurchasingScreenSupport.error(
                        "Recepción no disponible", "Vuelve a buscar el documento."));
            }
        }
        return new ScreenInteraction.Result(inputs, options(inputs), Optional.of(table(page)),
                detail, notices, selected, version);
    }

    private Map<ScreenElementId, List<ScreenInteraction.Option>> options(
            Map<ScreenElementId, String> inputs) {
        Map<ScreenElementId, List<ScreenInteraction.Option>> values = new LinkedHashMap<>();
        values.put(PurchasingScreenContract.RECEIPT_SEARCH_STATE, List.of(
                option(ALL, "Todos los estados"), option("DRAFT", "Borrador"),
                option("CONFIRMED", "Confirmada")));
        values.put(PurchasingScreenContract.RECEIPT_CONDITION, List.of(
                option("AVAILABLE", "Disponible"), option("QUARANTINED", "En cuarentena"),
                option("DAMAGED", "Dañado")));
        selectedOrder(inputs).ifPresent(order -> {
            values.put(PurchasingScreenContract.RECEIPT_ORDER,
                    List.of(option(order.id().toString(), order.snapshot().number())));
            values.put(PurchasingScreenContract.RECEIPT_ORDER_LINE,
                    order.lines().stream().filter(line -> line.pendingQuantity().signum() > 0)
                            .map(line -> option(line.id().toString(),
                                    line.item().description() + " · pendiente "
                                            + line.pendingQuantity().toPlainString()))
                            .toList());
        });
        selectedWarehouse(inputs).ifPresent(warehouse -> {
            values.put(PurchasingScreenContract.RECEIPT_WAREHOUSE,
                    List.of(option(warehouse.id().toString(),
                            warehouse.code() + " · " + warehouse.name())));
            values.put(PurchasingScreenContract.RECEIPT_LOCATION,
                    warehouse.locations().stream().filter(location -> location.active())
                            .map(location -> option(location.id().toString(),
                                    location.code() + " · " + location.name())).toList());
        });
        return Map.copyOf(values);
    }

    private Optional<PurchaseOrder> selectedOrder(Map<ScreenElementId, String> inputs) {
        return optional(inputs, PurchasingScreenContract.RECEIPT_ORDER).flatMap(value -> {
            var result = useCases.orderDetail(context(authorization, PurchasingPermissions.VIEW),
                    PurchaseOrderId.parse(value));
            return result.successful() ? result.value() : Optional.empty();
        });
    }

    private Optional<WarehouseReference> selectedWarehouse(
            Map<ScreenElementId, String> inputs) {
        Optional<String> selected = optional(inputs, PurchasingScreenContract.RECEIPT_WAREHOUSE);
        if (selected.isEmpty()) {
            return Optional.empty();
        }
        var view = context(authorization, PurchasingPermissions.VIEW);
        return storage.findWarehouse(
                view.companyContext().companyId(), WarehouseId.parse(selected.orElseThrow()));
    }

    private static ScreenInteraction.Table table(
            PurchasingDirectoryQueries.Page<PurchasingDirectoryQueries.ReceiptSummary> page) {
        return new ScreenInteraction.Table(PurchasingScreenContract.RECEIPT_RESULTS,
                List.of(new ScreenInteraction.Column("number", "Número"),
                        new ScreenInteraction.Column("order", "Orden"),
                        new ScreenInteraction.Column("state", "Estado"),
                        new ScreenInteraction.Column("lines", "Líneas")),
                page.items().stream().map(value -> new ScreenInteraction.Row(
                        value.id().toString(), List.of(value.number(), value.orderId().toString(),
                                value.state() == GoodsReceiptState.DRAFT ? "Borrador" : "Confirmada",
                                Long.toString(value.lineCount())))).toList(),
                page.total(), "No hay recepciones",
                "Ajusta los filtros o prepara la primera recepción.",
                Optional.of(new ScreenInteraction.TablePage(page.offset(), page.limit())));
    }

    private static ScreenInteraction.Detail detail(GoodsReceipt receipt) {
        var snapshot = receipt.snapshot();
        String lines = receipt.lines().stream().map(line -> line.orderLineId() + " · "
                        + line.quantity().toPlainString() + " · " + line.kind())
                .reduce((left, right) -> left + " | " + right).orElse("Sin líneas");
        return new ScreenInteraction.Detail(receipt.id().toString(),
                "Recepción " + snapshot.number(), List.of(
                        new ScreenInteraction.DetailItem("Orden", receipt.orderId().toString()),
                        new ScreenInteraction.DetailItem("Estado",
                                receipt.state() == GoodsReceiptState.DRAFT ? "Borrador" : "Confirmada"),
                        new ScreenInteraction.DetailItem("Líneas", lines),
                        new ScreenInteraction.DetailItem("Versión", Long.toString(receipt.version()))));
    }

    private static Map<ScreenElementId, String> defaults(Map<ScreenElementId, String> submitted) {
        Map<ScreenElementId, String> values = new HashMap<>(submitted);
        values.putIfAbsent(PurchasingScreenContract.RECEIPT_SEARCH_STATE, ALL);
        values.putIfAbsent(PurchasingScreenContract.RECEIPT_CONDITION,
                StockCondition.AVAILABLE.name());
        return values;
    }
}
