package py.com.logixone.plugins.commercialcatalog.infrastructure.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Instant;
import java.time.DateTimeException;
import java.util.ArrayList;
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

/** Authorized tax-profile administration without exposing fiscal-provider details. */
@ApplicationScoped
public class CommercialCatalogTaxProfileScreenHandler implements ScreenInteraction.Handler {

    private static final Logger LOGGER = System.getLogger(
            CommercialCatalogTaxProfileScreenHandler.class.getName());
    private static final String ALL = "ALL";

    @Inject
    CommercialCatalogUseCases useCases;

    @Inject
    CurrentCompanyAuthorization authorization;

    @Override
    public ScreenId screenId() {
        return CommercialCatalogScreenContract.TAX_PROFILES;
    }

    @Override
    public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return CommercialCatalogSelectorSources.TAX_PROFILES;
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = new LinkedHashMap<>(request.inputs());
        Optional<String> selectedId = request.selectedResourceId();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();

        try {
            if (request.actionId().filter(
                    CommercialCatalogScreenContract.REGISTER_TAX_PROFILE::equals).isPresent()) {
                CatalogDefinitionCommands.RegisterTaxProfile command =
                        new CatalogDefinitionCommands.RegisterTaxProfile(
                                required(inputs, CommercialCatalogScreenContract.TAX_PROFILE_NEW_CODE),
                                required(inputs, CommercialCatalogScreenContract.TAX_PROFILE_NEW_NAME),
                                required(inputs, CommercialCatalogScreenContract.TAX_PROFILE_NEW_KIND),
                                required(inputs,
                                        CommercialCatalogScreenContract.TAX_PROFILE_NEW_DESCRIPTION),
                                instant(inputs,
                                        CommercialCatalogScreenContract.TAX_PROFILE_NEW_VALID_FROM),
                                optional(inputs,
                                        CommercialCatalogScreenContract.TAX_PROFILE_NEW_VALID_UNTIL)
                                        .map(Instant::parse));
                CatalogOperationResult<CatalogDefinitions.TaxProfile> result =
                        useCases.registerTaxProfile(context(), command);
                if (result.successful()) {
                    CatalogDefinitions.TaxProfile created = result.value().orElseThrow();
                    selectedId = Optional.of(created.id().value().toString());
                    clearCreateInputs(inputs);
                    notices.add(new ScreenInteraction.Notice(
                            ScreenInteraction.NoticeLevel.SUCCESS,
                            "Perfil tributario registrado",
                            "La definición interna quedó disponible y auditada para esta empresa."));
                } else {
                    notices.add(error(
                            "No se pudo registrar el perfil",
                            failureMessage(result.code())));
                }
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.REVISE_TAX_PROFILE::equals).isPresent()) {
                selectedId = revise(request, inputs, notices);
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.SELECT_TAX_PROFILE::equals).isPresent()) {
                clearRevisionInputs(inputs);
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.INACTIVATE_TAX_PROFILE::equals).isPresent()) {
                selectedId = changeState(
                        request, CatalogDefinitions.State.INACTIVE, notices);
            } else if (request.actionId().filter(
                    CommercialCatalogScreenContract.ACTIVATE_TAX_PROFILE::equals).isPresent()) {
                selectedId = changeState(
                        request, CatalogDefinitions.State.ACTIVE, notices);
            } else if (request.actionId().isPresent()
                    && !request.actionId().orElseThrow().equals(
                            CommercialCatalogScreenContract.TAX_PROFILE_SEARCH)
                    && !request.actionId().orElseThrow().equals(
                            CommercialCatalogScreenContract.SELECT_TAX_PROFILE)) {
                throw new IllegalArgumentException("Unsupported tax profile screen action");
            }
        } catch (IllegalArgumentException | DateTimeException invalidInput) {
            LOGGER.log(Level.WARNING,
                    "event=commercial_catalog_tax_profile_input_rejected action={0}",
                    request.actionId().map(ScreenElementId::value).orElse("none"));
            notices.add(error(
                    "Revisa los datos ingresados",
                    "Completa los campos obligatorios y usa instantes ISO-8601 válidos."));
        }

        return load(inputs, selectedId, notices);
    }

    private Optional<String> revise(
            ScreenInteraction.Request request,
            Map<ScreenElementId, String> inputs,
            List<ScreenInteraction.Notice> notices) {
        String selected = request.selectedResourceId().orElseThrow(
                () -> new IllegalArgumentException("Tax profile selection is required"));
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("Tax profile version is required"));
        CatalogOperationResult<CatalogDefinitions.TaxProfile> result =
                useCases.reviseTaxProfile(
                        context(),
                        new CatalogDefinitionCommands.ReviseTaxProfile(
                                new py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId(
                                        UUID.fromString(selected)),
                                required(inputs,
                                        CommercialCatalogScreenContract.TAX_PROFILE_REVISION_KIND),
                                required(inputs,
                                        CommercialCatalogScreenContract
                                                .TAX_PROFILE_REVISION_DESCRIPTION),
                                instant(inputs,
                                        CommercialCatalogScreenContract
                                                .TAX_PROFILE_REVISION_VALID_FROM),
                                optional(inputs,
                                        CommercialCatalogScreenContract
                                                .TAX_PROFILE_REVISION_VALID_UNTIL)
                                        .map(Instant::parse),
                                version));
        if (result.successful()) {
            CatalogDefinitions.TaxProfile revised = result.value().orElseThrow();
            clearRevisionInputs(inputs);
            notices.add(new ScreenInteraction.Notice(
                    ScreenInteraction.NoticeLevel.SUCCESS,
                    "Revisión tributaria creada",
                    "La identidad del perfil se conservó y la nueva versión quedó auditada."));
            return Optional.of(revised.id().value().toString());
        }
        notices.add(error(
                "No se pudo crear la revisión tributaria",
                failureMessage(result.code())));
        return Optional.of(selected);
    }

    private Optional<String> changeState(
            ScreenInteraction.Request request,
            CatalogDefinitions.State targetState,
            List<ScreenInteraction.Notice> notices) {
        String selected = request.selectedResourceId().orElseThrow(
                () -> new IllegalArgumentException("Tax profile selection is required"));
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("Tax profile version is required"));
        CatalogOperationResult<CatalogDefinitions.TaxProfile> result =
                useCases.changeTaxProfileState(
                        context(),
                        new CatalogDefinitionCommands.ChangeTaxProfileState(
                                new py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId(
                                        UUID.fromString(selected)),
                                targetState,
                                version));
        if (result.successful()) {
            CatalogDefinitions.TaxProfile changed = result.value().orElseThrow();
            boolean active = changed.state() == CatalogDefinitions.State.ACTIVE;
            notices.add(new ScreenInteraction.Notice(
                    ScreenInteraction.NoticeLevel.SUCCESS,
                    active ? "Perfil tributario reactivado" : "Perfil tributario inactivado",
                    active
                            ? "El perfil vuelve a estar disponible para operaciones nuevas."
                            : "El perfil deja de ofrecerse en altas nuevas y conserva sus referencias históricas."));
            return Optional.of(changed.id().value().toString());
        }
        notices.add(error(
                "No se pudo cambiar el estado del perfil",
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
            throw new IllegalStateException("Authorized catalog definitions query failed");
        }

        inputs.putIfAbsent(CommercialCatalogScreenContract.TAX_PROFILE_SEARCH_TEXT, "");
        inputs.putIfAbsent(CommercialCatalogScreenContract.TAX_PROFILE_SEARCH_STATE, ALL);
        List<CatalogDefinitions.TaxProfile> visible = result.value().orElseThrow().taxProfiles()
                .stream()
                .filter(profile -> matches(profile, inputs))
                .toList();

        Optional<CatalogDefinitions.TaxProfile> selected = selectedId.flatMap(id ->
                result.value().orElseThrow().taxProfiles().stream()
                        .filter(profile -> profile.id().value().toString().equals(id))
                        .findFirst());
        if (selectedId.isPresent() && selected.isEmpty()) {
            selectedId = Optional.empty();
            notices.add(error(
                    "Perfil tributario no disponible",
                    "La definición ya no está disponible dentro de esta empresa."));
        }
        selected.ifPresent(profile -> {
            inputs.putIfAbsent(
                    CommercialCatalogScreenContract.TAX_PROFILE_REVISION_KIND,
                    profile.internalKindCode());
            inputs.putIfAbsent(
                    CommercialCatalogScreenContract.TAX_PROFILE_REVISION_DESCRIPTION,
                    profile.description());
            inputs.putIfAbsent(
                    CommercialCatalogScreenContract.TAX_PROFILE_REVISION_VALID_FROM,
                    profile.validFrom().toString());
            inputs.putIfAbsent(
                    CommercialCatalogScreenContract.TAX_PROFILE_REVISION_VALID_UNTIL,
                    profile.validUntil().map(Instant::toString).orElse(""));
        });

        Map<ScreenElementId, List<ScreenInteraction.Option>> options = Map.of(
                CommercialCatalogScreenContract.TAX_PROFILE_SEARCH_STATE,
                List.of(
                        option(ALL, "Todos los estados"),
                        option(CatalogDefinitions.State.ACTIVE.name(), "Activos"),
                        option(CatalogDefinitions.State.INACTIVE.name(), "Inactivos")));
        ScreenInteraction.Table presentationTable;
        if (selected.isPresent()) {
            CatalogOperationResult<List<CatalogDefinitions.TaxProfileRevision>> history =
                    useCases.taxProfileHistory(operationContext, selected.orElseThrow().id());
            if (!history.successful()) {
                throw new IllegalStateException("Authorized tax profile history query failed");
            }
            presentationTable = historyTable(history.value().orElseThrow());
        } else {
            presentationTable = table(visible);
        }
        return new ScreenInteraction.Result(
                inputs,
                options,
                Optional.of(presentationTable),
                selected.map(CommercialCatalogTaxProfileScreenHandler::detail),
                notices,
                selected.map(profile -> profile.id().value().toString()),
                selected.map(CatalogDefinitions.TaxProfile::version));
    }

    private CatalogOperationContext context() {
        return CatalogOperationContext.from(authorization.require(
                CommercialCatalogPluginDefinition.ID.value(),
                CommercialCatalogPermissions.DEFINITIONS_MANAGE.value()));
    }

    private static ScreenInteraction.Table table(
            List<CatalogDefinitions.TaxProfile> profiles) {
        return new ScreenInteraction.Table(
                CommercialCatalogScreenContract.TAX_PROFILE_RESULTS,
                List.of(
                        new ScreenInteraction.Column("code", "Código"),
                        new ScreenInteraction.Column("name", "Nombre"),
                        new ScreenInteraction.Column("kind", "Tratamiento interno"),
                        new ScreenInteraction.Column("validity", "Vigencia"),
                        new ScreenInteraction.Column("state", "Estado")),
                profiles.stream().map(profile -> new ScreenInteraction.Row(
                        profile.id().value().toString(),
                        List.of(
                                profile.code(),
                                profile.displayName(),
                                profile.internalKindCode(),
                                validity(profile),
                                stateLabel(profile.state())))).toList(),
                profiles.size(),
                "No hay perfiles tributarios",
                "Registra el primer perfil interno para habilitarlo en artículos y servicios.");
    }

    private static ScreenInteraction.Table historyTable(
            List<CatalogDefinitions.TaxProfileRevision> revisions) {
        return new ScreenInteraction.Table(
                CommercialCatalogScreenContract.TAX_PROFILE_HISTORY,
                List.of(
                        new ScreenInteraction.Column("version", "Versión"),
                        new ScreenInteraction.Column("status", "Estado"),
                        new ScreenInteraction.Column("kind", "Tratamiento interno"),
                        new ScreenInteraction.Column("description", "Descripción"),
                        new ScreenInteraction.Column("validity", "Vigencia")),
                revisions.stream().map(revision -> new ScreenInteraction.Row(
                        revision.profileId().value() + ":" + revision.version(),
                        List.of(
                                Long.toString(revision.version()),
                                revision.current() ? "Actual" : "Histórica",
                                revision.internalKindCode(),
                                revision.description(),
                                validity(revision)))).toList(),
                revisions.size(),
                "No hay revisiones tributarias",
                "El perfil seleccionado no tiene revisiones disponibles para esta empresa.");
    }

    private static ScreenInteraction.Detail detail(CatalogDefinitions.TaxProfile profile) {
        return new ScreenInteraction.Detail(
                profile.id().value().toString(),
                profile.displayName(),
                List.of(
                        new ScreenInteraction.DetailItem("Código", profile.code()),
                        new ScreenInteraction.DetailItem(
                                "Tratamiento interno", profile.internalKindCode()),
                        new ScreenInteraction.DetailItem("Descripción", profile.description()),
                        new ScreenInteraction.DetailItem("Vigente desde", profile.validFrom().toString()),
                        new ScreenInteraction.DetailItem(
                                "Vigente hasta",
                                profile.validUntil().map(Instant::toString).orElse("Sin fecha final")),
                        new ScreenInteraction.DetailItem("Estado", stateLabel(profile.state())),
                        new ScreenInteraction.DetailItem(
                                "Versión", Long.toString(profile.version())),
                        new ScreenInteraction.DetailItem(
                                "Correspondencia fiscal",
                                "Pendiente del plugin fiscal; este perfil no es una regla SIFEN")));
    }

    private static boolean matches(
            CatalogDefinitions.TaxProfile profile,
            Map<ScreenElementId, String> inputs) {
        String text = optional(inputs, CommercialCatalogScreenContract.TAX_PROFILE_SEARCH_TEXT)
                .orElse("").toLowerCase(Locale.ROOT);
        String state = optional(inputs, CommercialCatalogScreenContract.TAX_PROFILE_SEARCH_STATE)
                .orElse(ALL);
        return (text.isEmpty()
                || profile.code().toLowerCase(Locale.ROOT).contains(text)
                || profile.displayName().toLowerCase(Locale.ROOT).contains(text)
                || profile.internalKindCode().toLowerCase(Locale.ROOT).contains(text))
                && (ALL.equals(state) || profile.state().name().equals(state));
    }

    private static String validity(CatalogDefinitions.TaxProfile profile) {
        return profile.validUntil()
                .map(until -> profile.validFrom() + " — " + until)
                .orElse(profile.validFrom() + " — sin fecha final");
    }

    private static String validity(CatalogDefinitions.TaxProfileRevision revision) {
        return revision.validUntil()
                .map(until -> revision.validFrom() + " — " + until)
                .orElse(revision.validFrom() + " — sin fecha final");
    }

    private static String stateLabel(CatalogDefinitions.State state) {
        return state == CatalogDefinitions.State.ACTIVE ? "Activo" : "Inactivo";
    }

    private static Instant instant(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return Instant.parse(required(inputs, field));
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
                CommercialCatalogScreenContract.TAX_PROFILE_NEW_CODE,
                CommercialCatalogScreenContract.TAX_PROFILE_NEW_NAME,
                CommercialCatalogScreenContract.TAX_PROFILE_NEW_KIND,
                CommercialCatalogScreenContract.TAX_PROFILE_NEW_DESCRIPTION,
                CommercialCatalogScreenContract.TAX_PROFILE_NEW_VALID_FROM,
                CommercialCatalogScreenContract.TAX_PROFILE_NEW_VALID_UNTIL)
                .forEach(inputs::remove);
    }

    private static void clearRevisionInputs(Map<ScreenElementId, String> inputs) {
        List.of(
                CommercialCatalogScreenContract.TAX_PROFILE_REVISION_KIND,
                CommercialCatalogScreenContract.TAX_PROFILE_REVISION_DESCRIPTION,
                CommercialCatalogScreenContract.TAX_PROFILE_REVISION_VALID_FROM,
                CommercialCatalogScreenContract.TAX_PROFILE_REVISION_VALID_UNTIL)
                .forEach(inputs::remove);
    }

    private static String failureMessage(CatalogResultCode code) {
        return switch (code) {
            case CODE_CONFLICT -> "El código ya está utilizado dentro de esta empresa.";
            case VALIDITY_CONFLICT -> "La vigencia se superpone con otra revisión activa.";
            case ACCESS_DENIED -> "La autorización actual no permite administrar definiciones.";
            case VERSION_CONFLICT -> "La definición cambió; vuelve a cargar la información.";
            case REFERENCE_CONFLICT, NOT_FOUND -> "La definición relacionada ya no está disponible.";
            case IDENTIFIER_CONFLICT, INVALID_OPERATION ->
                    "Los datos no permiten registrar el perfil tributario.";
            case SUCCESS -> throw new IllegalArgumentException("SUCCESS is not a failure");
        };
    }

    private static ScreenInteraction.Option option(String value, String label) {
        return new ScreenInteraction.Option(value, label);
    }

    private static ScreenInteraction.Notice error(String summary, String detail) {
        return new ScreenInteraction.Notice(ScreenInteraction.NoticeLevel.ERROR, summary, detail);
    }
}
