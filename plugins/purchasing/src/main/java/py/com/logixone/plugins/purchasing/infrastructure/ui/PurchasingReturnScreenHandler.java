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
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.option;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.required;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.stableId;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
import py.com.logixone.plugins.purchasing.PurchasingScreenContract;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptLineId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptState;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnLineId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnState;
import py.com.logixone.plugins.purchasing.application.PurchasingOperationContext;
import py.com.logixone.plugins.purchasing.application.PurchasingPermissions;
import py.com.logixone.plugins.purchasing.application.PurchasingUseCases;
import py.com.logixone.plugins.purchasing.application.command.PurchasingCommands;
import py.com.logixone.plugins.purchasing.application.query.PurchasingDirectoryQueries;
import py.com.logixone.plugins.purchasing.domain.GoodsReceipt;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;
import py.com.logixone.plugins.purchasing.domain.SupplierReturn;

@ApplicationScoped
public class PurchasingReturnScreenHandler implements ScreenInteraction.Handler {
    @Inject PurchasingUseCases useCases;
    @Inject CurrentCompanyAuthorization authorization;

    @Override public ScreenId screenId() { return PurchasingScreenContract.RETURNS; }

    @Override public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return PurchasingSelectorSources.RETURNS;
    }

    @Override
    public ScreenInteraction.SelectorOptionPage searchOptions(
            ScreenInteraction.SelectorOptionRequest request) {
        PurchasingOperationContext view = context(authorization, PurchasingPermissions.VIEW);
        if (request.elementId().equals(PurchasingScreenContract.RETURN_ORDER)) {
            var page = useCases.searchOrders(view, new PurchasingDirectoryQueries.OrderCriteria(
                    Optional.of(request.query()).filter(value -> !value.isBlank()),
                    Optional.empty(), request.offset(), request.limit())).value().orElseThrow();
            var values = page.items().stream()
                    .filter(value -> value.state() == PurchaseOrderState.ISSUED
                            || value.state() == PurchaseOrderState.CLOSED)
                    .map(value -> option(value.id().toString(),
                            value.number() + " · " + value.supplierName())).toList();
            return new ScreenInteraction.SelectorOptionPage(
                    values, page.total(), page.offset(), page.limit());
        }
        if (request.elementId().equals(PurchasingScreenContract.RETURN_RECEIPT)) {
            var page = useCases.searchReceipts(view,
                    new PurchasingDirectoryQueries.ReceiptCriteria(
                            Optional.of(request.query()).filter(value -> !value.isBlank()),
                            Optional.of(GoodsReceiptState.CONFIRMED), request.offset(),
                            request.limit())).value().orElseThrow();
            return new ScreenInteraction.SelectorOptionPage(
                    page.items().stream().map(value -> option(value.id().toString(),
                            value.number())).toList(), page.total(), page.offset(), page.limit());
        }
        throw new IllegalArgumentException("Unsupported supplier return selector");
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = defaults(request.inputs());
        Optional<String> selected = request.selectedResourceId();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();
        try {
            if (request.actionId().isPresent()) {
                ScreenElementId action = request.actionId().orElseThrow();
                if (action.equals(PurchasingScreenContract.RETURN_SEARCH)) {
                    selected = Optional.empty();
                } else if (!action.equals(PurchasingScreenContract.SELECT_RETURN)) {
                    var changed = execute(action, request, inputs);
                    selected = changed.selectedResourceId();
                    notices.addAll(changed.notices());
                }
            }
        } catch (IllegalArgumentException failure) {
            notices.add(PurchasingScreenSupport.error(
                    "Revisa los datos ingresados",
                    "La orden, recepción, línea, causa o cantidad no es válida."));
        }
        return load(request, inputs, selected, notices);
    }

    private PurchasingScreenSupport.Mutation execute(
            ScreenElementId action, ScreenInteraction.Request request,
            Map<ScreenElementId, String> inputs) {
        if (action.equals(PurchasingScreenContract.CREATE_RETURN)) {
            PurchasingOperationContext operation = context(
                    authorization, PurchasingPermissions.RETURNS_CREATE);
            PurchaseOrderId orderId = PurchaseOrderId.parse(
                    required(inputs, PurchasingScreenContract.RETURN_ORDER));
            GoodsReceiptId receiptId = GoodsReceiptId.parse(
                    required(inputs, PurchasingScreenContract.RETURN_RECEIPT));
            GoodsReceipt receipt = useCases.receiptDetail(
                    context(authorization, PurchasingPermissions.VIEW), receiptId)
                    .value().orElseThrow();
            GoodsReceiptLineId receiptLineId = GoodsReceiptLineId.parse(
                    required(inputs, PurchasingScreenContract.RETURN_RECEIPT_LINE));
            var receiptLine = receipt.lines().stream()
                    .filter(line -> line.id().equals(receiptLineId)).findFirst().orElseThrow();
            if (!receipt.orderId().equals(orderId)) {
                throw new IllegalArgumentException("Receipt does not belong to order");
            }
            String key = operationKey(operation, action,
                    required(inputs, PurchasingScreenContract.RETURN_NUMBER),
                    receiptId.toString(), receiptLineId.toString());
            var line = new PurchasingCommands.ReturnLineInput(
                    new SupplierReturnLineId(stableId(key, "line")), receiptId, receiptLineId,
                    receiptLine.orderLineId(), decimal(inputs, PurchasingScreenContract.RETURN_QUANTITY));
            return mutation(useCases.createSupplierReturn(operation,
                            new PurchasingCommands.CreateSupplierReturn(
                                    key, required(inputs, PurchasingScreenContract.RETURN_NUMBER),
                                    orderId, required(inputs, PurchasingScreenContract.RETURN_REASON),
                                    List.of(line))),
                    "Devolución preparada", value -> value.id().toString(), Optional.empty());
        }

        if (action.equals(PurchasingScreenContract.CONFIRM_RETURN)) {
            SupplierReturnId id = SupplierReturnId.parse(
                    request.selectedResourceId().orElseThrow());
            long version = request.selectedResourceVersion().orElseThrow();
            SupplierReturn supplierReturn = useCases.returnDetail(
                    context(authorization, PurchasingPermissions.VIEW), id)
                    .value().orElseThrow();
            PurchaseOrder order = useCases.orderDetail(
                    context(authorization, PurchasingPermissions.VIEW), supplierReturn.orderId())
                    .value().orElseThrow();
            PurchasingOperationContext operation = context(
                    authorization, PurchasingPermissions.RETURNS_CONFIRM);
            String key = operationKey(operation, action, id.toString(), Long.toString(version));
            return mutation(useCases.confirmSupplierReturn(operation,
                            new PurchasingCommands.ConfirmSupplierReturn(
                                    key, id, version, order.version())),
                    "Devolución confirmada", value -> value.id().toString(),
                    Optional.of(id.toString()));
        }
        throw new IllegalArgumentException("Unsupported supplier return action");
    }

    private ScreenInteraction.Result load(
            ScreenInteraction.Request request, Map<ScreenElementId, String> inputs,
            Optional<String> selected, List<ScreenInteraction.Notice> notices) {
        PurchasingOperationContext view = context(authorization, PurchasingPermissions.VIEW);
        int offset = request.tablePage().map(ScreenInteraction.TablePageRequest::offset).orElse(0);
        int limit = request.tablePage().map(ScreenInteraction.TablePageRequest::limit)
                .orElse(PAGE_SIZE);
        var page = useCases.searchReturns(view, new PurchasingDirectoryQueries.ReturnCriteria(
                filter(inputs, PurchasingScreenContract.RETURN_SEARCH_TEXT),
                filterEnum(inputs, PurchasingScreenContract.RETURN_SEARCH_STATE,
                        SupplierReturnState.class), offset, limit)).value().orElseThrow();
        Optional<ScreenInteraction.Detail> detail = Optional.empty();
        Optional<Long> version = Optional.empty();
        if (selected.isPresent()) {
            var result = useCases.returnDetail(view, SupplierReturnId.parse(selected.orElseThrow()));
            if (result.successful()) {
                SupplierReturn value = result.value().orElseThrow();
                detail = Optional.of(detail(value));
                version = Optional.of(value.version());
            } else {
                selected = Optional.empty();
                notices.add(PurchasingScreenSupport.error(
                        "Devolución no disponible", "Vuelve a buscar el documento."));
            }
        }
        return new ScreenInteraction.Result(inputs, options(inputs), Optional.of(table(page)),
                detail, notices, selected, version);
    }

    private Map<ScreenElementId, List<ScreenInteraction.Option>> options(
            Map<ScreenElementId, String> inputs) {
        Map<ScreenElementId, List<ScreenInteraction.Option>> values = new LinkedHashMap<>();
        values.put(PurchasingScreenContract.RETURN_SEARCH_STATE, List.of(
                option(ALL, "Todos los estados"), option("DRAFT", "Borrador"),
                option("CONFIRMED", "Confirmada")));
        selectedOrder(inputs).ifPresent(order -> values.put(PurchasingScreenContract.RETURN_ORDER,
                List.of(option(order.id().toString(), order.snapshot().number()))));
        selectedReceipt(inputs).ifPresent(receipt -> {
            values.put(PurchasingScreenContract.RETURN_RECEIPT,
                    List.of(option(receipt.id().toString(), receipt.snapshot().number())));
            values.put(PurchasingScreenContract.RETURN_RECEIPT_LINE,
                    receipt.lines().stream().map(line -> option(line.id().toString(),
                            line.orderLineId() + " · " + line.quantity().toPlainString()))
                            .toList());
            inputs.put(PurchasingScreenContract.RETURN_ORDER, receipt.orderId().toString());
        });
        return Map.copyOf(values);
    }

    private Optional<PurchaseOrder> selectedOrder(Map<ScreenElementId, String> inputs) {
        return optional(inputs, PurchasingScreenContract.RETURN_ORDER).flatMap(value -> {
            var result = useCases.orderDetail(context(authorization, PurchasingPermissions.VIEW),
                    PurchaseOrderId.parse(value));
            return result.successful() ? result.value() : Optional.empty();
        });
    }

    private Optional<GoodsReceipt> selectedReceipt(Map<ScreenElementId, String> inputs) {
        return optional(inputs, PurchasingScreenContract.RETURN_RECEIPT).flatMap(value -> {
            var result = useCases.receiptDetail(context(authorization, PurchasingPermissions.VIEW),
                    GoodsReceiptId.parse(value));
            return result.successful() ? result.value() : Optional.empty();
        });
    }

    private static ScreenInteraction.Table table(
            PurchasingDirectoryQueries.Page<PurchasingDirectoryQueries.ReturnSummary> page) {
        return new ScreenInteraction.Table(PurchasingScreenContract.RETURN_RESULTS,
                List.of(new ScreenInteraction.Column("number", "Número"),
                        new ScreenInteraction.Column("order", "Orden"),
                        new ScreenInteraction.Column("reason", "Causa"),
                        new ScreenInteraction.Column("state", "Estado"),
                        new ScreenInteraction.Column("lines", "Líneas")),
                page.items().stream().map(value -> new ScreenInteraction.Row(
                        value.id().toString(), List.of(value.number(), value.orderId().toString(),
                                value.reason(), value.state() == SupplierReturnState.DRAFT
                                        ? "Borrador" : "Confirmada",
                                Long.toString(value.lineCount())))).toList(),
                page.total(), "No hay devoluciones",
                "Ajusta los filtros o prepara la primera devolución.",
                Optional.of(new ScreenInteraction.TablePage(page.offset(), page.limit())));
    }

    private static ScreenInteraction.Detail detail(SupplierReturn value) {
        var snapshot = value.snapshot();
        String lines = value.lines().stream().map(line -> line.receiptLineId() + " · "
                        + line.quantity().toPlainString() + " · " + line.kind())
                .reduce((left, right) -> left + " | " + right).orElse("Sin líneas");
        return new ScreenInteraction.Detail(value.id().toString(),
                "Devolución " + snapshot.number(), List.of(
                        new ScreenInteraction.DetailItem("Orden", value.orderId().toString()),
                        new ScreenInteraction.DetailItem("Causa", snapshot.reason()),
                        new ScreenInteraction.DetailItem("Estado",
                                value.state() == SupplierReturnState.DRAFT ? "Borrador" : "Confirmada"),
                        new ScreenInteraction.DetailItem("Líneas", lines),
                        new ScreenInteraction.DetailItem("Versión", Long.toString(value.version()))));
    }

    private static Map<ScreenElementId, String> defaults(Map<ScreenElementId, String> submitted) {
        Map<ScreenElementId, String> values = new HashMap<>(submitted);
        values.putIfAbsent(PurchasingScreenContract.RETURN_SEARCH_STATE, ALL);
        return values;
    }
}
