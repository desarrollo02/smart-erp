package py.com.logixone.plugins.purchasing.infrastructure.ui;

import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.ALL;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.PAGE_SIZE;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.context;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.canonicalUuid;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.exactOptionPage;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.decimal;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.enumValue;
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
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerDirectory;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerRole;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerSearchQuery;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.purchasing.PurchasingScreenContract;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestState;
import py.com.logixone.plugins.purchasing.application.PurchasingOperationContext;
import py.com.logixone.plugins.purchasing.application.PurchasingOperationResult;
import py.com.logixone.plugins.purchasing.application.PurchasingPermissions;
import py.com.logixone.plugins.purchasing.application.PurchasingUseCases;
import py.com.logixone.plugins.purchasing.application.command.PurchasingCommands;
import py.com.logixone.plugins.purchasing.application.query.PurchasingDirectoryQueries;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;
import py.com.logixone.plugins.referencedata.api.ReferenceDataQuery;

@ApplicationScoped
public class PurchasingOrderScreenHandler implements ScreenInteraction.Handler {
    @Inject PurchasingUseCases useCases;
    @Inject CurrentCompanyAuthorization authorization;
    @Inject BusinessPartnerDirectory partners;
    @Inject CatalogItemDirectory catalog;
    @Inject ReferenceDataDirectory referenceData;

    @Override public ScreenId screenId() { return PurchasingScreenContract.ORDERS; }

