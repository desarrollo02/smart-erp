package py.com.logixone.plugins.commercialcatalog.infrastructure.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchPage;
import py.com.logixone.plugins.commercialcatalog.application.CatalogOperationContext;
import py.com.logixone.plugins.commercialcatalog.application.CatalogOperationResult;
import py.com.logixone.plugins.commercialcatalog.application.CatalogResultCode;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogPermissions;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogUseCases;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogCommands;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.domain.BrandId;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogClassification;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemCode;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemName;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemSnapshot;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogVariant;
import py.com.logixone.plugins.commercialcatalog.domain.CategoryId;
import py.com.logixone.plugins.commercialcatalog.domain.ItemUnitConversion;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileReference;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;
import py.com.logixone.plugins.commercialcatalog.domain.UnitPurpose;
import py.com.logixone.plugins.commercialcatalog.domain.VariantAttributeCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;
import py.com.logixone.plugins.commercialcatalog.domain.VariantValueType;

/** Item-owned interaction adapter. It returns values only, never markup or EL. */
@ApplicationScoped
public class CommercialCatalogItemScreenHandler implements ScreenInteraction.Handler {

    private static final Logger LOGGER = System.getLogger(
            CommercialCatalogItemScreenHandler.class.getName());
    private static final int PAGE_SIZE = 20;
    private static final String ALL = "ALL";
    private static final String BOTH = "BOTH";
    private static final String NONE = "NONE";

    @Inject
    CommercialCatalogUseCases useCases;

    @Inject
    CurrentCompanyAuthorization authorization;

    @Override
    public ScreenId screenId() {
        return CommercialCatalogScreenContract.ITEMS;
    }

    @Override
    public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return CommercialCatalogSelectorSources.ITEMS;
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = defaults(request.inputs());
        Optional<String> selectedId = request.selectedResourceId();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();

