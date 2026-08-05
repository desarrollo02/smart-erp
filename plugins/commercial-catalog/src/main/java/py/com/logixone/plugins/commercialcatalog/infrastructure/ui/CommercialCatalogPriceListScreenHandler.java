package py.com.logixone.plugins.commercialcatalog.infrastructure.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugin.api.SelectorSourceDefinition;
import py.com.logixone.plugins.commercialcatalog.CommercialCatalogPluginDefinition;
import py.com.logixone.plugins.commercialcatalog.CommercialCatalogScreenContract;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemReference;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.api.CatalogTaxMode;
import py.com.logixone.plugins.commercialcatalog.api.PriceEntryId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.application.CatalogOperationContext;
import py.com.logixone.plugins.commercialcatalog.application.CatalogOperationResult;
import py.com.logixone.plugins.commercialcatalog.application.CatalogResultCode;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogPermissions;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogUseCases;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogCommands;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchPage;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSummary;
import py.com.logixone.plugins.commercialcatalog.domain.PriceEntry;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListCode;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListName;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListSnapshot;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListState;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;
import py.com.logixone.plugins.referencedata.api.CurrencyReference;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;

/** Price-list interaction adapter; all commands remain company-scoped and audited. */
@ApplicationScoped
public class CommercialCatalogPriceListScreenHandler implements ScreenInteraction.Handler {

    private static final Logger LOGGER = System.getLogger(
            CommercialCatalogPriceListScreenHandler.class.getName());
    private static final int PAGE_SIZE = 20;
    private static final int OPTION_SIZE = 100;
    private static final String ALL = "ALL";

    @Inject
    CommercialCatalogUseCases useCases;

    @Inject
    CurrentCompanyAuthorization authorization;

    @Inject
    ReferenceDataDirectory referenceDataDirectory;

    @Override
    public ScreenId screenId() {
        return CommercialCatalogScreenContract.PRICE_LISTS;
    }

    @Override
    public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return CommercialCatalogSelectorSources.PRICE_LISTS;
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = defaults(request.inputs());
        Optional<String> selectedId = request.selectedResourceId();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();

        try {
            if (request.actionId().isPresent()) {
                ScreenElementId action = request.actionId().orElseThrow();
                if (action.equals(CommercialCatalogScreenContract.PRICE_SEARCH)) {
                    selectedId = Optional.empty();
                } else if (!action.equals(CommercialCatalogScreenContract.SELECT_PRICE_LIST)) {
                    Mutation mutation = execute(action, request, inputs);
                    selectedId = mutation.selectedResourceId();
                    notices.addAll(mutation.notices());
                    if (mutation.successful()) {
                        clearMutationInputs(action, inputs);
                    }
                }
            }
        } catch (IllegalArgumentException invalidInput) {
            LOGGER.log(Level.WARNING,
                    "event=commercial_catalog_price_screen_input_rejected action={0} input_keys={1}",
                    request.actionId().map(ScreenElementId::value).orElse("none"),
                    inputs.keySet().stream().map(ScreenElementId::value).sorted().toList());
            notices.add(error(
                    "Revisa los datos ingresados",
                    "Uno o más valores no cumplen el formato permitido."));
        }

