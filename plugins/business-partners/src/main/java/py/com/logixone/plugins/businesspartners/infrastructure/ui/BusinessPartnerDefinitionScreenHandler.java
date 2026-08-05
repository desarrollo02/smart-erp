package py.com.logixone.plugins.businesspartners.infrastructure.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugin.api.SelectorSourceDefinition;
import py.com.logixone.plugins.businesspartners.BusinessPartnersPluginDefinition;
import py.com.logixone.plugins.businesspartners.BusinessPartnersScreenContract;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerDefinitionUseCases;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerOperationContext;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerPermissions;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerResultCode;
import py.com.logixone.plugins.businesspartners.application.command.ChangeBusinessPartnerDefinitionState;
import py.com.logixone.plugins.businesspartners.application.command.RegisterBusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.application.command.ReviseBusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionRevision;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerName;

/** Authorized administration for business-partner selector definitions. */
@ApplicationScoped
public class BusinessPartnerDefinitionScreenHandler implements ScreenInteraction.Handler {

    private static final Logger LOGGER = System.getLogger(
            BusinessPartnerDefinitionScreenHandler.class.getName());
    private static final String ALL = "ALL";

    @Inject
    BusinessPartnerDefinitionUseCases useCases;

    @Inject
    CurrentCompanyAuthorization authorization;

    @Override
    public ScreenId screenId() {
        return BusinessPartnersScreenContract.DEFINITIONS;
    }

    @Override
    public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return BusinessPartnerSelectorSources.DEFINITIONS;
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = new LinkedHashMap<>(request.inputs());
        inputs.putIfAbsent(
                BusinessPartnersScreenContract.DEFINITION_KIND,
                BusinessPartnerDefinitionKind.CHANNEL_KIND.name());
        inputs.putIfAbsent(
                BusinessPartnersScreenContract.DEFINITION_NEW_KIND,
                BusinessPartnerDefinitionKind.CHANNEL_KIND.name());
        Optional<String> selectedId = Optional.empty();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();

        try {
            Optional<DefinitionReference> selectedReference = request.selectedResourceId()
                    .map(BusinessPartnerDefinitionScreenHandler::reference);
            selectedReference.ifPresent(value -> inputs.put(
                    BusinessPartnersScreenContract.DEFINITION_KIND,
                    value.kind().name()));
            selectedId = selectedReference.map(value -> value.code().value());
            if (request.actionId().filter(
                    BusinessPartnersScreenContract.REGISTER_DEFINITION::equals).isPresent()) {
                BusinessPartnerDefinitionKind kind = newDefinitionKind(inputs);
                inputs.put(BusinessPartnersScreenContract.DEFINITION_KIND, kind.name());
                var result = useCases.registerDefinition(
                        context(),
                        new RegisterBusinessPartnerDefinition(
                                kind,
                                new BusinessPartnerAttributeCode(required(
                                        inputs,
                                        BusinessPartnersScreenContract.DEFINITION_NEW_CODE)),
                                new BusinessPartnerName(required(
                                        inputs,
                                        BusinessPartnersScreenContract.DEFINITION_NEW_NAME))));
                if (result.successful()) {
                    BusinessPartnerDefinition created = result.value().orElseThrow();
                    selectedId = Optional.of(created.code().value());
                    inputs.remove(BusinessPartnersScreenContract.DEFINITION_NEW_CODE);
                    inputs.remove(BusinessPartnersScreenContract.DEFINITION_NEW_NAME);
                    notices.add(new ScreenInteraction.Notice(
                            ScreenInteraction.NoticeLevel.SUCCESS,
                            definitionLabel(kind) + " registrado",
                            "El valor ya está disponible dentro de la empresa activa."));
                } else {
                    selectedId = Optional.empty();
                    notices.add(error(
                            "No se pudo registrar " + definitionLabel(kind).toLowerCase(Locale.ROOT),
                            failureMessage(result.code())));
                }
            } else if (request.actionId().filter(
                    BusinessPartnersScreenContract.REVISE_DEFINITION::equals).isPresent()) {
                selectedId = revise(request, inputs, notices);
            } else if (request.actionId().filter(
                    BusinessPartnersScreenContract.SELECT_DEFINITION::equals).isPresent()) {
                inputs.remove(BusinessPartnersScreenContract.DEFINITION_EDIT_NAME);
            } else if (request.actionId().filter(
                    BusinessPartnersScreenContract.DEFINITION_SEARCH::equals).isPresent()) {
                selectedId = Optional.empty();
            } else if (request.actionId().filter(
                    BusinessPartnersScreenContract.ACTIVATE_DEFINITION::equals).isPresent()) {
                selectedId = changeState(
                        request, inputs, BusinessPartnerState.ACTIVE, notices);
            } else if (request.actionId().filter(
                    BusinessPartnersScreenContract.INACTIVATE_DEFINITION::equals).isPresent()) {
                selectedId = changeState(
                        request, inputs, BusinessPartnerState.INACTIVE, notices);
            } else if (request.actionId().isPresent()
                    && !request.actionId().orElseThrow().equals(
                            BusinessPartnersScreenContract.DEFINITION_SEARCH)
                    && !request.actionId().orElseThrow().equals(
                            BusinessPartnersScreenContract.SELECT_DEFINITION)) {
                throw new IllegalArgumentException("Unsupported business-partner definition action");
            }
        } catch (IllegalArgumentException invalidInput) {
            LOGGER.log(Level.WARNING,
                    "event=business_partner_definition_input_rejected action={0}",
                    request.actionId().map(ScreenElementId::value).orElse("none"));
            selectedId = Optional.empty();
            notices.add(error(
                    "Revisa los datos ingresados",
                    "Completa un código estable y un nombre comprensible."));
        }