        try {
            if (request.actionId().isPresent()) {
                ScreenElementId action = request.actionId().orElseThrow();
                if (action.equals(CommercialCatalogScreenContract.ITEM_SEARCH)) {
                    selectedId = Optional.empty();
                } else if (action.equals(CommercialCatalogScreenContract.PREPARE_ITEM_VARIANT)) {
                    inputs.remove(CommercialCatalogScreenContract.ITEM_VARIANT_VALUES);
                } else if (!action.equals(CommercialCatalogScreenContract.SELECT_ITEM)) {
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
                    "event=commercial_catalog_item_screen_input_rejected action={0} input_keys={1}",
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
        if (action.equals(CommercialCatalogScreenContract.REGISTER_ITEM)) {
            CatalogCommands.RegisterItem command = new CatalogCommands.RegisterItem(
                    optional(inputs, CommercialCatalogScreenContract.ITEM_NEW_CODE)
                            .map(CatalogItemCode::new),
                    new CatalogItemName(required(
                            inputs, CommercialCatalogScreenContract.ITEM_NEW_NAME)),
                    optional(inputs, CommercialCatalogScreenContract.ITEM_NEW_DESCRIPTION)
                            .orElse(""),
                    enumValue(inputs, CommercialCatalogScreenContract.ITEM_NEW_TYPE, CatalogItemType.class),
                    scopes(required(inputs, CommercialCatalogScreenContract.ITEM_NEW_SCOPE)),
                    new UnitCode(required(
                            inputs, CommercialCatalogScreenContract.ITEM_NEW_BASE_UNIT)),
                    taxReference(required(
                            inputs, CommercialCatalogScreenContract.ITEM_NEW_TAX_PROFILE)));
            return mutation(useCases.registerItem(
                    context(CommercialCatalogPermissions.ITEMS_MANAGE), command),
                    "Artículo o servicio registrado");
        }

        CatalogItemId id = CatalogItemId.parse(request.selectedResourceId().orElseThrow(
                () -> new IllegalArgumentException("A selected catalog item is required")));
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("A selected catalog item version is required"));

        if (action.equals(CommercialCatalogScreenContract.REVISE_ITEM)) {
            return mutation(useCases.reviseItem(
                    context(CommercialCatalogPermissions.ITEMS_MANAGE),
                    new CatalogCommands.ReviseItem(
                            id,
                            version,
                            new CatalogItemCode(required(
                                    inputs, CommercialCatalogScreenContract.ITEM_EDIT_CODE)),
                            new CatalogItemName(required(
                                    inputs, CommercialCatalogScreenContract.ITEM_EDIT_NAME)),
                            optional(inputs, CommercialCatalogScreenContract.ITEM_EDIT_DESCRIPTION)
                                    .orElse(""),
                            scopes(required(
                                    inputs, CommercialCatalogScreenContract.ITEM_EDIT_SCOPE)))),
                    "Datos generales actualizados", id);
        }
        if (action.equals(CommercialCatalogScreenContract.ADD_IDENTIFIER)) {
            return mutation(useCases.addIdentifier(
                    context(CommercialCatalogPermissions.ITEMS_MANAGE),
                    new CatalogCommands.AddIdentifier(
                            id,
                            version,
                            required(inputs, CommercialCatalogScreenContract.IDENTIFIER_TYPE),
                            required(inputs, CommercialCatalogScreenContract.IDENTIFIER_VALUE))),
                    "Identificador agregado", id);
        }
        if (action.equals(CommercialCatalogScreenContract.CLASSIFY_ITEM)) {
            Optional<BrandId> brand = optional(inputs, CommercialCatalogScreenContract.BRAND)
                    .filter(value -> !NONE.equals(value))
                    .map(value -> new BrandId(UUID.fromString(value)));
            CatalogClassification classification = new CatalogClassification(
                    new CategoryId(UUID.fromString(required(
                            inputs, CommercialCatalogScreenContract.MAIN_CATEGORY))),
                    Set.of(),
                    brand,
                    Set.of());
            return mutation(useCases.classify(
                    context(CommercialCatalogPermissions.ITEMS_MANAGE),
                    new CatalogCommands.Classify(id, version, classification)),
                    "Clasificación actualizada", id);
        }
        if (action.equals(CommercialCatalogScreenContract.ADD_CONVERSION)) {
            Set<UnitPurpose> purposes = purposes(required(
                    inputs, CommercialCatalogScreenContract.CONVERSION_PURPOSE));
            ItemUnitConversion conversion = new ItemUnitConversion(
                    new UnitCode(required(
                            inputs, CommercialCatalogScreenContract.CONVERSION_UNIT)),
                    decimal(inputs, CommercialCatalogScreenContract.CONVERSION_FACTOR),
                    purposes,
                    purposes,
                    true);
            return mutation(useCases.addUnitConversion(
                    context(CommercialCatalogPermissions.ITEMS_MANAGE),
                    new CatalogCommands.AddUnitConversion(id, version, conversion)),
                    "Conversión agregada", id);
        }
        if (action.equals(CommercialCatalogScreenContract.ASSIGN_TAX_PROFILE)) {
            return mutation(useCases.assignTaxProfile(
                    context(CommercialCatalogPermissions.ITEMS_MANAGE),
                    new CatalogCommands.AssignTaxProfile(
                            id,
                            version,
                            taxReference(required(
                                    inputs, CommercialCatalogScreenContract.ITEM_TAX_PROFILE)))),
                    "Perfil tributario actualizado", id);
        }
        if (action.equals(CommercialCatalogScreenContract.ASSIGN_ITEM_VARIANT)) {
            VariantFamilyReference family = variantFamilyReference(required(
                    inputs, CommercialCatalogScreenContract.ITEM_VARIANT_FAMILY));
            return mutation(useCases.assignVariant(
                    context(CommercialCatalogPermissions.ITEMS_MANAGE),
                    new CatalogCommands.AssignVariant(
                            id,
                            version,
                            family.id(),
                            family.version(),
                            variantValues(required(
                                    inputs, CommercialCatalogScreenContract.ITEM_VARIANT_VALUES)))),
                    "Familia y valores de variante asignados", id);
        }
        if (action.equals(CommercialCatalogScreenContract.ACTIVATE_ITEM)) {
            return lifecycle(id, version, CatalogItemState.ACTIVE);
        }
        if (action.equals(CommercialCatalogScreenContract.INACTIVATE_ITEM)) {
            return lifecycle(id, version, CatalogItemState.INACTIVE);
        }
        throw new IllegalArgumentException("Unsupported catalog item screen action");
    }

    private Mutation lifecycle(CatalogItemId id, long version, CatalogItemState state) {
        return mutation(useCases.changeItemLifecycle(
                context(CommercialCatalogPermissions.ITEMS_MANAGE),
                new CatalogCommands.ChangeItemLifecycle(id, version, state, Optional.empty())),
                state == CatalogItemState.ACTIVE
                        ? "Artículo o servicio reactivado"
                        : "Artículo o servicio inactivado",
                id);
    }

    private Mutation mutation(
            CatalogOperationResult<CatalogItemSnapshot> result,
            String successSummary) {
        return mutation(result, successSummary, null);
    }

    private Mutation mutation(
            CatalogOperationResult<CatalogItemSnapshot> result,
            String successSummary,
            CatalogItemId fallbackId) {
        if (!result.successful()) {
            return new Mutation(
                    Optional.ofNullable(fallbackId).map(CatalogItemId::toString),
                    List.of(error("No se pudo completar la operación", failureMessage(result.code()))),
                    false);
        }
        CatalogItemSnapshot snapshot = result.value().orElseThrow();
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
            stage = "definitions";
            CatalogOperationResult<CatalogDefinitions.Snapshot> available =
                    useCases.definitions(viewContext);
            if (!available.successful()) {
                throw new IllegalStateException("Authorized catalog definitions query failed");
            }
            CatalogDefinitions.Snapshot definitions = available.value().orElseThrow();

            stage = "search";
            CatalogSearchCriteria criteria = new CatalogSearchCriteria(
                    filter(inputs, CommercialCatalogScreenContract.ITEM_SEARCH_TEXT).orElse(""),
                    filterEnum(inputs, CommercialCatalogScreenContract.ITEM_SEARCH_TYPE, CatalogItemType.class)
                            .map(Set::of).orElse(Set.of()),
                    filterEnum(inputs, CommercialCatalogScreenContract.ITEM_SEARCH_STATE, CatalogItemState.class)
                            .map(Set::of).orElse(Set.of()),
                    0,
                    PAGE_SIZE);
            CatalogOperationResult<CatalogSearchPage> search = useCases.search(viewContext, criteria);
            if (!search.successful()) {
                throw new IllegalStateException("Authorized catalog item search failed");
            }

            ScreenInteraction.Table table = table(search.value().orElseThrow());
            Optional<ScreenInteraction.Detail> detail = Optional.empty();
            Optional<Long> selectedVersion = Optional.empty();
            if (selectedId.isPresent()) {
                CatalogItemId id = CatalogItemId.parse(selectedId.orElseThrow());
                CatalogOperationResult<CatalogItemSnapshot> found = useCases.detail(viewContext, id);
                if (found.successful()) {
                    CatalogItemSnapshot snapshot = found.value().orElseThrow();
                    populateEditableValues(inputs, snapshot);
                    detail = Optional.of(detail(snapshot, definitions));
                    selectedVersion = Optional.of(snapshot.version());
                } else {
                    selectedId = Optional.empty();
                    notices.add(error(
                            "Artículo o servicio no disponible",
                            "El registro ya no existe o dejó de estar disponible en esta empresa."));
                }
            }

            Map<ScreenElementId, List<ScreenInteraction.Option>> options = options(definitions);
            normalizeVariantFamilySelection(inputs, options);
            applyDefinitionDefaults(inputs, options);
            populateVariantStructure(inputs, definitions);
            if (options.get(CommercialCatalogScreenContract.ITEM_NEW_BASE_UNIT).isEmpty()
                    || options.get(CommercialCatalogScreenContract.ITEM_NEW_TAX_PROFILE).isEmpty()) {
                notices.add(new ScreenInteraction.Notice(
                        ScreenInteraction.NoticeLevel.INFO,
                        "Configuración inicial pendiente",
                        "Registra al menos una unidad y un perfil tributario antes de crear conceptos."));
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
                    "event=commercial_catalog_item_screen_load_failed stage={0} type={1}",
                    stage,
                    failure.getClass().getName());
            throw failure;
        }
    }

