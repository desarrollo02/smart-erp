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
import py.com.logixone.plugins.commercialcatalog.domain.VariantAttributeCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;
import py.com.logixone.plugins.commercialcatalog.domain.VariantValueType;

/** Authorized, guided administration of company-owned variant families. */
@ApplicationScoped
public class CommercialCatalogVariantFamilyScreenHandler implements ScreenInteraction.Handler {

    private static final Logger LOGGER = System.getLogger(
            CommercialCatalogVariantFamilyScreenHandler.class.getName());
    private static final String ALL = "ALL";
    private static final String REQUIRED = "REQUIRED";
    private static final String OPTIONAL = "OPTIONAL";
    private static final String EMPTY_DRAFT = "Sin atributos agregados";
    private static final String ATTRIBUTE_SEPARATOR = " • ";
    private static final String PART_SEPARATOR = " | ";
    private static final int MAX_ATTRIBUTES = 8;

    @Inject
    CommercialCatalogUseCases useCases;

    @Inject
    CurrentCompanyAuthorization authorization;

    @Override
    public ScreenId screenId() {
        return CommercialCatalogScreenContract.VARIANT_FAMILIES;
    }

    @Override
    public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return CommercialCatalogSelectorSources.VARIANT_FAMILIES;
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = new LinkedHashMap<>(request.inputs());
        Optional<String> selectedId = request.selectedResourceId();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();