    @Override public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return PurchasingSelectorSources.ORDERS;
    }

    @Override
    public ScreenInteraction.SelectorOptionPage searchOptions(
            ScreenInteraction.SelectorOptionRequest request) {
        PurchasingOperationContext view = context(authorization, PurchasingPermissions.VIEW);
        var company = view.companyContext().companyId();
        if (request.elementId().equals(PurchasingScreenContract.ORDER_SUPPLIER)) {
            var exactId = canonicalUuid(request.query());
            if (exactId.isPresent()) {
                return exactOptionPage(request, partners.findById(company,
                                new BusinessPartnerId(exactId.orElseThrow()))
                        .filter(value -> value.state() == BusinessPartnerState.ACTIVE)
                        .filter(value -> value.roles().contains(BusinessPartnerRole.SUPPLIER))
                        .map(value -> option(value.id().toString(),
                                value.code() + " Â· " + value.displayName())));
            }
            var page = partners.search(company, new BusinessPartnerSearchQuery(
                    Optional.of(request.query()).filter(value -> !value.isBlank()),
                    Optional.of(BusinessPartnerRole.SUPPLIER),
                    Optional.of(BusinessPartnerState.ACTIVE), request.offset(), request.limit()));
            return new ScreenInteraction.SelectorOptionPage(
                    page.items().stream().map(value -> option(value.id().toString(),
                            value.code() + " · " + value.displayName())).toList(),
                    page.total(), page.offset(), page.limit());
        }
        if (request.elementId().equals(PurchasingScreenContract.ORDER_CURRENCY)) {
            var page = referenceData.searchCurrencies(company,
                    new ReferenceDataQuery(request.query(), request.offset(), request.limit(), true));
            return new ScreenInteraction.SelectorOptionPage(
                    page.entries().stream().map(value -> option(value.code().value(),
                            value.displayName() + " · " + value.code().value())).toList(),
                    page.total(), page.offset(), page.limit());
        }
        if (request.elementId().equals(PurchasingScreenContract.ORDER_ITEM)
                || request.elementId().equals(PurchasingScreenContract.ORDER_ADD_ITEM)) {
            var exactId = canonicalUuid(request.query());
            if (exactId.isPresent()) {
                return exactOptionPage(request, catalog.findById(company,
                                new py.com.logixone.plugins.commercialcatalog.api.CatalogItemId(
                                        exactId.orElseThrow()))
                        .filter(item -> item.state() == CatalogItemState.ACTIVE)
                        .filter(item -> item.scopes().contains(CatalogItemScope.PURCHASE))
                        .filter(item -> item.type() == CatalogItemType.PRODUCT
                                || item.type() == CatalogItemType.SERVICE)
                        .map(item -> option(item.id().toString(),
                                item.code() + " Â· " + item.displayName())));
            }
            var page = catalog.search(company, new CatalogSearchCriteria(
                    request.query(), java.util.Set.of(CatalogItemType.PRODUCT, CatalogItemType.SERVICE),
                    java.util.Set.of(CatalogItemState.ACTIVE),
                    java.util.Set.of(CatalogItemScope.PURCHASE), request.offset(), request.limit()));
            var values = page.items().stream().map(item -> option(item.id().toString(),
                    item.code() + " · " + item.displayName())).toList();
            return new ScreenInteraction.SelectorOptionPage(
                    values, page.total(), page.offset(), page.limit());
        }
        if (request.elementId().equals(PurchasingScreenContract.ORDER_REQUEST)) {
            var exactId = canonicalUuid(request.query());
            if (exactId.isPresent()) {
                return exactOptionPage(request, useCases.request(view,
                                new py.com.logixone.plugins.purchasing.api.PurchaseRequestId(
                                        exactId.orElseThrow())).value()
                        .filter(value -> value.state() == PurchaseRequestState.APPROVED)
                        .map(value -> option(value.id().toString(), value.number())));
            }
            var page = useCases.searchRequests(view,
                    new PurchasingDirectoryQueries.RequestCriteria(
                            Optional.of(request.query()).filter(value -> !value.isBlank()),
                            Optional.of(PurchaseRequestState.APPROVED), request.offset(),
                            request.limit())).value().orElseThrow();
            return new ScreenInteraction.SelectorOptionPage(
                    page.items().stream().map(value -> option(
                            value.id().toString(), value.number())).toList(),
                    page.total(), page.offset(), page.limit());
        }
        throw new IllegalArgumentException("Unsupported purchasing order selector");
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = defaults(request.inputs());
        Optional<String> selected = request.selectedResourceId();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();
        try {
            if (request.actionId().isPresent()) {
                ScreenElementId action = request.actionId().orElseThrow();
                if (action.equals(PurchasingScreenContract.ORDER_SEARCH)) {
                    selected = Optional.empty();
                } else if (!action.equals(PurchasingScreenContract.SELECT_ORDER)) {
                    var changed = execute(action, request, inputs);
                    selected = changed.selectedResourceId();
                    notices.addAll(changed.notices());
                }
            }
        } catch (IllegalArgumentException failure) {
            notices.add(PurchasingScreenSupport.error(
                    "Revisa los datos ingresados",
                    "Uno o más valores no cumplen el formato o la relación requerida."));
        }
        return load(request, inputs, selected, notices);
    }

    private PurchasingScreenSupport.Mutation execute(
            ScreenElementId action, ScreenInteraction.Request request,
            Map<ScreenElementId, String> inputs) {
        if (action.equals(PurchasingScreenContract.CREATE_ORDER)) {
            PurchasingOperationContext operation = context(
                    authorization, PurchasingPermissions.ORDERS_CREATE);
            String key = operationKey(operation, action,
                    required(inputs, PurchasingScreenContract.ORDER_NUMBER));
            var command = new PurchasingCommands.CreateOrder(
                    key, required(inputs, PurchasingScreenContract.ORDER_NUMBER),
                    BusinessPartnerId.parse(required(inputs, PurchasingScreenContract.ORDER_SUPPLIER)),
                    new CurrencyCode(required(inputs, PurchasingScreenContract.ORDER_CURRENCY)),
                    List.of(line(key, "initial", inputs,
                            PurchasingScreenContract.ORDER_KIND,
                            PurchasingScreenContract.ORDER_ITEM,
                            PurchasingScreenContract.ORDER_DESCRIPTION,
                            PurchasingScreenContract.ORDER_UNIT,
                            PurchasingScreenContract.ORDER_QUANTITY,
                            PurchasingScreenContract.ORDER_PRICE,
                            allocation(inputs))),
                    optional(inputs, PurchasingScreenContract.ORDER_JUSTIFICATION));
            return mutation(useCases.createOrder(operation, command),
                    "Orden creada", value -> value.id().toString(), Optional.empty());
        }

        PurchaseOrderId id = PurchaseOrderId.parse(request.selectedResourceId().orElseThrow());
        long version = request.selectedResourceVersion().orElseThrow();
        if (action.equals(PurchasingScreenContract.ADD_ORDER_LINE)) {
            PurchasingOperationContext operation = context(
                    authorization, PurchasingPermissions.ORDERS_CREATE);
            String key = operationKey(operation, action, id.toString(), Long.toString(version),
                    required(inputs, PurchasingScreenContract.ORDER_ADD_DESCRIPTION));
            var command = new PurchasingCommands.AddOrderLine(
                    key, id, version, line(key, "append", inputs,
                            PurchasingScreenContract.ORDER_ADD_KIND,
                            PurchasingScreenContract.ORDER_ADD_ITEM,
                            PurchasingScreenContract.ORDER_ADD_DESCRIPTION,
                            PurchasingScreenContract.ORDER_ADD_UNIT,
                            PurchasingScreenContract.ORDER_ADD_QUANTITY,
                            PurchasingScreenContract.ORDER_ADD_PRICE, List.of()));
            return mutation(useCases.addOrderLine(operation, command),
                    "Línea agregada", value -> value.id().toString(), Optional.of(id.toString()));
        }

        var permission = action.equals(PurchasingScreenContract.ISSUE_ORDER)
                ? PurchasingPermissions.ORDERS_ISSUE : PurchasingPermissions.ORDERS_CLOSE;
        PurchasingOperationContext operation = context(authorization, permission);
        String key = operationKey(operation, action, id.toString(), Long.toString(version));
        var command = new PurchasingCommands.OrderTransition(
                key, id, version, optional(inputs, PurchasingScreenContract.ORDER_REASON));
        PurchasingOperationResult<PurchaseOrder> result =
                action.equals(PurchasingScreenContract.ISSUE_ORDER)
                ? useCases.issueOrder(operation, command)
                : action.equals(PurchasingScreenContract.CANCEL_ORDER)
                        ? useCases.cancelOrder(operation, command)
                        : action.equals(PurchasingScreenContract.CLOSE_ORDER_SHORT)
                                ? useCases.closeOrderShort(operation, command)
                                : throwUnsupported();
        return mutation(result, "Estado de la orden actualizado",
                value -> value.id().toString(), Optional.of(id.toString()));
    }

    private ScreenInteraction.Result load(
            ScreenInteraction.Request request, Map<ScreenElementId, String> inputs,
            Optional<String> selected, List<ScreenInteraction.Notice> notices) {
        PurchasingOperationContext view = context(authorization, PurchasingPermissions.VIEW);
        int offset = request.tablePage().map(ScreenInteraction.TablePageRequest::offset).orElse(0);
        int limit = request.tablePage().map(ScreenInteraction.TablePageRequest::limit)
                .orElse(PAGE_SIZE);
        var page = useCases.searchOrders(view, new PurchasingDirectoryQueries.OrderCriteria(
                filter(inputs, PurchasingScreenContract.ORDER_SEARCH_TEXT),
                filterEnum(inputs, PurchasingScreenContract.ORDER_SEARCH_STATE,
                        PurchaseOrderState.class), offset, limit)).value().orElseThrow();
        Optional<ScreenInteraction.Detail> detail = Optional.empty();
        Optional<Long> version = Optional.empty();
        if (selected.isPresent()) {
            var result = useCases.orderDetail(view, PurchaseOrderId.parse(selected.orElseThrow()));
            if (result.successful()) {
                PurchaseOrder value = result.value().orElseThrow();
                detail = Optional.of(detail(value));
                version = Optional.of(value.version());
            } else {
                selected = Optional.empty();
                notices.add(PurchasingScreenSupport.error(
                        "Orden no disponible", "Vuelve a buscar el documento."));
            }
        }
        return new ScreenInteraction.Result(inputs, options(inputs), Optional.of(table(page)),
                detail, notices, selected, version);
    }

    private Map<ScreenElementId, List<ScreenInteraction.Option>> options(
            Map<ScreenElementId, String> inputs) {
        Map<ScreenElementId, List<ScreenInteraction.Option>> values = new LinkedHashMap<>();
        values.put(PurchasingScreenContract.ORDER_SEARCH_STATE, List.of(
                option(ALL, "Todos los estados"), option("DRAFT", "Borrador"),
                option("ISSUED", "Emitida"), option("CLOSED", "Cerrada"),
                option("CANCELLED", "Cancelada")));
        List<ScreenInteraction.Option> kinds = List.of(
                option("STOCK", "Producto con existencia"),
                option("NON_STOCK", "Producto sin existencia"),
                option("SERVICE", "Servicio"));
        values.put(PurchasingScreenContract.ORDER_KIND, kinds);
        values.put(PurchasingScreenContract.ORDER_ADD_KIND, kinds);
        selectedSupplier(inputs).ifPresent(value ->
                values.put(PurchasingScreenContract.ORDER_SUPPLIER, List.of(value)));
        selectedCurrency(inputs).ifPresent(value ->
                values.put(PurchasingScreenContract.ORDER_CURRENCY, List.of(value)));
        selectedItem(inputs, PurchasingScreenContract.ORDER_ITEM).ifPresent(value ->
                values.put(PurchasingScreenContract.ORDER_ITEM, List.of(value)));
        selectedItem(inputs, PurchasingScreenContract.ORDER_ADD_ITEM).ifPresent(value ->
                values.put(PurchasingScreenContract.ORDER_ADD_ITEM, List.of(value)));
        selectedRequest(inputs).ifPresent(request -> {
            values.put(PurchasingScreenContract.ORDER_REQUEST,
                    List.of(option(request.id().toString(), request.snapshot().number())));
            values.put(PurchasingScreenContract.ORDER_REQUEST_LINE,
                    request.lines().stream().map(line -> option(line.id().toString(),
                            line.item().description() + " · " + line.quantity().toPlainString()))
                            .toList());
        });
        return Map.copyOf(values);
    }

    private Optional<py.com.logixone.plugins.businesspartners.api.BusinessPartnerReference>
            supplier(Map<ScreenElementId, String> inputs) {
        return optional(inputs, PurchasingScreenContract.ORDER_SUPPLIER).flatMap(value ->
                partners.findById(context(authorization, PurchasingPermissions.VIEW)
                        .companyContext().companyId(), BusinessPartnerId.parse(value)));
    }

    private Optional<ScreenInteraction.Option> selectedSupplier(
            Map<ScreenElementId, String> inputs) {
        return supplier(inputs).map(value -> option(value.id().toString(),
                value.code() + " · " + value.displayName()));
    }

    private Optional<ScreenInteraction.Option> selectedCurrency(
            Map<ScreenElementId, String> inputs) {
        return optional(inputs, PurchasingScreenContract.ORDER_CURRENCY).flatMap(value ->
                referenceData.findCurrency(context(authorization, PurchasingPermissions.VIEW)
                        .companyContext().companyId(), new CurrencyCode(value)))
                .map(value -> option(value.code().value(), value.displayName()));
    }

    private Optional<ScreenInteraction.Option> selectedItem(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return optional(inputs, field).flatMap(value -> catalog.findById(
                        context(authorization, PurchasingPermissions.VIEW).companyContext().companyId(),
                        CatalogItemId.parse(value)))
                .map(value -> option(value.id().toString(),
                        value.code() + " · " + value.displayName()));
    }

    private Optional<py.com.logixone.plugins.purchasing.domain.PurchaseRequest> selectedRequest(
            Map<ScreenElementId, String> inputs) {
        return optional(inputs, PurchasingScreenContract.ORDER_REQUEST).flatMap(value -> {
            var result = useCases.requestDetail(
                    context(authorization, PurchasingPermissions.VIEW),
                    PurchaseRequestId.parse(value));
            return result.successful() ? result.value() : Optional.empty();
        });
    }

    private static List<PurchasingCommands.AllocationInput> allocation(
            Map<ScreenElementId, String> inputs) {
        Optional<String> request = optional(inputs, PurchasingScreenContract.ORDER_REQUEST);
        Optional<String> line = optional(inputs, PurchasingScreenContract.ORDER_REQUEST_LINE);
        Optional<String> quantity = optional(
                inputs, PurchasingScreenContract.ORDER_ALLOCATION_QUANTITY);
        if (request.isEmpty() && line.isEmpty() && quantity.isEmpty()) {
            return List.of();
        }
        if (request.isEmpty() || line.isEmpty() || quantity.isEmpty()) {
            throw new IllegalArgumentException("Incomplete request allocation");
        }
        return List.of(new PurchasingCommands.AllocationInput(
                PurchaseRequestId.parse(request.orElseThrow()),
                PurchaseRequestLineId.parse(line.orElseThrow()),
                new java.math.BigDecimal(quantity.orElseThrow())));
    }

    private static PurchasingCommands.OrderLineInput line(
            String key, String role, Map<ScreenElementId, String> inputs,
            ScreenElementId kind, ScreenElementId item, ScreenElementId description,
            ScreenElementId unit, ScreenElementId quantity, ScreenElementId price,
            List<PurchasingCommands.AllocationInput> allocations) {
        return new PurchasingCommands.OrderLineInput(
                new PurchaseOrderLineId(stableId(key, role)),
                new PurchasingCommands.ItemInput(
                        enumValue(inputs, kind, PurchaseLineKind.class),
                        optional(inputs, item).map(CatalogItemId::parse),
                        required(inputs, description), required(inputs, unit)),
                decimal(inputs, quantity), decimal(inputs, price), allocations);
    }

    private static ScreenInteraction.Table table(
            PurchasingDirectoryQueries.Page<PurchasingDirectoryQueries.OrderSummary> page) {
        return new ScreenInteraction.Table(PurchasingScreenContract.ORDER_RESULTS,
                List.of(new ScreenInteraction.Column("number", "Número"),
                        new ScreenInteraction.Column("supplier", "Proveedor"),
                        new ScreenInteraction.Column("currency", "Moneda"),
                        new ScreenInteraction.Column("state", "Estado"),
                        new ScreenInteraction.Column("lines", "Líneas")),
                page.items().stream().map(value -> new ScreenInteraction.Row(
                        value.id().toString(), List.of(value.number(), value.supplierName(),
                                value.currencyCode(), label(value.state()),
                                Long.toString(value.lineCount())))).toList(),
                page.total(), "No hay órdenes",
                "Ajusta los filtros o crea la primera orden.",
                Optional.of(new ScreenInteraction.TablePage(page.offset(), page.limit())));
    }

    private static ScreenInteraction.Detail detail(PurchaseOrder order) {
        var snapshot = order.snapshot();
        String lines = order.lines().stream().map(line -> line.item().description() + " · "
                        + line.orderedQuantity().toPlainString() + " · pendiente "
                        + line.pendingQuantity().toPlainString())
                .reduce((left, right) -> left + " | " + right).orElse("Sin líneas");
        return new ScreenInteraction.Detail(order.id().toString(),
                "Orden " + snapshot.number(), List.of(
                        new ScreenInteraction.DetailItem("Proveedor", snapshot.supplier().displayName()),
                        new ScreenInteraction.DetailItem("Moneda", snapshot.currency().code().value()),
                        new ScreenInteraction.DetailItem("Estado", label(order.state())),
                        new ScreenInteraction.DetailItem("Total", order.orderedTotal().toPlainString()),
                        new ScreenInteraction.DetailItem("Cumplimiento", lines),
                        new ScreenInteraction.DetailItem("Versión", Long.toString(order.version()))));
    }

    private static Map<ScreenElementId, String> defaults(Map<ScreenElementId, String> submitted) {
        Map<ScreenElementId, String> values = new HashMap<>(submitted);
        values.putIfAbsent(PurchasingScreenContract.ORDER_SEARCH_STATE, ALL);
        values.putIfAbsent(PurchasingScreenContract.ORDER_KIND, PurchaseLineKind.STOCK.name());
        values.putIfAbsent(PurchasingScreenContract.ORDER_ADD_KIND, PurchaseLineKind.STOCK.name());
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

    private static <T> T throwUnsupported() {
        throw new IllegalArgumentException("Unsupported purchasing order action");
    }
}