    private CatalogOperationContext context(ContributionId permission) {
        return CatalogOperationContext.from(authorization.require(
                CommercialCatalogPluginDefinition.ID.value(), permission.value()));
    }

    private static ScreenInteraction.Table table(CatalogSearchPage page) {
        List<ScreenInteraction.Column> columns = List.of(
                new ScreenInteraction.Column("code", "Código"),
                new ScreenInteraction.Column("name", "Nombre"),
                new ScreenInteraction.Column("type", "Tipo"),
                new ScreenInteraction.Column("unit", "Unidad"),
                new ScreenInteraction.Column("state", "Estado"));
        return new ScreenInteraction.Table(
                CommercialCatalogScreenContract.ITEM_RESULTS,
                columns,
                page.items().stream().map(CommercialCatalogItemScreenHandler::row).toList(),
                page.total(),
                "No encontramos artículos ni servicios",
                "Ajusta los filtros o registra el primer concepto comercial de esta empresa.");
    }

    private static ScreenInteraction.Row row(CatalogItemReference item) {
        return new ScreenInteraction.Row(item.id().toString(), List.of(
                item.code(),
                item.displayName(),
                typeLabel(item.type()),
                item.baseUnitCode(),
                stateLabel(item.state())));
    }

    private static ScreenInteraction.Detail detail(
            CatalogItemSnapshot snapshot, CatalogDefinitions.Snapshot definitions) {
        String classification = snapshot.classification()
                .map(value -> categoryName(definitions, value.mainCategory()))
                .orElse("Sin clasificación");
        String taxProfile = taxName(definitions, snapshot.taxProfile());
        return new ScreenInteraction.Detail(
                snapshot.id().toString(),
                snapshot.name().value(),
                List.of(
                        new ScreenInteraction.DetailItem("Código", snapshot.code().value()),
                        new ScreenInteraction.DetailItem("Tipo", typeLabel(snapshot.type())),
                        new ScreenInteraction.DetailItem("Estado", stateLabel(snapshot.state())),
                        new ScreenInteraction.DetailItem("Alcance", scopesLabel(snapshot.scopes())),
                        new ScreenInteraction.DetailItem("Unidad base", snapshot.baseUnit().value()),
                        new ScreenInteraction.DetailItem("Perfil tributario", taxProfile),
                        new ScreenInteraction.DetailItem("Clasificación", classification),
                        new ScreenInteraction.DetailItem(
                                "Identificadores", Integer.toString(snapshot.identifiers().size())),
                        new ScreenInteraction.DetailItem(
                                "Conversiones", Integer.toString(snapshot.conversions().size())),
                        new ScreenInteraction.DetailItem(
                                "Variante", variantLabel(snapshot.variant(), definitions)),
                        new ScreenInteraction.DetailItem("Versión", Long.toString(snapshot.version()))));
    }

