package py.com.logixone.plugins.purchasing.infrastructure.ui;

import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.ALL;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.PAGE_SIZE;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.context;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.filter;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.filterEnum;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.option;

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
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.application.PurchasingOperationContext;
import py.com.logixone.plugins.purchasing.application.PurchasingPermissions;
import py.com.logixone.plugins.purchasing.application.PurchasingUseCases;
import py.com.logixone.plugins.purchasing.application.query.PurchasingDirectoryQueries;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;

/** Read-only fulfillment view for purchase orders, receipts and returns. */
@ApplicationScoped
public class PurchasingTrackingScreenHandler implements ScreenInteraction.Handler {
    @Inject PurchasingUseCases useCases;
    @Inject CurrentCompanyAuthorization authorization;

    @Override
    public ScreenId screenId() {
        return PurchasingScreenContract.TRACKING;
    }

    @Override
    public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return PurchasingSelectorSources.TRACKING;
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = defaults(request.inputs());
        Optional<String> selected = request.selectedResourceId();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();
        if (request.actionId().filter(PurchasingScreenContract.TRACKING_SEARCH::equals).isPresent()) {
            selected = Optional.empty();
        }
        return load(request, inputs, selected, notices);
    }

    private ScreenInteraction.Result load(
            ScreenInteraction.Request request,
            Map<ScreenElementId, String> inputs,
            Optional<String> selected,
            List<ScreenInteraction.Notice> notices) {
        PurchasingOperationContext view = context(authorization, PurchasingPermissions.VIEW);
        int offset = request.tablePage().map(ScreenInteraction.TablePageRequest::offset).orElse(0);
        int limit = request.tablePage().map(ScreenInteraction.TablePageRequest::limit)
                .orElse(PAGE_SIZE);
        var page = useCases.searchOrders(view, new PurchasingDirectoryQueries.OrderCriteria(
                filter(inputs, PurchasingScreenContract.TRACKING_SEARCH_TEXT),
                filterEnum(inputs, PurchasingScreenContract.TRACKING_SEARCH_STATE,
                        PurchaseOrderState.class),
                offset, limit)).value().orElseThrow();
        Optional<ScreenInteraction.Detail> detail = Optional.empty();
        Optional<Long> version = Optional.empty();
        if (selected.isPresent()) {
            var result = useCases.orderDetail(view, PurchaseOrderId.parse(selected.orElseThrow()));
            if (result.successful()) {
                PurchaseOrder order = result.value().orElseThrow();
                detail = Optional.of(detail(order));
                version = Optional.of(order.version());
            } else {
                selected = Optional.empty();
                notices.add(PurchasingScreenSupport.error(
                        "Orden no disponible", "Vuelve a buscar el documento."));
            }
        }
        return new ScreenInteraction.Result(
                inputs, options(), Optional.of(table(page)), detail, notices, selected, version);
    }

    private static Map<ScreenElementId, List<ScreenInteraction.Option>> options() {
        Map<ScreenElementId, List<ScreenInteraction.Option>> values = new LinkedHashMap<>();
        values.put(PurchasingScreenContract.TRACKING_SEARCH_STATE, List.of(
                option(ALL, "Todos los estados"),
                option("DRAFT", "Borrador"),
                option("ISSUED", "Emitida"),
                option("CLOSED", "Cerrada"),
                option("CANCELLED", "Cancelada")));
        return Map.copyOf(values);
    }

    private static ScreenInteraction.Table table(
            PurchasingDirectoryQueries.Page<PurchasingDirectoryQueries.OrderSummary> page) {
        return new ScreenInteraction.Table(PurchasingScreenContract.TRACKING_RESULTS,
                List.of(new ScreenInteraction.Column("number", "Número"),
                        new ScreenInteraction.Column("supplier", "Proveedor"),
                        new ScreenInteraction.Column("state", "Estado"),
                        new ScreenInteraction.Column("lines", "Líneas")),
                page.items().stream().map(value -> new ScreenInteraction.Row(
                        value.id().toString(), List.of(value.number(), value.supplierName(),
                                label(value.state()), Long.toString(value.lineCount())))).toList(),
                page.total(), "No hay órdenes para seguir",
                "Ajusta los filtros o registra una orden de compra.",
                Optional.of(new ScreenInteraction.TablePage(page.offset(), page.limit())));
    }

    private static ScreenInteraction.Detail detail(PurchaseOrder order) {
        String fulfillment = order.lines().stream()
                .map(line -> line.item().description()
                        + " · pedida " + line.orderedQuantity().toPlainString()
                        + " · recibida " + line.receivedQuantity().toPlainString()
                        + " · devuelta " + line.returnedQuantity().toPlainString()
                        + " · pendiente " + line.pendingQuantity().toPlainString())
                .reduce((left, right) -> left + " | " + right)
                .orElse("Sin líneas");
        return new ScreenInteraction.Detail(order.id().toString(),
                "Seguimiento de " + order.snapshot().number(), List.of(
                        new ScreenInteraction.DetailItem(
                                "Proveedor", order.snapshot().supplier().displayName()),
                        new ScreenInteraction.DetailItem("Estado", label(order.state())),
                        new ScreenInteraction.DetailItem(
                                "Moneda", order.snapshot().currency().code().value()),
                        new ScreenInteraction.DetailItem(
                                "Total", order.orderedTotal().toPlainString()),
                        new ScreenInteraction.DetailItem("Cumplimiento", fulfillment),
                        new ScreenInteraction.DetailItem(
                                "Versión", Long.toString(order.version()))));
    }

    private static Map<ScreenElementId, String> defaults(
            Map<ScreenElementId, String> submitted) {
        Map<ScreenElementId, String> values = new HashMap<>(submitted);
        values.putIfAbsent(PurchasingScreenContract.TRACKING_SEARCH_STATE, ALL);
        return values;
    }

    private static String label(PurchaseOrderState state) {
        return switch (state) {
            case DRAFT -> "Borrador";
            case ISSUED -> "Emitida";
            case CLOSED -> "Cerrada";
            case CANCELLED -> "Cancelada";
        };
    }
}
