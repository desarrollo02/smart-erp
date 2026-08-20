package py.com.logixone.plugins.purchasing.infrastructure.ui;

import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.ALL;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.PAGE_SIZE;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.context;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.canonicalUuid;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.exactOptionPage;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.date;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.decimal;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.enumValue;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.filter;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.filterEnum;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.mutation;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.operationKey;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.optional;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.optionalDecimal;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.option;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.required;
import static py.com.logixone.plugins.purchasing.infrastructure.ui.PurchasingScreenSupport.stableId;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.DateTimeException;
import java.time.LocalDate;
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
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.purchasing.PurchasingScreenContract;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestState;
import py.com.logixone.plugins.purchasing.application.PurchasingOperationContext;
import py.com.logixone.plugins.purchasing.application.PurchasingOperationResult;
import py.com.logixone.plugins.purchasing.application.PurchasingPermissions;
import py.com.logixone.plugins.purchasing.application.PurchasingUseCases;
import py.com.logixone.plugins.purchasing.application.command.PurchasingCommands;
import py.com.logixone.plugins.purchasing.application.query.PurchasingDirectoryQueries;
import py.com.logixone.plugins.purchasing.domain.PurchaseRequest;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;
import py.com.logixone.plugins.referencedata.api.ReferenceDataQuery;

@ApplicationScoped
public class PurchasingRequestScreenHandler implements ScreenInteraction.Handler {
    @Inject PurchasingUseCases useCases;
    @Inject CurrentCompanyAuthorization authorization;
    @Inject CatalogItemDirectory catalog;
    @Inject ReferenceDataDirectory referenceData;

    @Override public ScreenId screenId() { return PurchasingScreenContract.REQUESTS; }

