package py.com.logixone.plugins.commercialcatalog.infrastructure.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugin.api.SelectorSourceDefinition;
import py.com.logixone.plugins.commercialcatalog.CommercialCatalogPluginDefinition;
import py.com.logixone.plugins.commercialcatalog.CommercialCatalogScreenContract;
import py.com.logixone.plugins.commercialcatalog.application.CatalogOperationContext;
import py.com.logixone.plugins.commercialcatalog.application.CatalogOperationResult;
import py.com.logixone.plugins.commercialcatalog.application.CatalogResultCode;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogPermissions;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogUseCases;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogDefinitionCommands;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.domain.CategoryId;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;

/** Authorized administration of company-owned unit, category, brand and tag definitions. */
@ApplicationScoped
public class CommercialCatalogDefinitionScreenHandler implements ScreenInteraction.Handler {

    private static final Logger LOGGER = System.getLogger(
            CommercialCatalogDefinitionScreenHandler.class.getName());
    private static final String ALL = "ALL";
    private static final String NONE = "NONE";

    @Inject
    CommercialCatalogUseCases useCases;

    @Inject
    CurrentCompanyAuthorization authorization;

    @Override
    public ScreenId screenId() {
        return CommercialCatalogScreenContract.DEFINITIONS;
    }

    @Override
    public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return CommercialCatalogSelectorSources.DEFINITIONS;
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = new LinkedHashMap<>(request.inputs());
        Optional<String> selectedId = request.selectedResourceId();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();