        return load(inputs, selectedId, notices);
    }

    private Optional<String> revise(
            ScreenInteraction.Request request,
            Map<ScreenElementId, String> inputs,
            List<ScreenInteraction.Notice> notices) {
        DefinitionReference reference = selectedReference(request);
        String code = reference.code().value();
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("A selected definition version is required"));
        BusinessPartnerDefinitionKind kind = reference.kind();
        inputs.put(BusinessPartnersScreenContract.DEFINITION_KIND, kind.name());
        var result = useCases.reviseDefinition(
                context(),
                new ReviseBusinessPartnerDefinition(
                        kind,
                        new BusinessPartnerAttributeCode(code),
                        new BusinessPartnerName(required(
                                inputs, BusinessPartnersScreenContract.DEFINITION_EDIT_NAME)),
                        version));
        if (!result.successful()) {
            notices.add(error(
                    "No se pudo guardar la revisión",
                    failureMessage(result.code())));
            return Optional.of(code);
        }
        BusinessPartnerDefinition revised = result.value().orElseThrow();
        inputs.remove(BusinessPartnersScreenContract.DEFINITION_EDIT_NAME);
        notices.add(new ScreenInteraction.Notice(
                ScreenInteraction.NoticeLevel.SUCCESS,
                "Nombre de " + definitionLabel(kind).toLowerCase(Locale.ROOT) + " actualizado",
                revised.version() == version
                        ? "El nombre ya coincidía con la revisión vigente."
                        : "El código se conservó y la nueva versión quedó registrada en el historial."));
        return Optional.of(revised.code().value());
    }

    private Optional<String> changeState(
            ScreenInteraction.Request request,
            Map<ScreenElementId, String> inputs,
            BusinessPartnerState targetState,
            List<ScreenInteraction.Notice> notices) {
        DefinitionReference reference = selectedReference(request);
        String code = reference.code().value();
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("A selected definition version is required"));
        BusinessPartnerDefinitionKind kind = reference.kind();
        inputs.put(BusinessPartnersScreenContract.DEFINITION_KIND, kind.name());
        var result = useCases.changeDefinitionState(
                context(),
                new ChangeBusinessPartnerDefinitionState(
                        kind,
                        new BusinessPartnerAttributeCode(code),
                        targetState,
                        version));
        if (!result.successful()) {
            notices.add(error("No se pudo cambiar el estado", failureMessage(result.code())));
            return Optional.of(code);
        }
        BusinessPartnerDefinition changed = result.value().orElseThrow();
        notices.add(new ScreenInteraction.Notice(
                ScreenInteraction.NoticeLevel.SUCCESS,
                targetState == BusinessPartnerState.ACTIVE
                        ? definitionLabel(kind) + " reactivado"
                        : definitionLabel(kind) + " inactivado",
                changed.version() == version
                        ? "La definición ya se encontraba en ese estado."
                        : "El estado y la nueva versión quedaron auditados por el servidor."));
        return Optional.of(changed.code().value());
    }

    private ScreenInteraction.Result load(
            Map<ScreenElementId, String> inputs,
            Optional<String> selectedId,
            List<ScreenInteraction.Notice> notices) {
        BusinessPartnerOperationContext operationContext = context();
        BusinessPartnerDefinitionKind kind = definitionKind(inputs);
        var result = useCases.definitions(
                operationContext, kind);
        if (!result.successful()) {
            throw new IllegalStateException("Authorized business-partner definitions query failed");
        }
        inputs.putIfAbsent(BusinessPartnersScreenContract.DEFINITION_SEARCH_TEXT, "");
        inputs.putIfAbsent(BusinessPartnersScreenContract.DEFINITION_SEARCH_STATE, ALL);

        List<BusinessPartnerDefinition> definitions = result.value().orElseThrow();
        List<BusinessPartnerDefinition> visible = definitions.stream()
                .filter(value -> matches(value, inputs))
                .toList();
        Optional<BusinessPartnerDefinition> selected = selectedId.flatMap(id -> definitions.stream()
                .filter(value -> value.code().value().equals(id))
                .findFirst());
        if (selectedId.isPresent() && selected.isEmpty()) {
            selectedId = Optional.empty();
            notices.add(error(
                    "Definición no disponible",
                    "El valor ya no está disponible dentro de esta empresa."));
        }
        selected.ifPresent(definition -> inputs.putIfAbsent(
                BusinessPartnersScreenContract.DEFINITION_EDIT_NAME,
                definition.displayName().value()));

        ScreenInteraction.Table presentationTable;
        if (selected.isPresent()) {
            BusinessPartnerDefinition definition = selected.orElseThrow();
            var history = useCases.definitionHistory(
                    operationContext,
                    kind,
                    definition.code());
            if (!history.successful()) {
                throw new IllegalStateException(
                        "Authorized business-partner definition history query failed");
            }
            presentationTable = historyTable(
                    history.value().orElseThrow(), definition.version(), kind);
        } else {
            presentationTable = table(visible, kind);
        }

        return new ScreenInteraction.Result(
                inputs,
                options(),
                Optional.of(presentationTable),
                selected.map(BusinessPartnerDefinitionScreenHandler::detail),
                notices,
                selected.map(value -> referenceId(value.kind(), value.code())),
                selected.map(BusinessPartnerDefinition::version));
    }

    private BusinessPartnerOperationContext context() {
        return BusinessPartnerOperationContext.from(authorization.require(
                BusinessPartnersPluginDefinition.ID.value(),
                BusinessPartnerPermissions.MANAGE.value()));
    }

    private static ScreenInteraction.Table table(
            List<BusinessPartnerDefinition> definitions,
            BusinessPartnerDefinitionKind kind) {
        return new ScreenInteraction.Table(
                BusinessPartnersScreenContract.DEFINITION_RESULTS,
                List.of(
                        new ScreenInteraction.Column("code", "Código"),
                        new ScreenInteraction.Column("name", "Nombre"),
                        new ScreenInteraction.Column("state", "Estado")),
                definitions.stream().map(value -> new ScreenInteraction.Row(
                        referenceId(kind, value.code()),
                        List.of(
                                value.code().value(),
                                value.displayName().value(),
                                stateLabel(value.state())))).toList(),
                definitions.size(),
                "No hay " + definitionPluralLabel(kind).toLowerCase(Locale.ROOT),
                "Registra el primer valor empresarial para utilizarlo en socios comerciales.");
    }

    private static ScreenInteraction.Table historyTable(
            List<BusinessPartnerDefinitionRevision> revisions,
            long currentVersion,
            BusinessPartnerDefinitionKind kind) {
        return new ScreenInteraction.Table(
                BusinessPartnersScreenContract.DEFINITION_HISTORY,
                List.of(
                        new ScreenInteraction.Column("version", "Versión"),
                        new ScreenInteraction.Column("status", "Condición"),
                        new ScreenInteraction.Column("name", "Nombre"),
                        new ScreenInteraction.Column("state", "Estado"),
                        new ScreenInteraction.Column("changed_at", "Registrada")),
                revisions.stream().map(revision -> new ScreenInteraction.Row(
                        revision.code().value() + ":" + revision.version(),
                        List.of(
                                Long.toString(revision.version()),
                                revision.version() == currentVersion ? "Actual" : "Histórica",
                                revision.displayName().value(),
                                stateLabel(revision.state()),
                                revision.changedAt().toString()))).toList(),
                revisions.size(),
                "No hay revisiones disponibles",
                definitionLabel(kind) + " seleccionado no tiene historia visible dentro de esta empresa.");
    }

    private static ScreenInteraction.Detail detail(BusinessPartnerDefinition definition) {
        return new ScreenInteraction.Detail(
                referenceId(definition.kind(), definition.code()),
                definition.displayName().value(),
                List.of(
                        new ScreenInteraction.DetailItem(
                                "Clase", definitionLabel(definition.kind())),
                        new ScreenInteraction.DetailItem("Código", definition.code().value()),
                        new ScreenInteraction.DetailItem("Nombre", definition.displayName().value()),
                        new ScreenInteraction.DetailItem("Estado", stateLabel(definition.state())),
                        new ScreenInteraction.DetailItem(
                                "Versión", Long.toString(definition.version()))));
    }

    private static Map<ScreenElementId, List<ScreenInteraction.Option>> options() {
        List<ScreenInteraction.Option> kinds = List.of(
                new ScreenInteraction.Option(
                        BusinessPartnerDefinitionKind.CHANNEL_KIND.name(),
                        definitionPluralLabel(BusinessPartnerDefinitionKind.CHANNEL_KIND)),
                new ScreenInteraction.Option(
                        BusinessPartnerDefinitionKind.IDENTIFICATION_TYPE.name(),
                        definitionPluralLabel(BusinessPartnerDefinitionKind.IDENTIFICATION_TYPE)),
                new ScreenInteraction.Option(
                        BusinessPartnerDefinitionKind.ADDRESS_TYPE.name(),
                        definitionPluralLabel(BusinessPartnerDefinitionKind.ADDRESS_TYPE)),
                new ScreenInteraction.Option(
                        BusinessPartnerDefinitionKind.ADDRESS_PURPOSE.name(),
                        definitionPluralLabel(BusinessPartnerDefinitionKind.ADDRESS_PURPOSE)));
        return Map.of(
                BusinessPartnersScreenContract.DEFINITION_KIND,
                kinds,
                BusinessPartnersScreenContract.DEFINITION_NEW_KIND,
                kinds,
                BusinessPartnersScreenContract.DEFINITION_SEARCH_STATE,
                List.of(
                        new ScreenInteraction.Option(ALL, "Todos los estados"),
                        new ScreenInteraction.Option(BusinessPartnerState.ACTIVE.name(), "Activos"),
                        new ScreenInteraction.Option(BusinessPartnerState.INACTIVE.name(), "Inactivos")));
    }

    private static boolean matches(
            BusinessPartnerDefinition definition, Map<ScreenElementId, String> inputs) {
        String text = optional(inputs, BusinessPartnersScreenContract.DEFINITION_SEARCH_TEXT)
                .orElse("").toLowerCase(Locale.ROOT);
        String state = optional(inputs, BusinessPartnersScreenContract.DEFINITION_SEARCH_STATE)
                .orElse(ALL);
        return (text.isEmpty()
                || definition.code().value().contains(text)
                || definition.displayName().value().toLowerCase(Locale.ROOT).contains(text))
                && (ALL.equals(state) || definition.state().name().equals(state));
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

    private static BusinessPartnerDefinitionKind definitionKind(
            Map<ScreenElementId, String> inputs) {
        return BusinessPartnerDefinitionKind.valueOf(optional(
                        inputs, BusinessPartnersScreenContract.DEFINITION_KIND)
                .orElse(BusinessPartnerDefinitionKind.CHANNEL_KIND.name()));
    }

    private static BusinessPartnerDefinitionKind newDefinitionKind(
            Map<ScreenElementId, String> inputs) {
        return BusinessPartnerDefinitionKind.valueOf(optional(
                        inputs, BusinessPartnersScreenContract.DEFINITION_NEW_KIND)
                .orElse(BusinessPartnerDefinitionKind.CHANNEL_KIND.name()));
    }

    private static DefinitionReference selectedReference(ScreenInteraction.Request request) {
        return reference(request.selectedResourceId().orElseThrow(
                () -> new IllegalArgumentException("A selected definition is required")));
    }

    private static DefinitionReference reference(String resourceId) {
        int separator = resourceId.indexOf(':');
        if (separator < 0) {
            return new DefinitionReference(
                    BusinessPartnerDefinitionKind.CHANNEL_KIND,
                    new BusinessPartnerAttributeCode(resourceId));
        }
        if (separator == 0 || separator == resourceId.length() - 1) {
            throw new IllegalArgumentException("Invalid definition reference");
        }
        return new DefinitionReference(
                BusinessPartnerDefinitionKind.valueOf(resourceId.substring(0, separator)),
                new BusinessPartnerAttributeCode(resourceId.substring(separator + 1)));
    }

    private static String referenceId(
            BusinessPartnerDefinitionKind kind, BusinessPartnerAttributeCode code) {
        return kind.name() + ":" + code.value();
    }

    private record DefinitionReference(
            BusinessPartnerDefinitionKind kind, BusinessPartnerAttributeCode code) {
    }

    private static String definitionLabel(BusinessPartnerDefinitionKind kind) {
        return switch (kind) {
            case CHANNEL_KIND -> "Tipo de canal";
            case IDENTIFICATION_TYPE -> "Tipo de identificación";
            case ADDRESS_TYPE -> "Tipo de dirección";
            case ADDRESS_PURPOSE -> "Propósito de dirección";
        };
    }

    private static String definitionPluralLabel(BusinessPartnerDefinitionKind kind) {
        return switch (kind) {
            case CHANNEL_KIND -> "Tipos de canal";
            case IDENTIFICATION_TYPE -> "Tipos de identificación";
            case ADDRESS_TYPE -> "Tipos de dirección";
            case ADDRESS_PURPOSE -> "Propósitos de dirección";
        };
    }

    private static String stateLabel(BusinessPartnerState state) {
        return state == BusinessPartnerState.ACTIVE ? "Activo" : "Inactivo";
    }

    private static String failureMessage(BusinessPartnerResultCode code) {
        return switch (code) {
            case GENERAL_CODE_CONFLICT -> "El código ya existe dentro de esta empresa.";
            case ACCESS_DENIED -> "La autorización actual no permite administrar definiciones.";
            case VERSION_CONFLICT -> "La definición cambió; vuelve a cargar la información.";
            case NOT_FOUND, ROLE_CODE_CONFLICT, INVALID_OPERATION ->
                    "Los datos no permiten registrar esta definición.";
            case SUCCESS -> throw new IllegalArgumentException("SUCCESS is not a failure");
        };
    }

    private static ScreenInteraction.Notice error(String summary, String detail) {
        return new ScreenInteraction.Notice(ScreenInteraction.NoticeLevel.ERROR, summary, detail);
    }
}