    private static Map<ScreenElementId, List<ScreenInteraction.Option>> options(
            CatalogDefinitions.Snapshot definitions) {
        Map<ScreenElementId, List<ScreenInteraction.Option>> options = new LinkedHashMap<>();
        options.put(CommercialCatalogScreenContract.ITEM_SEARCH_TYPE, List.of(
                option(ALL, "Todos los tipos"),
                option(CatalogItemType.PRODUCT.name(), "Producto"),
                option(CatalogItemType.SERVICE.name(), "Servicio")));
        options.put(CommercialCatalogScreenContract.ITEM_SEARCH_STATE, List.of(
                option(ALL, "Todos los estados"),
                option(CatalogItemState.ACTIVE.name(), "Activo"),
                option(CatalogItemState.INACTIVE.name(), "Inactivo")));
        options.put(CommercialCatalogScreenContract.ITEM_NEW_TYPE, List.of(
                option(CatalogItemType.PRODUCT.name(), "Producto"),
                option(CatalogItemType.SERVICE.name(), "Servicio")));
        List<ScreenInteraction.Option> scopes = List.of(
                option(BOTH, "Compra y venta"),
                option(CatalogItemScope.PURCHASE.name(), "Compra"),
                option(CatalogItemScope.SALE.name(), "Venta"));
        options.put(CommercialCatalogScreenContract.ITEM_NEW_SCOPE, scopes);
        options.put(CommercialCatalogScreenContract.ITEM_EDIT_SCOPE, scopes);
        options.put(CommercialCatalogScreenContract.CONVERSION_PURPOSE, scopes);

        List<ScreenInteraction.Option> units = definitions.units().stream()
                .filter(unit -> unit.state() == CatalogDefinitions.State.ACTIVE)
                .map(unit -> option(unit.code().value(), unit.displayName() + " · " + unit.code().value()))
                .toList();
        options.put(CommercialCatalogScreenContract.ITEM_NEW_BASE_UNIT, units);
        options.put(CommercialCatalogScreenContract.CONVERSION_UNIT, units);

        List<ScreenInteraction.Option> taxProfiles = definitions.taxProfiles().stream()
                .filter(profile -> profile.state() == CatalogDefinitions.State.ACTIVE)
                .map(profile -> option(
                        profile.id().value() + "|" + profile.version(),
                        profile.displayName()))
                .toList();
        options.put(CommercialCatalogScreenContract.ITEM_NEW_TAX_PROFILE, taxProfiles);
        options.put(CommercialCatalogScreenContract.ITEM_TAX_PROFILE, taxProfiles);

        options.put(CommercialCatalogScreenContract.ITEM_VARIANT_FAMILY,
                definitions.variantFamilies().stream()
                        .filter(family -> family.state() == CatalogDefinitions.State.ACTIVE)
                        .map(family -> option(
                                family.id().value() + "|" + family.version(),
                                family.displayName() + " · " + family.code()))
                        .toList());

        options.put(CommercialCatalogScreenContract.MAIN_CATEGORY,
                definitions.categories().stream()
                        .filter(category -> category.state() == CatalogDefinitions.State.ACTIVE)
                        .map(category -> option(category.id().value().toString(), category.displayName()))
                        .toList());
        List<ScreenInteraction.Option> brands = new ArrayList<>();
        brands.add(option(NONE, "Sin marca"));
        definitions.brands().stream()
                .filter(brand -> brand.state() == CatalogDefinitions.State.ACTIVE)
                .map(brand -> option(brand.id().value().toString(), brand.displayName()))
                .forEach(brands::add);
        options.put(CommercialCatalogScreenContract.BRAND, List.copyOf(brands));
        return Map.copyOf(options);
    }