    @Override public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return PurchasingSelectorSources.REQUESTS;
    }

    @Override
    public ScreenInteraction.SelectorOptionPage searchOptions(
            ScreenInteraction.SelectorOptionRequest request) {
        PurchasingOperationContext view = context(authorization, PurchasingPermissions.VIEW);
        if (request.elementId().equals(PurchasingScreenContract.REQUEST_ITEM)
                || request.elementId().equals(PurchasingScreenContract.REQUEST_ADD_ITEM)) {
            var exactId = canonicalUuid(request.query());
            if (exactId.isPresent()) {
                return exactOptionPage(request, catalog.findById(
                                view.companyContext().companyId(),
                                new py.com.logixone.plugins.commercialcatalog.api.CatalogItemId(
                                        exactId.orElseThrow()))
                        .filter(item -> item.state() == CatalogItemState.ACTIVE)
                        .filter(item -> item.scopes().contains(CatalogItemScope.PURCHASE))
                        .filter(item -> item.type() == CatalogItemType.PRODUCT
                                || item.type() == CatalogItemType.SERVICE)
                        .map(item -> option(item.id().toString(),
                                item.code() + " Â· " + item.displayName())));
            }
            var page = catalog.search(view.companyContext().companyId(), new CatalogSearchCriteria(
                    request.query(), java.util.Set.of(CatalogItemType.PRODUCT, CatalogItemType.SERVICE),
                    java.util.Set.of(CatalogItemState.ACTIVE),
                    java.util.Set.of(CatalogItemScope.PURCHASE), request.offset(), request.limit()));
            var items = page.items().stream().map(item -> option(item.id().toString(),
                    item.code() + " · " + item.displayName())).toList();
            return new ScreenInteraction.SelectorOptionPage(
                    items, page.total(), page.offset(), page.limit());
        }
        if (request.elementId().equals(PurchasingScreenContract.REQUEST_CURRENCY)
                || request.elementId().equals(PurchasingScreenContract.REQUEST_ADD_CURRENCY)) {
            var page = referenceData.searchCurrencies(view.companyContext().companyId(),
                    new ReferenceDataQuery(request.query(), request.offset(), request.limit(), true));
            return new ScreenInteraction.SelectorOptionPage(
                    page.entries().stream().map(currency -> option(
                            currency.code().value(), currency.displayName() + " · "
                                    + currency.code().value())).toList(),
                    page.total(), page.offset(), page.limit());
        }
        throw new IllegalArgumentException("Unsupported purchasing request selector");
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = defaults(request.inputs());
        Optional<String> selected = request.selectedResourceId();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();
        try {
            if (request.actionId().isPresent()) {
                ScreenElementId action = request.actionId().orElseThrow();
                if (action.equals(PurchasingScreenContract.REQUEST_SEARCH)) {
                    selected = Optional.empty();
                } else if (!action.equals(PurchasingScreenContract.SELECT_REQUEST)) {
                    var changed = execute(action, request, inputs);
                    selected = changed.selectedResourceId();
                    notices.addAll(changed.notices());
                }
            }
        } catch (IllegalArgumentException | DateTimeException failure) {
            notices.add(PurchasingScreenSupport.error(
                    "Revisa los datos ingresados",
                    "Uno o más valores no cumplen el formato o la relación requerida."));
        }
        return load(request, inputs, selected, notices);
    }

    private PurchasingScreenSupport.Mutation execute(
            ScreenElementId action, ScreenInteraction.Request request,
            Map<ScreenElementId, String> inputs) {
        if (action.equals(PurchasingScreenContract.CREATE_REQUEST)) {
            PurchasingOperationContext operation = context(
                    authorization, PurchasingPermissions.REQUESTS_CREATE);
            String key = operationKey(operation, action,
                    required(inputs, PurchasingScreenContract.REQUEST_NUMBER));
            var command = new PurchasingCommands.CreateRequest(
                    key, required(inputs, PurchasingScreenContract.REQUEST_NUMBER),
                    date(inputs, PurchasingScreenContract.REQUEST_DATE),
                    List.of(requestLine(key, "initial", inputs,
                            PurchasingScreenContract.REQUEST_KIND,
                            PurchasingScreenContract.REQUEST_ITEM,
                            PurchasingScreenContract.REQUEST_DESCRIPTION,
                            PurchasingScreenContract.REQUEST_UNIT,
                            PurchasingScreenContract.REQUEST_QUANTITY,
                            PurchasingScreenContract.REQUEST_EXPECTED_PRICE,
                            PurchasingScreenContract.REQUEST_CURRENCY)));
            return mutation(useCases.createRequest(operation, command),
                    "Solicitud creada", value -> value.id().toString(), Optional.empty());
        }

        PurchaseRequestId id = PurchaseRequestId.parse(
                request.selectedResourceId().orElseThrow());
        long version = request.selectedResourceVersion().orElseThrow();

        if (action.equals(PurchasingScreenContract.ADD_REQUEST_LINE)) {
            PurchasingOperationContext operation = context(
                    authorization, PurchasingPermissions.REQUESTS_CREATE);
            PurchaseRequest current = useCases.requestDetail(
                    context(authorization, PurchasingPermissions.VIEW), id)
                    .value().orElseThrow();
            String key = operationKey(operation, action, id.toString(), Long.toString(version),
                    required(inputs, PurchasingScreenContract.REQUEST_ADD_DESCRIPTION));
            List<PurchasingCommands.RequestLineInput> lines = new ArrayList<>();
            current.lines().forEach(line -> lines.add(input(line)));
            lines.add(requestLine(key, "append", inputs,
                    PurchasingScreenContract.REQUEST_ADD_KIND,
                    PurchasingScreenContract.REQUEST_ADD_ITEM,
                    PurchasingScreenContract.REQUEST_ADD_DESCRIPTION,
                    PurchasingScreenContract.REQUEST_ADD_UNIT,
                    PurchasingScreenContract.REQUEST_ADD_QUANTITY,
                    PurchasingScreenContract.REQUEST_ADD_EXPECTED_PRICE,
                    PurchasingScreenContract.REQUEST_ADD_CURRENCY));
            return mutation(useCases.replaceRequestLines(operation,
                            new PurchasingCommands.ReplaceRequestLines(key, id, version, lines)),
                    "Línea agregada", value -> value.id().toString(), Optional.of(id.toString()));
        }

        if (action.equals(PurchasingScreenContract.CLONE_REQUEST)) {
            PurchasingOperationContext operation = context(
                    authorization, PurchasingPermissions.REQUESTS_CREATE);
            PurchaseRequest current = useCases.requestDetail(
                    context(authorization, PurchasingPermissions.VIEW), id)
                    .value().orElseThrow();
            String key = operationKey(operation, action, id.toString(),
                    required(inputs, PurchasingScreenContract.REQUEST_CLONE_NUMBER));
            List<PurchaseRequestLineId> lineIds = java.util.stream.IntStream
                    .range(0, current.lines().size())
                    .mapToObj(index -> new PurchaseRequestLineId(stableId(key, "line-" + index)))
                    .toList();
            return mutation(useCases.cloneRequest(operation, new PurchasingCommands.CloneRequest(
                            key, id, required(inputs, PurchasingScreenContract.REQUEST_CLONE_NUMBER),
                            date(inputs, PurchasingScreenContract.REQUEST_CLONE_DATE), lineIds)),
                    "Solicitud clonada", value -> value.id().toString(), Optional.of(id.toString()));
        }

        var permission = action.equals(PurchasingScreenContract.SUBMIT_REQUEST)
                ? PurchasingPermissions.REQUESTS_SUBMIT
                : action.equals(PurchasingScreenContract.APPROVE_REQUEST)
                        || action.equals(PurchasingScreenContract.REJECT_REQUEST)
                        ? PurchasingPermissions.REQUESTS_APPROVE
                        : PurchasingPermissions.REQUESTS_CREATE;
        PurchasingOperationContext operation = context(authorization, permission);
        String key = operationKey(operation, action, id.toString(), Long.toString(version));
        Optional<String> reason = optional(inputs, PurchasingScreenContract.REQUEST_REASON);
        var command = new PurchasingCommands.RequestTransition(key, id, version, reason);
        PurchasingOperationResult<PurchaseRequest> result =
                action.equals(PurchasingScreenContract.SUBMIT_REQUEST)
                ? useCases.submitRequest(operation, command)
                : action.equals(PurchasingScreenContract.APPROVE_REQUEST)
                        ? useCases.approveRequest(operation, command)
                        : action.equals(PurchasingScreenContract.REJECT_REQUEST)
                                ? useCases.rejectRequest(operation, command)
                                : action.equals(PurchasingScreenContract.CANCEL_REQUEST)
                                        ? useCases.cancelRequest(operation, command)
                                        : throwUnsupported();
        return mutation(result, "Estado de la solicitud actualizado",
                value -> value.id().toString(), Optional.of(id.toString()));
    }

    private ScreenInteraction.Result load(
            ScreenInteraction.Request request, Map<ScreenElementId, String> inputs,
            Optional<String> selected, List<ScreenInteraction.Notice> notices) {
        PurchasingOperationContext view = context(authorization, PurchasingPermissions.VIEW);
        int offset = request.tablePage().map(ScreenInteraction.TablePageRequest::offset).orElse(0);
        int limit = request.tablePage().map(ScreenInteraction.TablePageRequest::limit)
                .orElse(PAGE_SIZE);
        var page = useCases.searchRequests(view, new PurchasingDirectoryQueries.RequestCriteria(
                filter(inputs, PurchasingScreenContract.REQUEST_SEARCH_TEXT),
                filterEnum(inputs, PurchasingScreenContract.REQUEST_SEARCH_STATE,
                        PurchaseRequestState.class), offset, limit)).value().orElseThrow();
        Optional<ScreenInteraction.Detail> detail = Optional.empty();
        Optional<Long> version = Optional.empty();
        Optional<PurchaseRequest> selectedRequest = Optional.empty();
        if (selected.isPresent()) {
            var result = useCases.requestDetail(view, PurchaseRequestId.parse(selected.orElseThrow()));
            if (result.successful()) {
                PurchaseRequest value = result.value().orElseThrow();
                selectedRequest = Optional.of(value);
                detail = Optional.of(detail(value));
                version = Optional.of(value.version());
                inputs.put(PurchasingScreenContract.REQUEST_SUMMARY, summary(value));
            } else {
                selected = Optional.empty();
                notices.add(PurchasingScreenSupport.error(
                        "Solicitud no disponible", "Vuelve a buscar el documento."));
            }
        }
        Optional<ScreenInteraction.Table> visibleTable = selectedRequest
                .map(PurchasingRequestScreenHandler::linesTable)
                .or(() -> Optional.of(table(page)));
        boolean requester = selectedRequest
                .map(value -> value.snapshot().requesterId().equals(
                        view.companyContext().actor().userId()))
                .orElse(false);
        return new ScreenInteraction.Result(inputs, options(inputs), visibleTable,
                detail, notices, selected, version,
                PurchasingFloorplanStates.requests(
                        selectedRequest.map(PurchaseRequest::state), requester));
    }

    private Map<ScreenElementId, List<ScreenInteraction.Option>> options(
            Map<ScreenElementId, String> inputs) {
        Map<ScreenElementId, List<ScreenInteraction.Option>> values = new LinkedHashMap<>();
        values.put(PurchasingScreenContract.REQUEST_SEARCH_STATE, List.of(
                option(ALL, "Todos los estados"), option("DRAFT", "Borrador"),
                option("SUBMITTED", "Enviada"), option("APPROVED", "Aprobada"),
                option("REJECTED", "Rechazada"), option("CANCELLED", "Cancelada")));
        List<ScreenInteraction.Option> kinds = List.of(
                option("STOCK", "Producto con existencia"),
                option("NON_STOCK", "Producto sin existencia"),
                option("SERVICE", "Servicio"));
        values.put(PurchasingScreenContract.REQUEST_KIND, kinds);
        values.put(PurchasingScreenContract.REQUEST_ADD_KIND, kinds);
        selectedItem(inputs, PurchasingScreenContract.REQUEST_ITEM)
                .ifPresent(value -> values.put(PurchasingScreenContract.REQUEST_ITEM, List.of(value)));
        selectedItem(inputs, PurchasingScreenContract.REQUEST_ADD_ITEM)
                .ifPresent(value -> values.put(PurchasingScreenContract.REQUEST_ADD_ITEM, List.of(value)));
        selectedCurrency(inputs, PurchasingScreenContract.REQUEST_CURRENCY)
                .ifPresent(value -> values.put(PurchasingScreenContract.REQUEST_CURRENCY, List.of(value)));
        selectedCurrency(inputs, PurchasingScreenContract.REQUEST_ADD_CURRENCY)
                .ifPresent(value -> values.put(PurchasingScreenContract.REQUEST_ADD_CURRENCY, List.of(value)));
        return Map.copyOf(values);
    }

    private Optional<ScreenInteraction.Option> selectedItem(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return optional(inputs, field).flatMap(value -> catalog.findById(
                        context(authorization, PurchasingPermissions.VIEW).companyContext().companyId(),
                        CatalogItemId.parse(value)))
                .map(item -> option(item.id().toString(),
                        item.code() + " · " + item.displayName()));
    }

    private Optional<ScreenInteraction.Option> selectedCurrency(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return optional(inputs, field).flatMap(value -> referenceData.findCurrency(
                        context(authorization, PurchasingPermissions.VIEW).companyContext().companyId(),
                        new CurrencyCode(value)))
                .map(currency -> option(currency.code().value(), currency.displayName()));
    }

    private static PurchasingCommands.RequestLineInput requestLine(
            String key, String role, Map<ScreenElementId, String> inputs,
            ScreenElementId kind, ScreenElementId item, ScreenElementId description,
            ScreenElementId unit, ScreenElementId quantity, ScreenElementId price,
            ScreenElementId currency) {
        Optional<java.math.BigDecimal> amount = optionalDecimal(inputs, price);
        Optional<PurchasingCommands.ExpectedPriceInput> expected = amount.map(value ->
                new PurchasingCommands.ExpectedPriceInput(value,
                        new CurrencyCode(required(inputs, currency))));
        return new PurchasingCommands.RequestLineInput(
                new PurchaseRequestLineId(stableId(key, role)),
                new PurchasingCommands.ItemInput(
                        enumValue(inputs, kind, PurchaseLineKind.class),
                        optional(inputs, item).map(CatalogItemId::parse),
                        required(inputs, description), required(inputs, unit)),
                decimal(inputs, quantity), expected);
    }

    private static PurchasingCommands.RequestLineInput input(PurchaseRequest.Line line) {
        return new PurchasingCommands.RequestLineInput(
                line.id(), new PurchasingCommands.ItemInput(
                        line.item().kind(), line.item().catalogItemId(),
                        line.item().description(), line.item().presentedUnitCode()),
                line.quantity(), line.expectedPrice().map(price ->
                        new PurchasingCommands.ExpectedPriceInput(
                                price.amount(), price.currency().code())));
    }

    private static ScreenInteraction.Table table(
            PurchasingDirectoryQueries.Page<PurchasingDirectoryQueries.RequestSummary> page) {
        return new ScreenInteraction.Table(PurchasingScreenContract.REQUEST_RESULTS,
                List.of(new ScreenInteraction.Column("number", "Número"),
                        new ScreenInteraction.Column("date", "Fecha"),
                        new ScreenInteraction.Column("state", "Estado"),
                        new ScreenInteraction.Column("lines", "Líneas")),
                page.items().stream().map(value -> new ScreenInteraction.Row(
                        value.id().toString(), List.of(value.number(), value.requestedOn().toString(),
                                label(value.state()), Long.toString(value.lineCount())))).toList(),
                page.total(), "No hay solicitudes",
                "Ajusta los filtros o crea la primera solicitud.",
                Optional.of(new ScreenInteraction.TablePage(page.offset(), page.limit())));
    }

    private static ScreenInteraction.Table linesTable(PurchaseRequest request) {
        return new ScreenInteraction.Table(PurchasingScreenContract.REQUEST_LINES,
                List.of(new ScreenInteraction.Column("description", "Descripción"),
                        new ScreenInteraction.Column("kind", "Tipo"),
                        new ScreenInteraction.Column("quantity", "Cantidad"),
                        new ScreenInteraction.Column("unit", "Unidad"),
                        new ScreenInteraction.Column("expected", "Precio esperado")),
                request.lines().stream().map(line -> new ScreenInteraction.Row(
                        line.id().toString(), List.of(
                                line.item().description(),
                                line.item().kind().name(),
                                line.quantity().toPlainString(),
                                line.item().presentedUnitCode(),
                                line.expectedPrice().map(price -> price.amount().toPlainString()
                                                + " " + price.currency().code().value())
                                        .orElse("Sin estimación"))))
                        .toList(),
                request.lines().size(), "Solicitud sin líneas",
                "Agrega al menos una línea antes de enviar." );
    }

    private static String summary(PurchaseRequest request) {
        return request.snapshot().number() + " · " + label(request.state())
                + " · " + request.lines().size() + " línea(s) · solicitante original conservado";
    }

    private static ScreenInteraction.Detail detail(PurchaseRequest request) {
        var snapshot = request.snapshot();
        String lines = request.lines().stream().map(line -> line.item().description() + " · "
                        + line.quantity().toPlainString() + " " + line.item().presentedUnitCode())
                .reduce((left, right) -> left + " | " + right).orElse("Sin líneas");
        return new ScreenInteraction.Detail(request.id().toString(),
                "Solicitud " + snapshot.number(), List.of(
                        new ScreenInteraction.DetailItem("Estado", label(request.state())),
                        new ScreenInteraction.DetailItem("Fecha solicitada", snapshot.requestedOn().toString()),
                        new ScreenInteraction.DetailItem("Solicitante", snapshot.requesterId().toString()),
                        new ScreenInteraction.DetailItem("Líneas", lines),
                        new ScreenInteraction.DetailItem("Versión", Long.toString(request.version()))));
    }

    private static Map<ScreenElementId, String> defaults(Map<ScreenElementId, String> submitted) {
        Map<ScreenElementId, String> values = new HashMap<>(submitted);
        values.putIfAbsent(PurchasingScreenContract.REQUEST_SEARCH_STATE, ALL);
        values.putIfAbsent(PurchasingScreenContract.REQUEST_DATE, LocalDate.now().toString());
        values.putIfAbsent(PurchasingScreenContract.REQUEST_CLONE_DATE, LocalDate.now().toString());
        values.putIfAbsent(PurchasingScreenContract.REQUEST_KIND, PurchaseLineKind.STOCK.name());
        values.putIfAbsent(PurchasingScreenContract.REQUEST_ADD_KIND, PurchaseLineKind.STOCK.name());
        return values;
    }

    private static String label(PurchaseRequestState state) {
        return switch (state) {
            case DRAFT -> "Borrador";
            case SUBMITTED -> "Enviada";
            case APPROVED -> "Aprobada";
            case REJECTED -> "Rechazada";
            case CANCELLED -> "Cancelada";
        };
    }

    private static <T> T throwUnsupported() {
        throw new IllegalArgumentException("Unsupported purchasing request action");
    }
}