        try {
            if (request.actionId().filter(
                    CommercialCatalogScreenContract.REGISTER_DEFINITION::equals).isPresent()) {
                Registration registration = register(inputs);
                if (registration.successful()) {
                    selectedId = registration.resourceId();
                    clearCreateInputs(inputs);
                    notices.add(new ScreenInteraction.Notice(
                            ScreenInteraction.NoticeLevel.SUCCESS,
                            "Definición registrada",
                            "El valor quedó disponible y auditado dentro de la empresa activa."));
                } else {
                    selectedId = Optional.empty();
                    notices.add(error(
                            "No se pudo registrar la definición",
                            failureMessage(registration.code())));
                }
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.REVISE_DEFINITION::equals).isPresent()) {
                selectedId = revise(request, inputs, notices);
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.REPLACE_DEFINITION::equals).isPresent()) {
                selectedId = replace(request, inputs, notices);
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.SELECT_DEFINITION::equals).isPresent()) {
                clearRevisionInputs(inputs);
                clearReplacementInputs(inputs);
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.ACTIVATE_DEFINITION::equals).isPresent()) {
                selectedId = changeState(
                        request, CatalogDefinitions.State.ACTIVE, notices);
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.INACTIVATE_DEFINITION::equals).isPresent()) {
                selectedId = changeState(
                        request, CatalogDefinitions.State.INACTIVE, notices);
            } else if (request.actionId().isPresent()
                    && !request.actionId().orElseThrow().equals(
                            CommercialCatalogScreenContract.DEFINITION_SEARCH)
                    && !request.actionId().orElseThrow().equals(
                            CommercialCatalogScreenContract.SELECT_DEFINITION)) {
                throw new IllegalArgumentException("Unsupported catalog definition action");
            }
        } catch (IllegalArgumentException invalidInput) {
            LOGGER.log(Level.WARNING,
                    "event=commercial_catalog_definition_input_rejected action={0}",
                    request.actionId().map(ScreenElementId::value).orElse("none"));
            selectedId = Optional.empty();
            notices.add(error(
                    "Revisa los datos ingresados",
                    "Completa código y nombre; usa decimales de 0 a 12 y una categoría superior válida."));
        }

        return load(inputs, selectedId, notices);
    }

    private Optional<String> revise(
            ScreenInteraction.Request request,
            Map<ScreenElementId, String> inputs,
            List<ScreenInteraction.Notice> notices) {
        String selected = request.selectedResourceId().orElseThrow(
                () -> new IllegalArgumentException("A selected definition is required"));
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("A selected definition version is required"));
        DefinitionIdentity identity = DefinitionIdentity.parse(selected);
        Optional<Integer> decimalScale = identity.kind() == CatalogDefinitions.SimpleKind.UNIT
                ? Optional.of(Integer.parseInt(required(
                        inputs,
                        CommercialCatalogScreenContract.DEFINITION_REVISION_UNIT_SCALE)))
                : Optional.empty();
        Optional<CategoryId> parentId = identity.kind() == CatalogDefinitions.SimpleKind.CATEGORY
                ? parent(
                        inputs,
                        CommercialCatalogScreenContract.DEFINITION_REVISION_CATEGORY_PARENT)
                : Optional.empty();
        CatalogOperationResult<CatalogDefinitions.SimpleRevision> result =
                useCases.reviseSimpleDefinition(
                        context(),
                        new CatalogDefinitionCommands.ReviseSimpleDefinition(
                                identity.kind(),
                                identity.identity(),
                                required(
                                        inputs,
                                        CommercialCatalogScreenContract.DEFINITION_REVISION_NAME),
                                decimalScale,
                                parentId,
                                version));
        if (!result.successful()) {
            notices.add(error(
                    "No se pudo crear la revisión",
                    failureMessage(result.code())));
            return Optional.of(selected);
        }
        CatalogDefinitions.SimpleRevision revised = result.value().orElseThrow();
        clearRevisionInputs(inputs);
        notices.add(new ScreenInteraction.Notice(
                ScreenInteraction.NoticeLevel.SUCCESS,
                "Revisión creada",
                "El código y la identidad se conservaron; la nueva versión quedó auditada."));
        return Optional.of(resourceId(revised.kind(), revised.identity()));
    }

    private Optional<String> replace(
            ScreenInteraction.Request request,
            Map<ScreenElementId, String> inputs,
            List<ScreenInteraction.Notice> notices) {
        String selected = request.selectedResourceId().orElseThrow(
                () -> new IllegalArgumentException("A selected definition is required"));
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("A selected definition version is required"));
        DefinitionIdentity identity = DefinitionIdentity.parse(selected);
        Optional<Integer> decimalScale = identity.kind() == CatalogDefinitions.SimpleKind.UNIT
                ? Optional.of(Integer.parseInt(required(
                        inputs,
                        CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_UNIT_SCALE)))
                : Optional.empty();
        Optional<CategoryId> parentId = identity.kind() == CatalogDefinitions.SimpleKind.CATEGORY
                ? parent(
                        inputs,
                        CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_CATEGORY_PARENT)
                : Optional.empty();
        CatalogOperationResult<CatalogDefinitions.Replacement> result =
                useCases.replaceSimpleDefinition(
                        context(),
                        new CatalogDefinitionCommands.ReplaceSimpleDefinition(
                                identity.kind(),
                                identity.identity(),
                                required(
                                        inputs,
                                        CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_CODE),
                                required(
                                        inputs,
                                        CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_NAME),
                                decimalScale,
                                parentId,
                                version));
        if (!result.successful()) {
            notices.add(error(
                    "No se pudo reemplazar la definición",
                    failureMessage(result.code())));
            return Optional.of(selected);
        }
        CatalogDefinitions.Replacement replacement = result.value().orElseThrow();
        clearReplacementInputs(inputs);
        clearRevisionInputs(inputs);
        notices.add(new ScreenInteraction.Notice(
                ScreenInteraction.NoticeLevel.SUCCESS,
                "Definición reemplazada",
                "La anterior quedó inactiva; sus referencias e historia no fueron reasignadas."));
        return Optional.of(resourceId(replacement.kind(), replacement.replacementIdentity()));
    }

    private Optional<String> changeState(
            ScreenInteraction.Request request,
            CatalogDefinitions.State targetState,
            List<ScreenInteraction.Notice> notices) {
        String resourceId = request.selectedResourceId().orElseThrow(
                () -> new IllegalArgumentException("A selected definition is required"));
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("A selected definition version is required"));
        DefinitionIdentity identity = DefinitionIdentity.parse(resourceId);
        CatalogOperationResult<CatalogDefinitions.Lifecycle> result =
                useCases.changeSimpleDefinitionState(
                        context(), new CatalogDefinitionCommands.ChangeSimpleState(
                                identity.kind(), identity.identity(), targetState, version));
        if (!result.successful()) {
            notices.add(error(
                    "No se pudo cambiar el estado",
                    failureMessage(result.code())));
            return Optional.of(resourceId);
        }
        CatalogDefinitions.Lifecycle lifecycle = result.value().orElseThrow();
        notices.add(new ScreenInteraction.Notice(
                ScreenInteraction.NoticeLevel.SUCCESS,
                targetState == CatalogDefinitions.State.ACTIVE
                        ? "Definición reactivada"
                        : "Definición inactivada",
                lifecycle.changed()
                        ? "El estado y la nueva versión quedaron auditados por el servidor."
                        : "La definición ya se encontraba en ese estado."));
        return Optional.of(resourceId(identity.kind(), lifecycle.identity()));
    }

    private Registration register(Map<ScreenElementId, String> inputs) {
        DefinitionKind kind = DefinitionKind.valueOf(required(
                inputs, CommercialCatalogScreenContract.DEFINITION_NEW_KIND)
                .toUpperCase(Locale.ROOT));
        String code = required(inputs, CommercialCatalogScreenContract.DEFINITION_NEW_CODE);
        String name = required(inputs, CommercialCatalogScreenContract.DEFINITION_NEW_NAME);
        return switch (kind) {
            case UNIT -> outcome(
                    useCases.registerUnit(context(), new CatalogDefinitionCommands.RegisterUnit(
                            new UnitCode(code), name,
                            Integer.parseInt(required(
                                    inputs,
                                    CommercialCatalogScreenContract.DEFINITION_UNIT_SCALE)))),
                    unit -> resourceId(DefinitionKind.UNIT, unit.code().value()));
            case CATEGORY -> outcome(
                    useCases.registerCategory(
                            context(),
                            new CatalogDefinitionCommands.RegisterCategory(
                                    parent(inputs), code, name)),
                    category -> resourceId(
                            DefinitionKind.CATEGORY, category.id().value().toString()));
            case BRAND -> outcome(
                    useCases.registerBrand(
                            context(), new CatalogDefinitionCommands.RegisterBrand(code, name)),
                    brand -> resourceId(DefinitionKind.BRAND, brand.id().value().toString()));
            case TAG -> outcome(
                    useCases.registerTag(
                            context(), new CatalogDefinitionCommands.RegisterTag(code, name)),
                    tag -> resourceId(DefinitionKind.TAG, tag.id().value().toString()));
        };
    }

    private ScreenInteraction.Result load(
            Map<ScreenElementId, String> inputs,
            Optional<String> selectedId,
            List<ScreenInteraction.Notice> notices) {
        CatalogOperationContext operationContext = context();
        CatalogOperationResult<CatalogDefinitions.Snapshot> result =
                useCases.managedDefinitions(operationContext);
        if (!result.successful()) {
            throw new IllegalStateException("Authorized catalog definitions query failed");
        }

        CatalogDefinitions.Snapshot snapshot = result.value().orElseThrow();
        inputs.putIfAbsent(CommercialCatalogScreenContract.DEFINITION_SEARCH_TEXT, "");
        inputs.putIfAbsent(CommercialCatalogScreenContract.DEFINITION_SEARCH_KIND, ALL);
        inputs.putIfAbsent(CommercialCatalogScreenContract.DEFINITION_SEARCH_STATE, ALL);
        inputs.putIfAbsent(
                CommercialCatalogScreenContract.DEFINITION_NEW_KIND,
                DefinitionKind.UNIT.name());
        inputs.putIfAbsent(CommercialCatalogScreenContract.DEFINITION_UNIT_SCALE, "0");
        inputs.putIfAbsent(CommercialCatalogScreenContract.DEFINITION_CATEGORY_PARENT, NONE);

        List<DefinitionView> definitions = definitions(snapshot);
        List<DefinitionView> visible = definitions.stream()
                .filter(definition -> matches(definition, inputs))
                .toList();
        Optional<DefinitionView> selected = selectedId.flatMap(id -> definitions.stream()
                .filter(definition -> definition.resourceId().equals(id))
                .findFirst());
        if (selectedId.isPresent() && selected.isEmpty()) {
            selectedId = Optional.empty();
            notices.add(error(
                    "Definición no disponible",
                    "El valor ya no está disponible dentro de esta empresa."));
        }

        selected.ifPresent(definition -> {
            inputs.putIfAbsent(
                    CommercialCatalogScreenContract.DEFINITION_REVISION_NAME,
                    definition.name());
            definition.decimalScale().ifPresent(scale -> inputs.putIfAbsent(
                    CommercialCatalogScreenContract.DEFINITION_REVISION_UNIT_SCALE,
                    Integer.toString(scale)));
            if (definition.kind() == DefinitionKind.CATEGORY) {
                inputs.putIfAbsent(
                        CommercialCatalogScreenContract.DEFINITION_REVISION_CATEGORY_PARENT,
                        definition.parentId().map(CategoryId::value)
                                .map(UUID::toString).orElse(NONE));
            }
            inputs.putIfAbsent(
                    CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_NAME,
                    definition.name());
            definition.decimalScale().ifPresent(scale -> inputs.putIfAbsent(
                    CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_UNIT_SCALE,
                    Integer.toString(scale)));
            if (definition.kind() == DefinitionKind.CATEGORY) {
                inputs.putIfAbsent(
                        CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_CATEGORY_PARENT,
                        definition.parentId().map(CategoryId::value)
                                .map(UUID::toString).orElse(NONE));
            }
        });

        ScreenInteraction.Table presentationTable;
        if (selected.isPresent()) {
            DefinitionIdentity identity = DefinitionIdentity.parse(
                    selected.orElseThrow().resourceId());
            CatalogOperationResult<List<CatalogDefinitions.SimpleRevision>> history =
                    useCases.simpleDefinitionHistory(
                            operationContext, identity.kind(), identity.identity());
            if (!history.successful()) {
                throw new IllegalStateException(
                        "Authorized simple definition history query failed");
            }
            presentationTable = historyTable(history.value().orElseThrow(), snapshot);
        } else {
            presentationTable = table(visible);
        }

        return new ScreenInteraction.Result(
                inputs,
                options(snapshot, selected),
                Optional.of(presentationTable),
                selected.map(DefinitionView::detail),
                notices,
                selected.map(DefinitionView::resourceId),
                selected.map(DefinitionView::version));
    }

    private CatalogOperationContext context() {
        return CatalogOperationContext.from(authorization.require(
                CommercialCatalogPluginDefinition.ID.value(),
                CommercialCatalogPermissions.DEFINITIONS_MANAGE.value()));
    }

    private static List<DefinitionView> definitions(CatalogDefinitions.Snapshot snapshot) {
        Map<CategoryId, String> categoryNames = snapshot.categories().stream()
                .collect(java.util.stream.Collectors.toMap(
                        CatalogDefinitions.Category::id,
                        CatalogDefinitions.Category::displayName));
        List<DefinitionView> values = new ArrayList<>();
        snapshot.units().forEach(unit -> values.add(new DefinitionView(
                resourceId(DefinitionKind.UNIT, unit.code().value()),
                DefinitionKind.UNIT,
                unit.code().value(),
                unit.displayName(),
                unit.state(),
                unit.version(),
                Optional.of(unit.decimalScale()),
                Optional.empty(),
                replacementLabel(
                        snapshot, CatalogDefinitions.SimpleKind.UNIT,
                        unit.code().value()),
                List.of(new ScreenInteraction.DetailItem(
                        "Decimales", Integer.toString(unit.decimalScale()))))));
        snapshot.categories().forEach(category -> values.add(new DefinitionView(
                resourceId(DefinitionKind.CATEGORY, category.id().value().toString()),
                DefinitionKind.CATEGORY,
                category.code(),
                category.displayName(),
                category.state(),
                category.version(),
                Optional.empty(),
                category.parentId(),
                replacementLabel(
                        snapshot, CatalogDefinitions.SimpleKind.CATEGORY,
                        category.id().value().toString()),
                List.of(new ScreenInteraction.DetailItem(
                        "Categoría superior",
                        category.parentId().map(parent ->
                                categoryNames.getOrDefault(parent, parent.value().toString()))
                                .orElse("Sin categoría superior"))))));
        snapshot.brands().forEach(brand -> values.add(new DefinitionView(
                resourceId(DefinitionKind.BRAND, brand.id().value().toString()),
                DefinitionKind.BRAND,
                brand.code(),
                brand.displayName(),
                brand.state(),
                brand.version(),
                Optional.empty(),
                Optional.empty(),
                replacementLabel(
                        snapshot, CatalogDefinitions.SimpleKind.BRAND,
                        brand.id().value().toString()),
                List.of())));
        snapshot.tags().forEach(tag -> values.add(new DefinitionView(
                resourceId(DefinitionKind.TAG, tag.id().value().toString()),
                DefinitionKind.TAG,
                tag.code(),
                tag.displayName(),
                tag.state(),
                tag.version(),
                Optional.empty(),
                Optional.empty(),
                replacementLabel(
                        snapshot, CatalogDefinitions.SimpleKind.TAG,
                        tag.id().value().toString()),
                List.of())));
        return values.stream()
                .sorted(Comparator.comparing((DefinitionView value) -> value.kind().label)
                        .thenComparing(DefinitionView::code))
                .toList();
    }

    private static Map<ScreenElementId, List<ScreenInteraction.Option>> options(
            CatalogDefinitions.Snapshot snapshot,
            Optional<DefinitionView> selected) {
        Map<ScreenElementId, List<ScreenInteraction.Option>> options = new LinkedHashMap<>();
        options.put(
                CommercialCatalogScreenContract.DEFINITION_SEARCH_KIND,
                List.of(
                        option(ALL, "Todos los tipos"),
                        option(DefinitionKind.UNIT.name(), DefinitionKind.UNIT.label),
                        option(DefinitionKind.CATEGORY.name(), DefinitionKind.CATEGORY.label),
                        option(DefinitionKind.BRAND.name(), DefinitionKind.BRAND.label),
                        option(DefinitionKind.TAG.name(), DefinitionKind.TAG.label)));
        options.put(
                CommercialCatalogScreenContract.DEFINITION_SEARCH_STATE,
                List.of(
                        option(ALL, "Todos los estados"),
                        option(CatalogDefinitions.State.ACTIVE.name(), "Activos"),
                        option(CatalogDefinitions.State.INACTIVE.name(), "Inactivos")));
        options.put(
                CommercialCatalogScreenContract.DEFINITION_NEW_KIND,
                List.of(
                        option(DefinitionKind.UNIT.name(), DefinitionKind.UNIT.label),
                        option(DefinitionKind.CATEGORY.name(), DefinitionKind.CATEGORY.label),
                        option(DefinitionKind.BRAND.name(), DefinitionKind.BRAND.label),
                        option(DefinitionKind.TAG.name(), DefinitionKind.TAG.label)));
        List<ScreenInteraction.Option> scales = java.util.stream.IntStream.rangeClosed(0, 12)
                .mapToObj(value -> option(Integer.toString(value), Integer.toString(value)))
                .toList();
        options.put(CommercialCatalogScreenContract.DEFINITION_UNIT_SCALE, scales);
        options.put(CommercialCatalogScreenContract.DEFINITION_REVISION_UNIT_SCALE, scales);
        options.put(CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_UNIT_SCALE, scales);
        List<ScreenInteraction.Option> parents = new ArrayList<>();
        parents.add(option(NONE, "Sin categoría superior"));
        snapshot.categories().stream()
                .filter(category -> category.state() == CatalogDefinitions.State.ACTIVE)
                .sorted(Comparator.comparing(CatalogDefinitions.Category::displayName))
                .map(category -> option(
                        category.id().value().toString(),
                        category.displayName() + " · " + category.code()))
                .forEach(parents::add);
        options.put(
                CommercialCatalogScreenContract.DEFINITION_CATEGORY_PARENT,
                List.copyOf(parents));
        List<ScreenInteraction.Option> revisionParents = parents.stream()
                .filter(parent -> selected
                        .filter(definition -> definition.kind() == DefinitionKind.CATEGORY)
                        .map(DefinitionView::resourceId)
                        .map(DefinitionIdentity::parse)
                        .map(DefinitionIdentity::identity)
                        .map(identity -> !identity.equals(parent.value()))
                        .orElse(true))
                .toList();
        options.put(
                CommercialCatalogScreenContract.DEFINITION_REVISION_CATEGORY_PARENT,
                revisionParents);
        options.put(
                CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_CATEGORY_PARENT,
                revisionParents);
        return Map.copyOf(options);
    }

    private static Optional<String> replacementLabel(
            CatalogDefinitions.Snapshot snapshot,
            CatalogDefinitions.SimpleKind kind,
            String identity) {
        return snapshot.replacements().stream()
                .filter(link -> link.kind() == kind && link.previousIdentity().equals(identity))
                .findFirst()
                .map(CatalogDefinitions.ReplacementLink::replacementIdentity)
                .map(replacementIdentity -> switch (kind) {
                    case UNIT -> snapshot.units().stream()
                            .filter(unit -> unit.code().value().equals(replacementIdentity))
                            .map(unit -> unit.displayName() + " · " + unit.code().value())
                            .findFirst().orElse(replacementIdentity);
                    case CATEGORY -> snapshot.categories().stream()
                            .filter(category -> category.id().value().toString()
                                    .equals(replacementIdentity))
                            .map(category -> category.displayName() + " · " + category.code())
                            .findFirst().orElse(replacementIdentity);
                    case BRAND -> snapshot.brands().stream()
                            .filter(brand -> brand.id().value().toString()
                                    .equals(replacementIdentity))
                            .map(brand -> brand.displayName() + " · " + brand.code())
                            .findFirst().orElse(replacementIdentity);
                    case TAG -> snapshot.tags().stream()
                            .filter(tag -> tag.id().value().toString().equals(replacementIdentity))
                            .map(tag -> tag.displayName() + " · " + tag.code())
                            .findFirst().orElse(replacementIdentity);
                });
    }

    private static ScreenInteraction.Table table(List<DefinitionView> definitions) {
        return new ScreenInteraction.Table(
                CommercialCatalogScreenContract.DEFINITION_RESULTS,
                List.of(
                        new ScreenInteraction.Column("kind", "Tipo"),
                        new ScreenInteraction.Column("code", "Código"),
                        new ScreenInteraction.Column("name", "Nombre"),
                        new ScreenInteraction.Column("state", "Estado")),
                definitions.stream().map(definition -> new ScreenInteraction.Row(
                        definition.resourceId(),
                        List.of(
                                definition.kind().label,
                                definition.code(),
                                definition.name(),
                                stateLabel(definition.state())))).toList(),
                definitions.size(),
                "No hay definiciones",
                "Registra una unidad, categoría, marca o etiqueta para utilizarla en el catálogo.");
    }

    private static ScreenInteraction.Table historyTable(
            List<CatalogDefinitions.SimpleRevision> revisions,
            CatalogDefinitions.Snapshot snapshot) {
        Map<CategoryId, String> categoryNames = snapshot.categories().stream()
                .collect(java.util.stream.Collectors.toMap(
                        CatalogDefinitions.Category::id,
                        CatalogDefinitions.Category::displayName));
        return new ScreenInteraction.Table(
                CommercialCatalogScreenContract.DEFINITION_HISTORY,
                List.of(
                        new ScreenInteraction.Column("version", "Versión"),
                        new ScreenInteraction.Column("status", "Vigencia"),
                        new ScreenInteraction.Column("name", "Nombre"),
                        new ScreenInteraction.Column("configuration", "Configuración"),
                        new ScreenInteraction.Column("state", "Estado")),
                revisions.stream().map(revision -> new ScreenInteraction.Row(
                        resourceId(revision.kind(), revision.identity()) + ":" + revision.version(),
                        List.of(
                                Long.toString(revision.version()),
                                revision.current() ? "Actual" : "Histórica",
                                revision.displayName(),
                                revisionConfiguration(revision, categoryNames),
                                stateLabel(revision.state())))).toList(),
                revisions.size(),
                "No hay revisiones",
                "La definición seleccionada no tiene revisiones disponibles para esta empresa.");
    }

    private static String revisionConfiguration(
            CatalogDefinitions.SimpleRevision revision,
            Map<CategoryId, String> categoryNames) {
        return switch (revision.kind()) {
            case UNIT -> "Decimales: " + revision.decimalScale().orElseThrow();
            case CATEGORY -> revision.parentId()
                    .map(parent -> "Categoría superior: "
                            + categoryNames.getOrDefault(parent, parent.value().toString()))
                    .orElse("Sin categoría superior");
            case BRAND, TAG -> "Sin configuración adicional";
        };
    }

    private static boolean matches(
            DefinitionView definition, Map<ScreenElementId, String> inputs) {
        String text = optional(inputs, CommercialCatalogScreenContract.DEFINITION_SEARCH_TEXT)
                .orElse("").toLowerCase(Locale.ROOT);
        String kind = optional(inputs, CommercialCatalogScreenContract.DEFINITION_SEARCH_KIND)
                .orElse(ALL);
        String state = optional(inputs, CommercialCatalogScreenContract.DEFINITION_SEARCH_STATE)
                .orElse(ALL);
        return (text.isEmpty()
                || definition.code().toLowerCase(Locale.ROOT).contains(text)
                || definition.name().toLowerCase(Locale.ROOT).contains(text))
                && (ALL.equals(kind) || definition.kind().name().equals(kind))
                && (ALL.equals(state) || definition.state().name().equals(state));
    }

    private static Optional<CategoryId> parent(Map<ScreenElementId, String> inputs) {
        return parent(inputs, CommercialCatalogScreenContract.DEFINITION_CATEGORY_PARENT);
    }

    private static Optional<CategoryId> parent(
            Map<ScreenElementId, String> inputs,
            ScreenElementId field) {
        Optional<String> value = optional(inputs, field);
        if (value.isEmpty() || NONE.equals(value.orElseThrow())) {
            return Optional.empty();
        }
        return Optional.of(new CategoryId(UUID.fromString(value.orElseThrow())));
    }

    private static <T> Registration outcome(
            CatalogOperationResult<T> result, Function<T, String> resourceId) {
        if (result.successful()) {
            return new Registration(
                    Optional.of(resourceId.apply(result.value().orElseThrow())),
                    CatalogResultCode.SUCCESS);
        }
        return new Registration(Optional.empty(), result.code());
    }

    private static String required(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return optional(inputs, field).orElseThrow(
                () -> new IllegalArgumentException("Missing required screen value"));
    }

    private static Optional<String> optional(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return Optional.ofNullable(inputs.get(field)).map(String::strip)
                .filter(value -> !value.isEmpty());
    }

    private static void clearCreateInputs(Map<ScreenElementId, String> inputs) {
        List.of(
                CommercialCatalogScreenContract.DEFINITION_NEW_CODE,
                CommercialCatalogScreenContract.DEFINITION_NEW_NAME,
                CommercialCatalogScreenContract.DEFINITION_CATEGORY_PARENT)
                .forEach(inputs::remove);
    }

    private static void clearRevisionInputs(Map<ScreenElementId, String> inputs) {
        List.of(
                CommercialCatalogScreenContract.DEFINITION_REVISION_NAME,
                CommercialCatalogScreenContract.DEFINITION_REVISION_UNIT_SCALE,
                CommercialCatalogScreenContract.DEFINITION_REVISION_CATEGORY_PARENT)
                .forEach(inputs::remove);
    }

    private static void clearReplacementInputs(Map<ScreenElementId, String> inputs) {
        List.of(
                CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_CODE,
                CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_NAME,
                CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_UNIT_SCALE,
                CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_CATEGORY_PARENT)
                .forEach(inputs::remove);
    }

    private static String resourceId(DefinitionKind kind, String identity) {
        return kind.name() + ":" + identity;
    }

    private static String resourceId(
            CatalogDefinitions.SimpleKind kind, String identity) {
        return kind.name() + ":" + identity;
    }

    private static String stateLabel(CatalogDefinitions.State state) {
        return state == CatalogDefinitions.State.ACTIVE ? "Activo" : "Inactivo";
    }

    private static String failureMessage(CatalogResultCode code) {
        return switch (code) {
            case CODE_CONFLICT -> "El código ya está utilizado dentro de esta empresa.";
            case ACCESS_DENIED -> "La autorización actual no permite administrar definiciones.";
            case VERSION_CONFLICT -> "La definición cambió; vuelve a cargar la información.";
            case REFERENCE_CONFLICT ->
                    "La categoría superior ya no está disponible.";
            case NOT_FOUND -> "La definición ya no está disponible dentro de esta empresa.";
            case VALIDITY_CONFLICT, IDENTIFIER_CONFLICT, INVALID_OPERATION ->
                    "Los datos no permiten registrar esta definición.";
            case SUCCESS -> throw new IllegalArgumentException("SUCCESS is not a failure");
        };
    }

    private static ScreenInteraction.Option option(String value, String label) {
        return new ScreenInteraction.Option(value, label);
    }

    private static ScreenInteraction.Notice error(String summary, String detail) {
        return new ScreenInteraction.Notice(ScreenInteraction.NoticeLevel.ERROR, summary, detail);
    }

    private enum DefinitionKind {
        UNIT("Unidad"),
        CATEGORY("Categoría"),
        BRAND("Marca"),
        TAG("Etiqueta");

        private final String label;

        DefinitionKind(String label) {
            this.label = label;
        }
    }

    private record DefinitionIdentity(
            CatalogDefinitions.SimpleKind kind, String identity) {

        private static DefinitionIdentity parse(String resourceId) {
            int separator = resourceId.indexOf(':');
            if (separator < 1 || separator == resourceId.length() - 1) {
                throw new IllegalArgumentException("Invalid definition identity");
            }
            return new DefinitionIdentity(
                    CatalogDefinitions.SimpleKind.valueOf(resourceId.substring(0, separator)),
                    resourceId.substring(separator + 1));
        }
    }

    private record Registration(Optional<String> resourceId, CatalogResultCode code) {
        private Registration {
            resourceId = java.util.Objects.requireNonNull(resourceId, "resourceId");
            code = java.util.Objects.requireNonNull(code, "code");
            if (resourceId.isPresent() != (code == CatalogResultCode.SUCCESS)) {
                throw new IllegalArgumentException("Registration result is inconsistent");
            }
        }

        private boolean successful() {
            return code == CatalogResultCode.SUCCESS;
        }
    }

    private record DefinitionView(
            String resourceId,
            DefinitionKind kind,
            String code,
            String name,
            CatalogDefinitions.State state,
            long version,
            Optional<Integer> decimalScale,
            Optional<CategoryId> parentId,
            Optional<String> replacement,
            List<ScreenInteraction.DetailItem> specificDetails) {

        private DefinitionView {
            decimalScale = java.util.Objects.requireNonNull(decimalScale, "decimalScale");
            parentId = java.util.Objects.requireNonNull(parentId, "parentId");
            replacement = java.util.Objects.requireNonNull(replacement, "replacement");
            specificDetails = List.copyOf(specificDetails);
        }

        private ScreenInteraction.Detail detail() {
            List<ScreenInteraction.DetailItem> items = new ArrayList<>(List.of(
                    new ScreenInteraction.DetailItem("Tipo", kind.label),
                    new ScreenInteraction.DetailItem("Código", code),
                    new ScreenInteraction.DetailItem("Nombre", name)));
            items.addAll(specificDetails);
            replacement.ifPresent(value -> items.add(
                    new ScreenInteraction.DetailItem("Reemplazada por", value)));
            items.add(new ScreenInteraction.DetailItem("Estado", stateLabel(state)));
            items.add(new ScreenInteraction.DetailItem("Versión", Long.toString(version)));
            return new ScreenInteraction.Detail(resourceId, name, items);
        }
    }
}