    private static void applyDefinitionDefaults(
            Map<ScreenElementId, String> inputs,
            Map<ScreenElementId, List<ScreenInteraction.Option>> options) {
        first(options, CommercialCatalogScreenContract.ITEM_NEW_BASE_UNIT)
                .ifPresent(value -> inputs.putIfAbsent(
                        CommercialCatalogScreenContract.ITEM_NEW_BASE_UNIT, value));
        first(options, CommercialCatalogScreenContract.ITEM_NEW_TAX_PROFILE)
                .ifPresent(value -> inputs.putIfAbsent(
                        CommercialCatalogScreenContract.ITEM_NEW_TAX_PROFILE, value));
        first(options, CommercialCatalogScreenContract.ITEM_TAX_PROFILE)
                .ifPresent(value -> inputs.putIfAbsent(
                        CommercialCatalogScreenContract.ITEM_TAX_PROFILE, value));
        first(options, CommercialCatalogScreenContract.MAIN_CATEGORY)
                .ifPresent(value -> inputs.putIfAbsent(
                        CommercialCatalogScreenContract.MAIN_CATEGORY, value));
        first(options, CommercialCatalogScreenContract.ITEM_VARIANT_FAMILY)
                .ifPresent(value -> inputs.putIfAbsent(
                        CommercialCatalogScreenContract.ITEM_VARIANT_FAMILY, value));
        inputs.putIfAbsent(CommercialCatalogScreenContract.BRAND, NONE);
    }