        return load(inputs, selectedId, notices);
    }

    private Mutation execute(
            ScreenElementId action,
            ScreenInteraction.Request request,
            Map<ScreenElementId, String> inputs) {
        if (action.equals(CommercialCatalogScreenContract.REGISTER_PRICE_LIST)) {
            CatalogCommands.RegisterPriceList command = new CatalogCommands.RegisterPriceList(
                    optional(inputs, CommercialCatalogScreenContract.PRICE_NEW_CODE)
                            .map(PriceListCode::new),
                    new PriceListName(required(
                            inputs, CommercialCatalogScreenContract.PRICE_NEW_NAME)),
                    required(inputs, CommercialCatalogScreenContract.PRICE_CURRENCY),
                    enumValue(inputs, CommercialCatalogScreenContract.PRICE_TAX_MODE, CatalogTaxMode.class),
                    integer(inputs, CommercialCatalogScreenContract.PRICE_SCALE),
                    enumValue(inputs, CommercialCatalogScreenContract.PRICE_ROUNDING_MODE, RoundingMode.class));
            return mutation(useCases.registerPriceList(
                    context(CommercialCatalogPermissions.PRICES_MANAGE), command),
                    "Lista de precios registrada");
        }

        PriceListId id = PriceListId.parse(request.selectedResourceId().orElseThrow(
                () -> new IllegalArgumentException("A selected price list is required")));
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("A selected price list version is required"));

        if (action.equals(CommercialCatalogScreenContract.RENAME_PRICE_LIST)) {
            CatalogCommands.RenamePriceList command = new CatalogCommands.RenamePriceList(
                    id,
                    version,
                    new PriceListName(required(
                            inputs, CommercialCatalogScreenContract.PRICE_EDIT_NAME)));
            return mutation(useCases.renamePriceList(
                    context(CommercialCatalogPermissions.PRICES_MANAGE),
                    command),
                    "Nombre de la lista actualizado", id);
        }
        if (action.equals(CommercialCatalogScreenContract.ADD_PRICE_ENTRY)) {
            CatalogCommands.AddPriceEntry command = new CatalogCommands.AddPriceEntry(
                    id,
                    version,
                    CatalogItemId.parse(required(
                            inputs, CommercialCatalogScreenContract.PRICE_ENTRY_ITEM)),
                    new UnitCode(required(
                            inputs, CommercialCatalogScreenContract.PRICE_ENTRY_UNIT)),
                    decimal(inputs, CommercialCatalogScreenContract.PRICE_ENTRY_MINIMUM),
                    decimal(inputs, CommercialCatalogScreenContract.PRICE_ENTRY_AMOUNT),
                    instant(inputs, CommercialCatalogScreenContract.PRICE_ENTRY_VALID_FROM),
                    optional(inputs, CommercialCatalogScreenContract.PRICE_ENTRY_VALID_UNTIL)
                            .map(Instant::parse));
            return mutation(useCases.addPriceEntry(
                    context(CommercialCatalogPermissions.PRICES_MANAGE),
                    command),
                    "Precio agregado", id);
        }
        if (action.equals(CommercialCatalogScreenContract.INACTIVATE_PRICE_ENTRY)) {
            CatalogCommands.InactivatePriceEntry command = new CatalogCommands.InactivatePriceEntry(
                    id,
                    version,
                    PriceEntryId.parse(required(
                            inputs,
                            CommercialCatalogScreenContract.PRICE_ENTRY_TO_INACTIVATE)));
            return mutation(useCases.inactivatePriceEntry(
                    context(CommercialCatalogPermissions.PRICES_MANAGE),
                    command),
                    "Precio inactivado", id);
        }
        if (action.equals(CommercialCatalogScreenContract.ACTIVATE_PRICE_LIST)) {
            return lifecycle(id, version, PriceListState.ACTIVE);
        }
        if (action.equals(CommercialCatalogScreenContract.INACTIVATE_PRICE_LIST)) {
            return lifecycle(id, version, PriceListState.INACTIVE);
        }
        throw new IllegalArgumentException("Unsupported price-list screen action");
    }

    private Mutation lifecycle(PriceListId id, long version, PriceListState state) {
        return mutation(useCases.changePriceListLifecycle(
                context(CommercialCatalogPermissions.PRICES_MANAGE),
                new CatalogCommands.ChangePriceListLifecycle(id, version, state)),
                state == PriceListState.ACTIVE
                        ? "Lista de precios reactivada"
                        : "Lista de precios inactivada",
                id);
    }

    private Mutation mutation(
            CatalogOperationResult<PriceListSnapshot> result,
            String successSummary) {
        return mutation(result, successSummary, null);
    }

    private Mutation mutation(
            CatalogOperationResult<PriceListSnapshot> result,
            String successSummary,
            PriceListId fallbackId) {
        if (!result.successful()) {
            return new Mutation(
                    Optional.ofNullable(fallbackId).map(PriceListId::toString),
                    List.of(error("No se pudo completar la operación", failureMessage(result.code()))),
                    false);
        }
        PriceListSnapshot snapshot = result.value().orElseThrow();
        return new Mutation(
                Optional.of(snapshot.id().toString()),
                List.of(new ScreenInteraction.Notice(
                        ScreenInteraction.NoticeLevel.SUCCESS,
                        successSummary,
                        "El cambio fue confirmado y auditado por el servidor.")),
                true);
    }

    private ScreenInteraction.Result load(
            Map<ScreenElementId, String> inputs,
            Optional<String> selectedId,
            List<ScreenInteraction.Notice> notices) {
        String stage = "authorization";
        try {
            CatalogOperationContext viewContext = context(CommercialCatalogPermissions.VIEW);

            stage = "reference-currencies";
            List<CurrencyReference> currencies = referenceDataDirectory
                    .currencies(viewContext.companyContext().companyId()).stream()
                    .filter(CurrencyReference::enabled)
                    .toList();

            stage = "definitions";
            CatalogOperationResult<CatalogDefinitions.Snapshot> available =
                    useCases.definitions(viewContext);
            if (!available.successful()) {
                throw new IllegalStateException("Authorized catalog definitions query failed");
            }
            CatalogDefinitions.Snapshot definitions = available.value().orElseThrow();

            stage = "items";
            var itemSearch = useCases.search(viewContext, new CatalogSearchCriteria(
                    "", Set.of(), Set.of(CatalogItemState.ACTIVE), 0, OPTION_SIZE));
            if (!itemSearch.successful()) {
                throw new IllegalStateException("Authorized catalog item option query failed");
            }
            List<CatalogItemReference> items = itemSearch.value().orElseThrow().items();

            stage = "price_lists";
            PriceListSearchCriteria criteria = new PriceListSearchCriteria(
                    filter(inputs, CommercialCatalogScreenContract.PRICE_SEARCH_TEXT).orElse(""),
                    filterEnum(inputs, CommercialCatalogScreenContract.PRICE_SEARCH_STATE, PriceListState.class)
                            .map(Set::of).orElse(Set.of()),
                    0,
                    PAGE_SIZE);
            CatalogOperationResult<PriceListSearchPage> search = useCases.priceLists(viewContext, criteria);
            if (!search.successful()) {
                throw new IllegalStateException("Authorized price-list search failed");
            }

            ScreenInteraction.Table table = table(search.value().orElseThrow());
            Optional<ScreenInteraction.Detail> detail = Optional.empty();
            Optional<PriceListSnapshot> selectedSnapshot = Optional.empty();
            Optional<Long> selectedVersion = Optional.empty();
            if (selectedId.isPresent()) {
                PriceListId id = PriceListId.parse(selectedId.orElseThrow());
                CatalogOperationResult<PriceListSnapshot> found =
                        useCases.priceListDetail(viewContext, id);
                if (found.successful()) {
                    PriceListSnapshot snapshot = found.value().orElseThrow();
                    selectedSnapshot = Optional.of(snapshot);
                    populateEditableValues(inputs, snapshot);
                    detail = Optional.of(detail(snapshot, items));
                    selectedVersion = Optional.of(snapshot.version());
                } else {
                    selectedId = Optional.empty();
                    notices.add(error(
                            "Lista de precios no disponible",
                            "La lista ya no existe o dejó de estar disponible en esta empresa."));
                }
            }

            Map<ScreenElementId, List<ScreenInteraction.Option>> options =
                    options(currencies, definitions, items, selectedSnapshot);
            applyOptionDefaults(inputs, options);
            if (items.isEmpty() || options.get(
                    CommercialCatalogScreenContract.PRICE_ENTRY_UNIT).isEmpty()) {
                notices.add(new ScreenInteraction.Notice(
                        ScreenInteraction.NoticeLevel.INFO,
                        "Datos requeridos pendientes",
                        "Necesitas al menos un concepto activo y una unidad antes de agregar precios."));
            }

            return new ScreenInteraction.Result(
                    inputs,
                    options,
                    Optional.of(table),
                    detail,
                    notices,
                    selectedId,
                    selectedVersion);
        } catch (RuntimeException failure) {
            LOGGER.log(Level.ERROR,
                    "event=commercial_catalog_price_screen_load_failed stage={0} type={1}",
                    stage,
                    failure.getClass().getName());
            throw failure;
        }
    }

    private CatalogOperationContext context(ContributionId permission) {
        return CatalogOperationContext.from(authorization.require(
                CommercialCatalogPluginDefinition.ID.value(), permission.value()));
    }

    private static ScreenInteraction.Table table(PriceListSearchPage page) {
        List<ScreenInteraction.Column> columns = List.of(
                new ScreenInteraction.Column("code", "Código"),
                new ScreenInteraction.Column("name", "Nombre"),
                new ScreenInteraction.Column("currency", "Moneda"),
                new ScreenInteraction.Column("entries", "Precios"),
                new ScreenInteraction.Column("state", "Estado"));
        return new ScreenInteraction.Table(
                CommercialCatalogScreenContract.PRICE_RESULTS,
                columns,
                page.items().stream().map(CommercialCatalogPriceListScreenHandler::row).toList(),
                page.total(),
                "No encontramos listas de precios",
                "Ajusta los filtros o registra la primera política de precios de esta empresa.");
    }

    private static ScreenInteraction.Row row(PriceListSummary list) {
        return new ScreenInteraction.Row(list.id().toString(), List.of(
                list.code().value(),
                list.name().value(),
                list.currency(),
                list.activeEntries() + " activas · " + list.entries() + " total",
                stateLabel(list.state())));
    }

    private static ScreenInteraction.Detail detail(
            PriceListSnapshot snapshot, List<CatalogItemReference> items) {
        Map<CatalogItemId, String> names = new HashMap<>();
        items.forEach(item -> names.put(item.id(), item.displayName()));
        List<ScreenInteraction.DetailItem> details = new ArrayList<>();
        details.add(new ScreenInteraction.DetailItem("Código", snapshot.code().value()));
        details.add(new ScreenInteraction.DetailItem("Estado", stateLabel(snapshot.state())));
        details.add(new ScreenInteraction.DetailItem("Moneda", snapshot.currency()));
        details.add(new ScreenInteraction.DetailItem("Impuestos", taxModeLabel(snapshot.taxMode())));
        details.add(new ScreenInteraction.DetailItem("Decimales", Integer.toString(snapshot.scale())));
        details.add(new ScreenInteraction.DetailItem("Redondeo", snapshot.roundingMode().name()));
        details.add(new ScreenInteraction.DetailItem(
                "Entradas activas",
                Long.toString(snapshot.entries().stream().filter(PriceEntry::active).count())));
        details.add(new ScreenInteraction.DetailItem("Entradas totales", Integer.toString(snapshot.entries().size())));
        int index = 1;
        for (PriceEntry entry : snapshot.entries().stream().limit(20).toList()) {
            String itemName = names.getOrDefault(entry.itemId(), "Concepto no disponible");
            details.add(new ScreenInteraction.DetailItem(
                    "Precio " + index++,
                    itemName + " · " + entry.unit().value() + " · " + entry.amount().toPlainString()
                            + " · desde " + entry.validFrom()
                            + (entry.active() ? " · Activo" : " · Inactivo")));
        }
        details.add(new ScreenInteraction.DetailItem("Versión", Long.toString(snapshot.version())));
        return new ScreenInteraction.Detail(snapshot.id().toString(), snapshot.name().value(), details);
    }

    private static Map<ScreenElementId, List<ScreenInteraction.Option>> options(
            List<CurrencyReference> currencies,
            CatalogDefinitions.Snapshot definitions,
            List<CatalogItemReference> items,
            Optional<PriceListSnapshot> selected) {
        Map<ScreenElementId, List<ScreenInteraction.Option>> options = new LinkedHashMap<>();
        options.put(CommercialCatalogScreenContract.PRICE_SEARCH_STATE, List.of(
                option(ALL, "Todos los estados"),
                option(PriceListState.ACTIVE.name(), "Activo"),
                option(PriceListState.INACTIVE.name(), "Inactivo")));
        options.put(CommercialCatalogScreenContract.PRICE_CURRENCY,
                currencies.stream()
                        .map(currency -> option(
                                currency.code().value(), currency.displayName()))
                        .toList());
        options.put(CommercialCatalogScreenContract.PRICE_TAX_MODE, List.of(
                option(CatalogTaxMode.TAX_INCLUDED.name(), "Impuestos incluidos"),
                option(CatalogTaxMode.NET.name(), "Importe neto")));
        options.put(CommercialCatalogScreenContract.PRICE_SCALE, List.of(
                option("0", "0 decimales"), option("1", "1 decimal"),
                option("2", "2 decimales"), option("3", "3 decimales"),
                option("4", "4 decimales"), option("5", "5 decimales"),
                option("6", "6 decimales")));
        options.put(CommercialCatalogScreenContract.PRICE_ROUNDING_MODE, List.of(
                option(RoundingMode.HALF_UP.name(), "Mitad hacia arriba"),
                option(RoundingMode.HALF_EVEN.name(), "Mitad al par"),
                option(RoundingMode.DOWN.name(), "Hacia cero"),
                option(RoundingMode.UP.name(), "Alejado de cero")));
        options.put(CommercialCatalogScreenContract.PRICE_ENTRY_ITEM,
                items.stream().map(item -> option(
                        item.id().toString(), item.displayName() + " · " + item.code())).toList());
        options.put(CommercialCatalogScreenContract.PRICE_ENTRY_UNIT,
                definitions.units().stream()
                        .filter(unit -> unit.state() == CatalogDefinitions.State.ACTIVE)
                        .map(unit -> option(unit.code().value(), unit.displayName() + " · " + unit.code().value()))
                        .toList());
        options.put(CommercialCatalogScreenContract.PRICE_ENTRY_TO_INACTIVATE,
                selected.stream().flatMap(snapshot -> snapshot.entries().stream())
                        .filter(PriceEntry::active)
                        .map(entry -> option(
                                entry.id().toString(),
                                itemName(items, entry.itemId()) + " · " + entry.unit().value()
                                        + " · " + entry.amount().toPlainString()))
                        .toList());
        return Map.copyOf(options);
    }

    private static void applyOptionDefaults(
            Map<ScreenElementId, String> inputs,
            Map<ScreenElementId, List<ScreenInteraction.Option>> options) {
        first(options, CommercialCatalogScreenContract.PRICE_CURRENCY).ifPresent(value ->
                inputs.compute(CommercialCatalogScreenContract.PRICE_CURRENCY,
                        (field, selected) -> options.get(field).stream()
                                .anyMatch(option -> option.value().equals(selected))
                                        ? selected : value));
        first(options, CommercialCatalogScreenContract.PRICE_ENTRY_ITEM).ifPresent(value ->
                inputs.putIfAbsent(CommercialCatalogScreenContract.PRICE_ENTRY_ITEM, value));
        first(options, CommercialCatalogScreenContract.PRICE_ENTRY_UNIT).ifPresent(value ->
                inputs.putIfAbsent(CommercialCatalogScreenContract.PRICE_ENTRY_UNIT, value));
        first(options, CommercialCatalogScreenContract.PRICE_ENTRY_TO_INACTIVATE).ifPresent(value ->
                inputs.putIfAbsent(CommercialCatalogScreenContract.PRICE_ENTRY_TO_INACTIVATE, value));
    }

    private static Optional<String> first(
            Map<ScreenElementId, List<ScreenInteraction.Option>> options,
            ScreenElementId id) {
        return options.getOrDefault(id, List.of()).stream().findFirst()
                .map(ScreenInteraction.Option::value);
    }

    private static Map<ScreenElementId, String> defaults(
            Map<ScreenElementId, String> submitted) {
        Map<ScreenElementId, String> inputs = new HashMap<>(submitted);
        inputs.putIfAbsent(CommercialCatalogScreenContract.PRICE_SEARCH_STATE, ALL);
        inputs.putIfAbsent(
                CommercialCatalogScreenContract.PRICE_TAX_MODE, CatalogTaxMode.TAX_INCLUDED.name());
        inputs.putIfAbsent(CommercialCatalogScreenContract.PRICE_SCALE, "0");
        inputs.putIfAbsent(
                CommercialCatalogScreenContract.PRICE_ROUNDING_MODE, RoundingMode.HALF_UP.name());
        inputs.putIfAbsent(CommercialCatalogScreenContract.PRICE_ENTRY_MINIMUM, "1");
        return inputs;
    }

    private static void populateEditableValues(
            Map<ScreenElementId, String> inputs, PriceListSnapshot snapshot) {
        inputs.put(CommercialCatalogScreenContract.PRICE_EDIT_NAME, snapshot.name().value());
    }

    private static void clearMutationInputs(
            ScreenElementId action, Map<ScreenElementId, String> inputs) {
        if (action.equals(CommercialCatalogScreenContract.REGISTER_PRICE_LIST)) {
            clear(inputs,
                    CommercialCatalogScreenContract.PRICE_NEW_CODE,
                    CommercialCatalogScreenContract.PRICE_NEW_NAME);
        } else if (action.equals(CommercialCatalogScreenContract.ADD_PRICE_ENTRY)) {
            clear(inputs,
                    CommercialCatalogScreenContract.PRICE_ENTRY_AMOUNT,
                    CommercialCatalogScreenContract.PRICE_ENTRY_VALID_FROM,
                    CommercialCatalogScreenContract.PRICE_ENTRY_VALID_UNTIL);
        }
    }

    private static void clear(Map<ScreenElementId, String> inputs, ScreenElementId... fields) {
        for (ScreenElementId field : fields) {
            inputs.remove(field);
        }
    }

    private static String required(Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return optional(inputs, field).orElseThrow(
                () -> new IllegalArgumentException("Missing required screen value"));
    }

    private static Optional<String> optional(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return Optional.ofNullable(inputs.get(field)).map(String::strip)
                .filter(value -> !value.isEmpty());
    }

    private static Optional<String> filter(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return optional(inputs, field).filter(value -> !ALL.equals(value));
    }

    private static <E extends Enum<E>> Optional<E> filterEnum(
            Map<ScreenElementId, String> inputs, ScreenElementId field, Class<E> type) {
        return filter(inputs, field).map(value -> Enum.valueOf(type, value));
    }

    private static <E extends Enum<E>> E enumValue(
            Map<ScreenElementId, String> inputs, ScreenElementId field, Class<E> type) {
        return Enum.valueOf(type, required(inputs, field));
    }

    private static int integer(Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return Integer.parseInt(required(inputs, field));
    }

    private static BigDecimal decimal(Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return new BigDecimal(required(inputs, field));
    }

    private static Instant instant(Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return Instant.parse(required(inputs, field));
    }

    private static String itemName(List<CatalogItemReference> items, CatalogItemId itemId) {
        return items.stream().filter(item -> item.id().equals(itemId))
                .map(CatalogItemReference::displayName).findFirst().orElse("Concepto no disponible");
    }

    private static ScreenInteraction.Option option(String value, String label) {
        return new ScreenInteraction.Option(value, label);
    }

    private static String stateLabel(PriceListState state) {
        return state == PriceListState.ACTIVE ? "Activo" : "Inactivo";
    }

    private static String taxModeLabel(CatalogTaxMode mode) {
        return mode == CatalogTaxMode.TAX_INCLUDED ? "Impuestos incluidos" : "Importe neto";
    }

    private static String failureMessage(CatalogResultCode code) {
        return switch (code) {
            case VERSION_CONFLICT -> "La lista cambió desde que fue abierta. Revisa la versión actual.";
            case CODE_CONFLICT -> "El código ya está utilizado dentro de esta empresa.";
            case IDENTIFIER_CONFLICT -> "Un identificador relacionado ya está en uso.";
            case REFERENCE_CONFLICT -> "El artículo o la unidad ya no está disponible.";
            case VALIDITY_CONFLICT -> "Existe otra entrada activa con el mismo alcance y vigencia superpuesta.";
            case NOT_FOUND -> "La lista o entrada ya no está disponible.";
            case ACCESS_DENIED -> "La autorización actual no permite esta operación.";
            case INVALID_OPERATION -> "El estado actual no admite la operación solicitada.";
            case SUCCESS -> throw new IllegalArgumentException("SUCCESS is not a failure");
        };
    }

    private static ScreenInteraction.Notice error(String summary, String detail) {
        return new ScreenInteraction.Notice(ScreenInteraction.NoticeLevel.ERROR, summary, detail);
    }

    private record Mutation(
            Optional<String> selectedResourceId,
            List<ScreenInteraction.Notice> notices,
            boolean successful) {

        private Mutation {
            selectedResourceId = Optional.ofNullable(selectedResourceId.orElse(null));
            notices = List.copyOf(notices);
        }
    }
}