        try {
            if (request.actionId().filter(
                    CommercialCatalogScreenContract.ADD_VARIANT_ATTRIBUTE::equals).isPresent()) {
                addAttribute(
                        inputs,
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_DRAFT,
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_CODE,
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_NAME,
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_TYPE,
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_REQUIRED);
                selectedId = Optional.empty();
                notices.add(info(
                        "Atributo agregado",
                        "El borrador conserva el orden. Puedes agregar otro atributo o registrar la familia."));
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.REMOVE_VARIANT_ATTRIBUTE::equals).isPresent()) {
                boolean removed = removeLastAttribute(
                        inputs, CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_DRAFT);
                selectedId = Optional.empty();
                notices.add(new ScreenInteraction.Notice(
                        removed ? ScreenInteraction.NoticeLevel.INFO
                                : ScreenInteraction.NoticeLevel.WARNING,
                        removed ? "Último atributo retirado" : "No hay atributos para retirar",
                        removed
                                ? "El borrador fue actualizado sin guardar todavía en el catálogo."
                                : "Agrega el primer atributo antes de intentar retirarlo."));
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.REGISTER_VARIANT_FAMILY::equals).isPresent()) {
                Registration registration = register(inputs);
                if (registration.successful()) {
                    selectedId = registration.resourceId();
                    clearCreateInputs(inputs);
                    notices.add(new ScreenInteraction.Notice(
                            ScreenInteraction.NoticeLevel.SUCCESS,
                            "Familia de variantes registrada",
                            "La familia y sus atributos quedaron disponibles dentro de la empresa activa."));
                } else {
                    selectedId = Optional.empty();
                    notices.add(error(
                            "No se pudo registrar la familia",
                            failureMessage(registration.code())));
                }
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.ADD_VARIANT_REVISION_ATTRIBUTE::equals)
                    .isPresent()) {
                request.selectedResourceId().orElseThrow(
                        () -> new IllegalArgumentException(
                                "Variant family selection is required"));
                addAttribute(
                        inputs,
                        CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_DRAFT,
                        CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_CODE,
                        CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_NAME,
                        CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_TYPE,
                        CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_REQUIRED);
                notices.add(info(
                        "Atributo incorporado a la revisión",
                        "El cambio sigue en borrador hasta crear la nueva revisión."));
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.REMOVE_VARIANT_REVISION_ATTRIBUTE::equals)
                    .isPresent()) {
                request.selectedResourceId().orElseThrow(
                        () -> new IllegalArgumentException(
                                "Variant family selection is required"));
                boolean removed = removeLastAttribute(
                        inputs, CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_DRAFT);
                notices.add(new ScreenInteraction.Notice(
                        removed ? ScreenInteraction.NoticeLevel.INFO
                                : ScreenInteraction.NoticeLevel.WARNING,
                        removed ? "Último atributo retirado de la revisión"
                                : "La revisión no tiene atributos para retirar",
                        removed
                                ? "El borrador cambió; la revisión todavía no fue creada."
                                : "Agrega un atributo antes de intentar retirarlo."));
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.REVISE_VARIANT_FAMILY::equals).isPresent()) {
                selectedId = revise(request, inputs, notices);
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.ACTIVATE_VARIANT_FAMILY::equals).isPresent()) {
                selectedId = changeState(
                        request, CatalogDefinitions.State.ACTIVE, notices);
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.INACTIVATE_VARIANT_FAMILY::equals).isPresent()) {
                selectedId = changeState(
                        request, CatalogDefinitions.State.INACTIVE, notices);
            } else if (request.actionId().isPresent()
                    && !request.actionId().orElseThrow().equals(
                            CommercialCatalogScreenContract.VARIANT_FAMILY_SEARCH)
                    && !request.actionId().orElseThrow().equals(
                            CommercialCatalogScreenContract.SELECT_VARIANT_FAMILY)) {
                throw new IllegalArgumentException("Unsupported variant family action");
            }
        } catch (IllegalArgumentException invalidInput) {
            LOGGER.log(Level.WARNING,
                    "event=commercial_catalog_variant_family_input_rejected action={0}",
                    request.actionId().map(ScreenElementId::value).orElse("none"));
            notices.add(error(
                    "Revisa la familia y sus atributos",
                    "Completa código y nombre, evita atributos duplicados y agrega entre 1 y 8 atributos válidos."));
        }

        return load(inputs, selectedId, notices);
    }

    private void addAttribute(
            Map<ScreenElementId, String> inputs,
            ScreenElementId draftField,
            ScreenElementId codeField,
            ScreenElementId nameField,
            ScreenElementId typeField,
            ScreenElementId requiredField) {
        List<CatalogDefinitions.VariantAttribute> attributes = draft(inputs, draftField);
        if (attributes.size() >= MAX_ATTRIBUTES) {
            throw new IllegalArgumentException("Variant family screen limit exceeded");
        }
        CatalogDefinitions.VariantAttribute attribute = currentAttribute(
                inputs, attributes.size(), codeField, nameField, typeField, requiredField);
        rejectDuplicate(attributes, attribute);
        attributes.add(attribute);
        inputs.put(draftField, formatDraft(attributes));
        clearCurrentAttribute(inputs, codeField, nameField, typeField, requiredField);
    }

    private boolean removeLastAttribute(
            Map<ScreenElementId, String> inputs, ScreenElementId draftField) {
        List<CatalogDefinitions.VariantAttribute> attributes = draft(inputs, draftField);
        if (attributes.isEmpty()) {
            return false;
        }
        attributes.removeLast();
        inputs.put(draftField, formatDraft(attributes));
        return true;
    }

    private Registration register(Map<ScreenElementId, String> inputs) {
        String code = required(inputs, CommercialCatalogScreenContract.VARIANT_FAMILY_NEW_CODE);
        String name = required(inputs, CommercialCatalogScreenContract.VARIANT_FAMILY_NEW_NAME);
        List<CatalogDefinitions.VariantAttribute> attributes = submissionAttributes(
                inputs,
                CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_DRAFT,
                CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_CODE,
                CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_NAME,
                CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_TYPE,
                CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_REQUIRED);

        CatalogOperationResult<CatalogDefinitions.VariantFamily> result =
                useCases.registerVariantFamily(
                        context(),
                        new CatalogDefinitionCommands.RegisterVariantFamily(
                                code, name, attributes));
        if (result.successful()) {
            CatalogDefinitions.VariantFamily family = result.value().orElseThrow();
            return new Registration(
                    Optional.of(resourceId(family)), CatalogResultCode.SUCCESS);
        }
        return new Registration(Optional.empty(), result.code());
    }

    private Optional<String> revise(
            ScreenInteraction.Request request,
            Map<ScreenElementId, String> inputs,
            List<ScreenInteraction.Notice> notices) {
        String selected = request.selectedResourceId().orElseThrow(
                () -> new IllegalArgumentException("Variant family selection is required"));
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("Variant family version is required"));
        String name = required(
                inputs, CommercialCatalogScreenContract.VARIANT_FAMILY_REVISION_NAME);
        List<CatalogDefinitions.VariantAttribute> attributes = submissionAttributes(
                inputs,
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_DRAFT,
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_CODE,
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_NAME,
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_TYPE,
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_REQUIRED);
        CatalogOperationResult<CatalogDefinitions.VariantFamily> result =
                useCases.reviseVariantFamily(
                        context(),
                        new CatalogDefinitionCommands.ReviseVariantFamily(
                                variantFamilyId(selected), name, attributes, version));
        if (result.successful()) {
            CatalogDefinitions.VariantFamily revised = result.value().orElseThrow();
            clearRevisionInputs(inputs);
            notices.add(new ScreenInteraction.Notice(
                    ScreenInteraction.NoticeLevel.SUCCESS,
                    "Nueva revisión de familia creada",
                    "Nombre y estructura quedaron versionados; las asignaciones anteriores conservan su revisión original."));
            return Optional.of(resourceId(revised));
        }
        notices.add(error(
                "No se pudo crear la revisión de la familia",
                failureMessage(result.code())));
        return Optional.of(selected);
    }

    private Optional<String> changeState(
            ScreenInteraction.Request request,
            CatalogDefinitions.State targetState,
            List<ScreenInteraction.Notice> notices) {
        String selected = request.selectedResourceId().orElseThrow(
                () -> new IllegalArgumentException("Variant family selection is required"));
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("Variant family version is required"));
        VariantFamilyId id = variantFamilyId(selected);
        CatalogOperationResult<CatalogDefinitions.VariantFamily> result =
                useCases.changeVariantFamilyState(
                        context(),
                        new CatalogDefinitionCommands.ChangeVariantFamilyState(
                                id, targetState, version));
        if (result.successful()) {
            CatalogDefinitions.VariantFamily changed = result.value().orElseThrow();
            boolean active = changed.state() == CatalogDefinitions.State.ACTIVE;
            notices.add(new ScreenInteraction.Notice(
                    ScreenInteraction.NoticeLevel.SUCCESS,
                    active ? "Familia de variantes reactivada"
                            : "Familia de variantes inactivada",
                    active
                            ? "La familia recuperó su estado activo sin cambiar sus atributos ni su identidad."
                            : "La familia quedó inactiva y conserva sus atributos, identidad y referencias históricas."));
            return Optional.of(resourceId(changed));
        }
        notices.add(error(
                "No se pudo cambiar el estado de la familia",
                failureMessage(result.code())));
        return Optional.of(selected);
    }

    private ScreenInteraction.Result load(
            Map<ScreenElementId, String> inputs,
            Optional<String> selectedId,
            List<ScreenInteraction.Notice> notices) {
        CatalogOperationContext operationContext = context();
        CatalogOperationResult<CatalogDefinitions.Snapshot> result =
                useCases.managedDefinitions(operationContext);
        if (!result.successful()) {
            throw new IllegalStateException("Authorized variant family query failed");
        }

        inputs.putIfAbsent(CommercialCatalogScreenContract.VARIANT_FAMILY_SEARCH_TEXT, "");
        inputs.putIfAbsent(CommercialCatalogScreenContract.VARIANT_FAMILY_SEARCH_STATE, ALL);
        inputs.putIfAbsent(
                CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_TYPE,
                VariantValueType.TEXT.name());
        inputs.putIfAbsent(
                CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_REQUIRED, REQUIRED);
        inputs.putIfAbsent(
                CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_DRAFT, EMPTY_DRAFT);
        inputs.putIfAbsent(
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_TYPE,
                VariantValueType.TEXT.name());
        inputs.putIfAbsent(
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_REQUIRED, REQUIRED);

        List<CatalogDefinitions.VariantFamily> families = result.value().orElseThrow()
                .variantFamilies().stream()
                .sorted(Comparator.comparing(CatalogDefinitions.VariantFamily::displayName))
                .toList();
        List<CatalogDefinitions.VariantFamily> visible = families.stream()
                .filter(family -> matches(family, inputs))
                .toList();
        Optional<CatalogDefinitions.VariantFamily> selected = selectedId.flatMap(id ->
                families.stream().filter(family -> resourceId(family).equals(id)).findFirst());
        if (selectedId.isPresent() && selected.isEmpty()) {
            selectedId = Optional.empty();
            notices.add(error(
                    "Familia no disponible",
                    "La familia ya no está disponible dentro de esta empresa."));
        }

        selected.ifPresent(family -> {
            inputs.putIfAbsent(
                    CommercialCatalogScreenContract.VARIANT_FAMILY_REVISION_NAME,
                    family.displayName());
            inputs.putIfAbsent(
                    CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_DRAFT,
                    formatDraft(family.attributes().stream()
                            .sorted(Comparator.comparingInt(
                                    CatalogDefinitions.VariantAttribute::position))
                            .toList()));
        });

        ScreenInteraction.Table presentationTable;
        if (selected.isPresent()) {
            CatalogOperationResult<List<CatalogDefinitions.VariantFamilyRevision>> history =
                    useCases.variantFamilyHistory(
                            operationContext, selected.orElseThrow().id());
            if (!history.successful()) {
                throw new IllegalStateException(
                        "Authorized variant family history query failed");
            }
            presentationTable = historyTable(history.value().orElseThrow());
        } else {
            presentationTable = table(visible);
        }

        return new ScreenInteraction.Result(
                inputs,
                options(),
                Optional.of(presentationTable),
                selected.map(CommercialCatalogVariantFamilyScreenHandler::detail),
                notices,
                selected.map(CommercialCatalogVariantFamilyScreenHandler::resourceId),
                selected.map(CatalogDefinitions.VariantFamily::version));
    }

    private CatalogOperationContext context() {
        return CatalogOperationContext.from(authorization.require(
                CommercialCatalogPluginDefinition.ID.value(),
                CommercialCatalogPermissions.DEFINITIONS_MANAGE.value()));
    }

    private static Map<ScreenElementId, List<ScreenInteraction.Option>> options() {
        List<ScreenInteraction.Option> types = List.of(
                option(VariantValueType.TEXT.name(), typeLabel(VariantValueType.TEXT)),
                option(VariantValueType.NUMBER.name(), typeLabel(VariantValueType.NUMBER)),
                option(VariantValueType.BOOLEAN.name(), typeLabel(VariantValueType.BOOLEAN)));
        List<ScreenInteraction.Option> requirements = List.of(
                option(REQUIRED, "Obligatorio"),
                option(OPTIONAL, "Opcional"));
        return Map.of(
                CommercialCatalogScreenContract.VARIANT_FAMILY_SEARCH_STATE,
                List.of(
                        option(ALL, "Todos los estados"),
                        option(CatalogDefinitions.State.ACTIVE.name(), "Activas"),
                        option(CatalogDefinitions.State.INACTIVE.name(), "Inactivas")),
                CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_TYPE,
                types,
                CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_REQUIRED,
                requirements,
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_TYPE,
                types,
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_REQUIRED,
                requirements);
    }

    private static ScreenInteraction.Table table(
            List<CatalogDefinitions.VariantFamily> families) {
        return new ScreenInteraction.Table(
                CommercialCatalogScreenContract.VARIANT_FAMILY_RESULTS,
                List.of(
                        new ScreenInteraction.Column("code", "Código"),
                        new ScreenInteraction.Column("name", "Nombre"),
                        new ScreenInteraction.Column("attributes", "Atributos"),
                        new ScreenInteraction.Column("state", "Estado")),
                families.stream().map(family -> new ScreenInteraction.Row(
                        resourceId(family),
                        List.of(
                                family.code(),
                                family.displayName(),
                                Integer.toString(family.attributes().size()),
                                stateLabel(family.state())))).toList(),
                families.size(),
                "No hay familias de variantes",
                "Registra una familia y sus atributos para describir variantes de artículos." );
    }

    private static ScreenInteraction.Table historyTable(
            List<CatalogDefinitions.VariantFamilyRevision> revisions) {
        return new ScreenInteraction.Table(
                CommercialCatalogScreenContract.VARIANT_FAMILY_HISTORY,
                List.of(
                        new ScreenInteraction.Column("version", "Versión"),
                        new ScreenInteraction.Column("status", "Revisión"),
                        new ScreenInteraction.Column("name", "Nombre"),
                        new ScreenInteraction.Column("state", "Estado"),
                        new ScreenInteraction.Column("attributes", "Estructura de atributos")),
                revisions.stream().map(revision -> new ScreenInteraction.Row(
                        revision.familyId().value() + ":" + revision.version(),
                        List.of(
                                Long.toString(revision.version()),
                                revision.current() ? "Actual" : "Histórica",
                                revision.displayName(),
                                stateLabel(revision.state()),
                                formatHistoryAttributes(revision.attributes())))).toList(),
                revisions.size(),
                "No hay revisiones de la familia",
                "La familia seleccionada no tiene revisiones disponibles para esta empresa.");
    }

    private static String formatHistoryAttributes(
            List<CatalogDefinitions.VariantAttribute> attributes) {
        return attributes.stream()
                .sorted(Comparator.comparingInt(CatalogDefinitions.VariantAttribute::position))
                .map(attribute -> attribute.code().value() + " · "
                        + typeLabel(attribute.valueType()) + " · "
                        + (attribute.required() ? "Obligatorio" : "Opcional"))
                .collect(java.util.stream.Collectors.joining("; "));
    }

    private static ScreenInteraction.Detail detail(CatalogDefinitions.VariantFamily family) {
        List<ScreenInteraction.DetailItem> items = new ArrayList<>(List.of(
                new ScreenInteraction.DetailItem("Código", family.code()),
                new ScreenInteraction.DetailItem("Nombre", family.displayName()),
                new ScreenInteraction.DetailItem(
                        "Cantidad de atributos", Integer.toString(family.attributes().size()))));
        family.attributes().stream()
                .sorted(Comparator.comparingInt(CatalogDefinitions.VariantAttribute::position))
                .limit(24)
                .forEach(attribute -> items.add(new ScreenInteraction.DetailItem(
                        "Atributo " + (attribute.position() + 1),
                        attribute.code().value() + " · " + attribute.displayName() + " · "
                                + typeLabel(attribute.valueType()) + " · "
                                + (attribute.required() ? "Obligatorio" : "Opcional"))));
        if (family.attributes().size() > 24) {
            items.add(new ScreenInteraction.DetailItem(
                    "Atributos adicionales",
                    Integer.toString(family.attributes().size() - 24)));
        }
        items.add(new ScreenInteraction.DetailItem("Estado", stateLabel(family.state())));
        items.add(new ScreenInteraction.DetailItem("Versión", Long.toString(family.version())));
        return new ScreenInteraction.Detail(resourceId(family), family.displayName(), items);
    }

    private static boolean matches(
            CatalogDefinitions.VariantFamily family,
            Map<ScreenElementId, String> inputs) {
        String text = optional(inputs, CommercialCatalogScreenContract.VARIANT_FAMILY_SEARCH_TEXT)
                .orElse("").toLowerCase(Locale.ROOT);
        String state = optional(inputs, CommercialCatalogScreenContract.VARIANT_FAMILY_SEARCH_STATE)
                .orElse(ALL);
        return (text.isEmpty()
                || family.code().toLowerCase(Locale.ROOT).contains(text)
                || family.displayName().toLowerCase(Locale.ROOT).contains(text))
                && (ALL.equals(state) || family.state().name().equals(state));
    }

    private static List<CatalogDefinitions.VariantAttribute> submissionAttributes(
            Map<ScreenElementId, String> inputs,
            ScreenElementId draftField,
            ScreenElementId codeField,
            ScreenElementId nameField,
            ScreenElementId typeField,
            ScreenElementId requiredField) {
        List<CatalogDefinitions.VariantAttribute> attributes = draft(inputs, draftField);
        boolean hasCode = optional(inputs, codeField).isPresent();
        boolean hasName = optional(inputs, nameField).isPresent();
        if (hasCode || hasName) {
            if (attributes.size() >= MAX_ATTRIBUTES) {
                throw new IllegalArgumentException("Variant family screen limit exceeded");
            }
            CatalogDefinitions.VariantAttribute attribute = currentAttribute(
                    inputs, attributes.size(), codeField, nameField, typeField, requiredField);
            rejectDuplicate(attributes, attribute);
            attributes.add(attribute);
        }
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("Variant family requires at least one attribute");
        }
        return attributes;
    }

    private static CatalogDefinitions.VariantAttribute currentAttribute(
            Map<ScreenElementId, String> inputs,
            int position,
            ScreenElementId codeField,
            ScreenElementId nameField,
            ScreenElementId typeField,
            ScreenElementId requiredField) {
        String code = required(inputs, codeField);
        String name = required(inputs, nameField);
        if (name.contains("|") || name.contains("•")) {
            throw new IllegalArgumentException("Variant attribute name contains a separator");
        }
        VariantValueType type = VariantValueType.valueOf(required(inputs, typeField));
        String requirement = required(inputs, requiredField);
        if (!REQUIRED.equals(requirement) && !OPTIONAL.equals(requirement)) {
            throw new IllegalArgumentException("Invalid variant attribute requirement");
        }
        return new CatalogDefinitions.VariantAttribute(
                new VariantAttributeCode(code), name, type, REQUIRED.equals(requirement), position);
    }

    private static List<CatalogDefinitions.VariantAttribute> draft(
            Map<ScreenElementId, String> inputs, ScreenElementId draftField) {
        String value = optional(inputs, draftField)
                .orElse(EMPTY_DRAFT);
        if (EMPTY_DRAFT.equals(value)) {
            return new ArrayList<>();
        }
        String[] encoded = value.split(ATTRIBUTE_SEPARATOR, -1);
        if (encoded.length > MAX_ATTRIBUTES) {
            throw new IllegalArgumentException("Variant family screen limit exceeded");
        }
        List<CatalogDefinitions.VariantAttribute> attributes = new ArrayList<>();
        for (int position = 0; position < encoded.length; position++) {
            String[] parts = encoded[position].split(" \\| ", -1);
            if (parts.length != 4) {
                throw new IllegalArgumentException("Invalid variant attribute draft");
            }
            VariantValueType type = typeFromLabel(parts[2]);
            boolean required = switch (parts[3]) {
                case "Obligatorio" -> true;
                case "Opcional" -> false;
                default -> throw new IllegalArgumentException(
                        "Invalid variant attribute requirement draft");
            };
            CatalogDefinitions.VariantAttribute attribute = new CatalogDefinitions.VariantAttribute(
                    new VariantAttributeCode(parts[0]), parts[1], type, required, position);
            rejectDuplicate(attributes, attribute);
            attributes.add(attribute);
        }
        return attributes;
    }

    private static String formatDraft(List<CatalogDefinitions.VariantAttribute> attributes) {
        if (attributes.isEmpty()) {
            return EMPTY_DRAFT;
        }
        return attributes.stream().map(attribute ->
                attribute.code().value() + PART_SEPARATOR
                        + attribute.displayName() + PART_SEPARATOR
                        + typeLabel(attribute.valueType()) + PART_SEPARATOR
                        + (attribute.required() ? "Obligatorio" : "Opcional"))
                .collect(java.util.stream.Collectors.joining(ATTRIBUTE_SEPARATOR));
    }

    private static void rejectDuplicate(
            List<CatalogDefinitions.VariantAttribute> attributes,
            CatalogDefinitions.VariantAttribute candidate) {
        if (attributes.stream().anyMatch(attribute -> attribute.code().equals(candidate.code()))) {
            throw new IllegalArgumentException("Duplicate variant attribute code");
        }
    }

    private static VariantValueType typeFromLabel(String value) {
        return switch (value) {
            case "Texto" -> VariantValueType.TEXT;
            case "Número" -> VariantValueType.NUMBER;
            case "Sí/No" -> VariantValueType.BOOLEAN;
            default -> throw new IllegalArgumentException("Invalid variant attribute type draft");
        };
    }

    private static String typeLabel(VariantValueType type) {
        return switch (type) {
            case TEXT -> "Texto";
            case NUMBER -> "Número";
            case BOOLEAN -> "Sí/No";
        };
    }

    private static Optional<String> optional(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return Optional.ofNullable(inputs.get(field)).map(String::strip)
                .filter(value -> !value.isEmpty());
    }

    private static String required(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return optional(inputs, field).orElseThrow(
                () -> new IllegalArgumentException("Missing required screen value"));
    }

    private static void clearCurrentAttribute(
            Map<ScreenElementId, String> inputs,
            ScreenElementId codeField,
            ScreenElementId nameField,
            ScreenElementId typeField,
            ScreenElementId requiredField) {
        inputs.remove(codeField);
        inputs.remove(nameField);
        inputs.put(typeField, VariantValueType.TEXT.name());
        inputs.put(requiredField, REQUIRED);
    }

    private static void clearCreateInputs(Map<ScreenElementId, String> inputs) {
        inputs.remove(CommercialCatalogScreenContract.VARIANT_FAMILY_NEW_CODE);
        inputs.remove(CommercialCatalogScreenContract.VARIANT_FAMILY_NEW_NAME);
        clearCurrentAttribute(
                inputs,
                CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_CODE,
                CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_NAME,
                CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_TYPE,
                CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_REQUIRED);
        inputs.put(CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_DRAFT, EMPTY_DRAFT);
    }

    private static void clearRevisionInputs(Map<ScreenElementId, String> inputs) {
        inputs.remove(CommercialCatalogScreenContract.VARIANT_FAMILY_REVISION_NAME);
        clearCurrentAttribute(
                inputs,
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_CODE,
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_NAME,
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_TYPE,
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_REQUIRED);
        inputs.remove(CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_DRAFT);
    }

    private static String resourceId(CatalogDefinitions.VariantFamily family) {
        return "VARIANT_FAMILY:" + family.id().value();
    }

    private static VariantFamilyId variantFamilyId(String resourceId) {
        String prefix = "VARIANT_FAMILY:";
        if (!resourceId.startsWith(prefix)) {
            throw new IllegalArgumentException("Invalid variant family resource identity");
        }
        return new VariantFamilyId(UUID.fromString(resourceId.substring(prefix.length())));
    }

    private static String stateLabel(CatalogDefinitions.State state) {
        return state == CatalogDefinitions.State.ACTIVE ? "Activa" : "Inactiva";
    }

    private static String failureMessage(CatalogResultCode code) {
        return switch (code) {
            case CODE_CONFLICT -> "El código de familia ya está utilizado dentro de esta empresa.";
            case ACCESS_DENIED -> "La autorización actual no permite administrar familias.";
            case VERSION_CONFLICT -> "La familia cambió; vuelve a cargar la información.";
            case REFERENCE_CONFLICT, NOT_FOUND -> "Una referencia de la familia ya no está disponible.";
            case VALIDITY_CONFLICT, IDENTIFIER_CONFLICT, INVALID_OPERATION ->
                    "Los datos no permiten registrar esta familia de variantes.";
            case SUCCESS -> throw new IllegalArgumentException("SUCCESS is not a failure");
        };
    }

    private static ScreenInteraction.Option option(String value, String label) {
        return new ScreenInteraction.Option(value, label);
    }

    private static ScreenInteraction.Notice info(String summary, String detail) {
        return new ScreenInteraction.Notice(ScreenInteraction.NoticeLevel.INFO, summary, detail);
    }

    private static ScreenInteraction.Notice error(String summary, String detail) {
        return new ScreenInteraction.Notice(ScreenInteraction.NoticeLevel.ERROR, summary, detail);
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
}