    private static void normalizeVariantFamilySelection(
            Map<ScreenElementId, String> inputs,
            Map<ScreenElementId, List<ScreenInteraction.Option>> options) {
        Optional<String> selected = optional(
                inputs, CommercialCatalogScreenContract.ITEM_VARIANT_FAMILY);
        if (selected.isPresent() && options.getOrDefault(
                        CommercialCatalogScreenContract.ITEM_VARIANT_FAMILY, List.of()).stream()
                .noneMatch(option -> option.value().equals(selected.orElseThrow()))) {
            inputs.remove(CommercialCatalogScreenContract.ITEM_VARIANT_FAMILY);
            inputs.remove(CommercialCatalogScreenContract.ITEM_VARIANT_STRUCTURE);
        }
    }

    private static Optional<String> first(
            Map<ScreenElementId, List<ScreenInteraction.Option>> options,
            ScreenElementId id) {
        return options.getOrDefault(id, List.of()).stream()
                .findFirst().map(ScreenInteraction.Option::value);
    }

    private static Map<ScreenElementId, String> defaults(
            Map<ScreenElementId, String> submitted) {
        Map<ScreenElementId, String> inputs = new HashMap<>(submitted);
        inputs.putIfAbsent(CommercialCatalogScreenContract.ITEM_SEARCH_TYPE, ALL);
        inputs.putIfAbsent(CommercialCatalogScreenContract.ITEM_SEARCH_STATE, ALL);
        inputs.putIfAbsent(
                CommercialCatalogScreenContract.ITEM_NEW_TYPE, CatalogItemType.PRODUCT.name());
        inputs.putIfAbsent(CommercialCatalogScreenContract.ITEM_NEW_SCOPE, BOTH);
        inputs.putIfAbsent(CommercialCatalogScreenContract.ITEM_EDIT_SCOPE, BOTH);
        inputs.putIfAbsent(CommercialCatalogScreenContract.CONVERSION_PURPOSE, BOTH);
        return inputs;
    }

    private static void populateEditableValues(
            Map<ScreenElementId, String> inputs, CatalogItemSnapshot snapshot) {
        inputs.put(CommercialCatalogScreenContract.ITEM_EDIT_CODE, snapshot.code().value());
        inputs.put(CommercialCatalogScreenContract.ITEM_EDIT_NAME, snapshot.name().value());
        inputs.put(CommercialCatalogScreenContract.ITEM_EDIT_DESCRIPTION, snapshot.description());
        inputs.put(CommercialCatalogScreenContract.ITEM_EDIT_SCOPE, scopeValue(snapshot.scopes()));
        inputs.put(CommercialCatalogScreenContract.ITEM_TAX_PROFILE,
                snapshot.taxProfile().id().value() + "|" + snapshot.taxProfile().version());
        snapshot.classification().ifPresent(classification -> {
            inputs.put(CommercialCatalogScreenContract.MAIN_CATEGORY,
                    classification.mainCategory().value().toString());
            inputs.put(CommercialCatalogScreenContract.BRAND,
                    classification.brand().map(brand -> brand.value().toString()).orElse(NONE));
        });
    }

    private static void clearMutationInputs(
            ScreenElementId action, Map<ScreenElementId, String> inputs) {
        if (action.equals(CommercialCatalogScreenContract.REGISTER_ITEM)) {
            clear(inputs,
                    CommercialCatalogScreenContract.ITEM_NEW_CODE,
                    CommercialCatalogScreenContract.ITEM_NEW_NAME,
                    CommercialCatalogScreenContract.ITEM_NEW_DESCRIPTION);
        } else if (action.equals(CommercialCatalogScreenContract.ADD_IDENTIFIER)) {
            clear(inputs,
                    CommercialCatalogScreenContract.IDENTIFIER_TYPE,
                    CommercialCatalogScreenContract.IDENTIFIER_VALUE);
        } else if (action.equals(CommercialCatalogScreenContract.ADD_CONVERSION)) {
            clear(inputs, CommercialCatalogScreenContract.CONVERSION_FACTOR);
        } else if (action.equals(CommercialCatalogScreenContract.ASSIGN_ITEM_VARIANT)) {
            clear(inputs, CommercialCatalogScreenContract.ITEM_VARIANT_VALUES);
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

    private static BigDecimal decimal(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return new BigDecimal(required(inputs, field));
    }

    private static Set<CatalogItemScope> scopes(String value) {
        return switch (value) {
            case BOTH -> EnumSet.allOf(CatalogItemScope.class);
            case "PURCHASE" -> EnumSet.of(CatalogItemScope.PURCHASE);
            case "SALE" -> EnumSet.of(CatalogItemScope.SALE);
            default -> throw new IllegalArgumentException("Unknown catalog scope");
        };
    }

    private static Set<UnitPurpose> purposes(String value) {
        return switch (value) {
            case BOTH -> EnumSet.of(UnitPurpose.PURCHASE, UnitPurpose.SALE);
            case "PURCHASE" -> EnumSet.of(UnitPurpose.PURCHASE);
            case "SALE" -> EnumSet.of(UnitPurpose.SALE);
            default -> throw new IllegalArgumentException("Unknown unit purpose");
        };
    }

    private static TaxProfileReference taxReference(String value) {
        String[] parts = value.split("\\|", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid tax profile reference");
        }
        return new TaxProfileReference(
                new TaxProfileId(UUID.fromString(parts[0])), Long.parseLong(parts[1]));
    }

    private static VariantFamilyReference variantFamilyReference(String value) {
        String[] parts = value.split("\\|", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid variant family reference");
        }
        return new VariantFamilyReference(
                new VariantFamilyId(UUID.fromString(parts[0])), Long.parseLong(parts[1]));
    }

    private static Map<VariantAttributeCode, String> variantValues(String value) {
        Map<VariantAttributeCode, String> attributes = new LinkedHashMap<>();
        for (String token : value.split(";")) {
            String candidate = token.strip();
            int separator = candidate.indexOf('=');
            if (separator <= 0 || separator == candidate.length() - 1) {
                throw new IllegalArgumentException("Invalid variant attribute assignment");
            }
            VariantAttributeCode code = new VariantAttributeCode(
                    candidate.substring(0, separator));
            String attributeValue = candidate.substring(separator + 1).strip();
            if (attributeValue.isEmpty() || attributes.putIfAbsent(code, attributeValue) != null) {
                throw new IllegalArgumentException("Invalid repeated or empty variant value");
            }
        }
        return Map.copyOf(attributes);
    }

    private static void populateVariantStructure(
            Map<ScreenElementId, String> inputs, CatalogDefinitions.Snapshot definitions) {
        Optional<CatalogDefinitions.VariantFamily> selected = optional(
                        inputs, CommercialCatalogScreenContract.ITEM_VARIANT_FAMILY)
                .map(CommercialCatalogItemScreenHandler::variantFamilyReference)
                .flatMap(reference -> definitions.variantFamilies().stream()
                        .filter(family -> family.id().equals(reference.id())
                                && family.version() == reference.version()
                                && family.state() == CatalogDefinitions.State.ACTIVE)
                        .findFirst());
        if (selected.isEmpty()) {
            inputs.remove(CommercialCatalogScreenContract.ITEM_VARIANT_STRUCTURE);
            return;
        }
        String structure = selected.orElseThrow().attributes().stream()
                .map(attribute -> attribute.code().value() + " (" + attribute.displayName()
                        + ", " + variantTypeLabel(attribute.valueType()) + ", "
                        + (attribute.required() ? "obligatorio" : "opcional") + ")")
                .collect(java.util.stream.Collectors.joining("; "));
        inputs.put(CommercialCatalogScreenContract.ITEM_VARIANT_STRUCTURE, structure);
    }

    private static String variantLabel(
            Optional<CatalogVariant> assigned, CatalogDefinitions.Snapshot definitions) {
        if (assigned.isEmpty()) {
            return "Sin asignar";
        }
        CatalogVariant variant = assigned.orElseThrow();
        String family = definitions.variantFamilies().stream()
                .filter(candidate -> candidate.id().equals(variant.familyId()))
                .map(CatalogDefinitions.VariantFamily::displayName)
                .findFirst().orElse("Familia histórica");
        String values = variant.attributes().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey().value() + "=" + entry.getValue().value())
                .collect(java.util.stream.Collectors.joining("; "));
        return family + " · revisión " + variant.familyVersion() + " · " + values;
    }

    private static String variantTypeLabel(VariantValueType type) {
        return switch (type) {
            case TEXT -> "texto";
            case NUMBER -> "número";
            case BOOLEAN -> "Sí/No (true/false)";
        };
    }

    private static String categoryName(
            CatalogDefinitions.Snapshot definitions, CategoryId id) {
        return definitions.categories().stream().filter(category -> category.id().equals(id))
                .map(CatalogDefinitions.Category::displayName).findFirst().orElse("Categoría no disponible");
    }

    private static String taxName(
            CatalogDefinitions.Snapshot definitions, TaxProfileReference reference) {
        return definitions.taxProfiles().stream()
                .filter(profile -> profile.id().equals(reference.id())
                        && profile.version() == reference.version())
                .map(CatalogDefinitions.TaxProfile::displayName)
                .findFirst().orElse("Perfil no disponible");
    }

    private static ScreenInteraction.Option option(String value, String label) {
        return new ScreenInteraction.Option(value, label);
    }

    private static String typeLabel(CatalogItemType type) {
        return type == CatalogItemType.PRODUCT ? "Producto" : "Servicio";
    }

    private static String stateLabel(CatalogItemState state) {
        return state == CatalogItemState.ACTIVE ? "Activo" : "Inactivo";
    }

    private static String scopeValue(Set<CatalogItemScope> scopes) {
        return scopes.size() == 2 ? BOTH : scopes.iterator().next().name();
    }

    private static String scopesLabel(Set<CatalogItemScope> scopes) {
        return switch (scopeValue(scopes)) {
            case BOTH -> "Compra y venta";
            case "PURCHASE" -> "Compra";
            case "SALE" -> "Venta";
            default -> throw new IllegalStateException("Unknown catalog scope");
        };
    }

    private static String failureMessage(CatalogResultCode code) {
        return switch (code) {
            case VERSION_CONFLICT -> "El registro cambió desde que fue abierto. Revisa la versión actual.";
            case CODE_CONFLICT -> "El código ya está utilizado dentro de esta empresa.";
            case IDENTIFIER_CONFLICT -> "El identificador ya está activo en otro concepto de esta empresa.";
            case REFERENCE_CONFLICT -> "Una unidad, categoría, marca, perfil o familia ya no está disponible.";
            case VALIDITY_CONFLICT -> "La vigencia solicitada se superpone con otra definición.";
            case NOT_FOUND -> "El artículo o servicio ya no está disponible.";
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

    private record VariantFamilyReference(VariantFamilyId id, long version) {
        private VariantFamilyReference {
            Objects.requireNonNull(id, "id");
            if (version < 0) {
                throw new IllegalArgumentException("Variant family version must not be negative");
            }
        }
    }
}
