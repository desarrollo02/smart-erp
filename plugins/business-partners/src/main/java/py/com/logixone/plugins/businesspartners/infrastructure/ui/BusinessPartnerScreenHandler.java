package py.com.logixone.plugins.businesspartners.infrastructure.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugin.api.SelectorSourceDefinition;
import py.com.logixone.plugins.businesspartners.BusinessPartnersPluginDefinition;
import py.com.logixone.plugins.businesspartners.BusinessPartnersScreenContract;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerKind;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerReference;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerRole;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerOperationContext;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerOperationResult;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerDefinitionUseCases;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerPermissions;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerResultCode;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerUseCases;
import py.com.logixone.plugins.businesspartners.application.command.BusinessPartnerCommands;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchCriteria;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchPage;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAddress;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerContact;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerContactChannel;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDetailId;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerIdentification;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerName;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerSnapshot;
import py.com.logixone.plugins.referencedata.api.CountryReference;
import py.com.logixone.plugins.referencedata.api.CountryCode;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;
import py.com.logixone.plugins.referencedata.api.ReferenceDataQuery;

/** Plugin-owned interaction adapter; it provides data, never markup or EL. */
@ApplicationScoped
public class BusinessPartnerScreenHandler implements ScreenInteraction.Handler {

    private static final Logger LOGGER = System.getLogger(BusinessPartnerScreenHandler.class.getName());
    private static final int PAGE_SIZE = 20;
    private static final String ALL = "ALL";

    @Inject
    BusinessPartnerUseCases useCases;

    @Inject
    BusinessPartnerDefinitionUseCases definitionUseCases;

    @Inject
    CurrentCompanyAuthorization authorization;

    @Inject
    ReferenceDataDirectory referenceDataDirectory;

    @Override
    public ScreenId screenId() {
        return BusinessPartnersScreenContract.DIRECTORY;
    }

    @Override
    public Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
        return BusinessPartnerSelectorSources.DIRECTORY;
    }

    @Override
    public ScreenInteraction.SelectorOptionPage searchOptions(
            ScreenInteraction.SelectorOptionRequest request) {
        if (!request.elementId().equals(BusinessPartnersScreenContract.IDENTIFICATION_COUNTRY)) {
            throw new IllegalArgumentException("Unsupported business-partner selector source");
        }
        BusinessPartnerOperationContext viewContext = context(BusinessPartnerPermissions.VIEW);
        var page = referenceDataDirectory.searchCountries(
                viewContext.companyContext().companyId(),
                new ReferenceDataQuery(
                        request.query(), request.offset(), request.limit(), true));
        return new ScreenInteraction.SelectorOptionPage(
                page.entries().stream()
                        .map(country -> new ScreenInteraction.Option(
                                country.code().value(),
                                country.displayName() + " · " + country.code().value()))
                        .toList(),
                page.total(),
                page.offset(),
                page.limit());
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = defaults(request.inputs());
        Optional<String> selectedId = request.selectedResourceId();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();

        try {
            if (request.actionId().isPresent()) {
                ScreenElementId action = request.actionId().orElseThrow();
                if (action.equals(BusinessPartnersScreenContract.SEARCH)) {
                    selectedId = Optional.empty();
                } else if (!action.equals(BusinessPartnersScreenContract.SELECT_PARTNER)) {
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
                    "event=business_partner_screen_input_rejected action={0} input_keys={1}",
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
        if (action.equals(BusinessPartnersScreenContract.REGISTER)) {
            var command = new BusinessPartnerCommands.Register(
                    optional(inputs, BusinessPartnersScreenContract.NEW_CODE).map(BusinessPartnerCode::new),
                    enumValue(inputs, BusinessPartnersScreenContract.NEW_KIND, BusinessPartnerKind.class),
                    new BusinessPartnerName(required(inputs, BusinessPartnersScreenContract.NEW_DISPLAY_NAME)),
                    optional(inputs, BusinessPartnersScreenContract.NEW_LEGAL_NAME).map(BusinessPartnerName::new),
                    optional(inputs, BusinessPartnersScreenContract.NEW_TRADE_NAME).map(BusinessPartnerName::new));
            return mutation(useCases.register(context(BusinessPartnerPermissions.MANAGE), command),
                    "Socio comercial registrado");
        }

        BusinessPartnerId id = BusinessPartnerId.parse(
                request.selectedResourceId().orElseThrow(
                        () -> new IllegalArgumentException("A selected partner is required")));
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("A selected partner version is required"));

        if (action.equals(BusinessPartnersScreenContract.CHANGE_CODE)) {
            return mutation(useCases.changeCode(
                    context(BusinessPartnerPermissions.MANAGE),
                    new BusinessPartnerCommands.ChangeCode(
                            id,
                            version,
                            new BusinessPartnerCode(required(
                                    inputs, BusinessPartnersScreenContract.EDIT_CODE)))),
                    "Código actualizado", id);
        }
        if (action.equals(BusinessPartnersScreenContract.RENAME)) {
            return mutation(useCases.rename(
                    context(BusinessPartnerPermissions.MANAGE),
                    new BusinessPartnerCommands.Rename(
                            id,
                            version,
                            new BusinessPartnerName(required(
                                    inputs, BusinessPartnersScreenContract.EDIT_DISPLAY_NAME)),
                            optional(inputs, BusinessPartnersScreenContract.EDIT_LEGAL_NAME)
                                    .map(BusinessPartnerName::new),
                            optional(inputs, BusinessPartnersScreenContract.EDIT_TRADE_NAME)
                                    .map(BusinessPartnerName::new))),
                    "Nombres actualizados", id);
        }
        if (action.equals(BusinessPartnersScreenContract.ADD_IDENTIFICATION)) {
            var identification = BusinessPartnerIdentification.create(
                    detailId(),
                    new BusinessPartnerAttributeCode(required(
                            inputs, BusinessPartnersScreenContract.IDENTIFICATION_TYPE)),
                    optional(inputs, BusinessPartnersScreenContract.IDENTIFICATION_COUNTRY),
                    required(inputs, BusinessPartnersScreenContract.IDENTIFICATION_VALUE),
                    Optional.empty(),
                    Optional.empty());
            return mutation(useCases.addIdentification(
                    context(BusinessPartnerPermissions.MANAGE),
                    new BusinessPartnerCommands.AddIdentification(id, version, identification)),
                    "Identificación agregada", id);
        }
        if (action.equals(BusinessPartnersScreenContract.ADD_ADDRESS)) {
            var address = new BusinessPartnerAddress(
                    detailId(),
                    new BusinessPartnerAttributeCode(required(
                            inputs, BusinessPartnersScreenContract.ADDRESS_TYPE)),
                    new BusinessPartnerAttributeCode(required(
                            inputs, BusinessPartnersScreenContract.ADDRESS_PURPOSE)),
                    required(inputs, BusinessPartnersScreenContract.ADDRESS_LINE),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    optional(inputs, BusinessPartnersScreenContract.ADDRESS_LOCALITY),
                    true,
                    false);
            return mutation(useCases.addAddress(
                    context(BusinessPartnerPermissions.MANAGE),
                    new BusinessPartnerCommands.AddAddress(id, version, address)),
                    "Dirección agregada", id);
        }
        if (action.equals(BusinessPartnersScreenContract.ADD_CHANNEL)) {
            var channel = new BusinessPartnerContactChannel(
                    detailId(),
                    new BusinessPartnerAttributeCode(required(
                            inputs, BusinessPartnersScreenContract.CHANNEL_KIND)),
                    new BusinessPartnerAttributeCode("general"),
                    required(inputs, BusinessPartnersScreenContract.CHANNEL_VALUE),
                    true,
                    false);
            return mutation(useCases.addChannel(
                    context(BusinessPartnerPermissions.MANAGE),
                    new BusinessPartnerCommands.AddChannel(id, version, channel)),
                    "Canal de contacto agregado", id);
        }
        if (action.equals(BusinessPartnersScreenContract.ADD_CONTACT)) {
            var contact = new BusinessPartnerContact(
                    detailId(),
                    new BusinessPartnerName(required(
                            inputs, BusinessPartnersScreenContract.CONTACT_NAME)),
                    optional(inputs, BusinessPartnersScreenContract.CONTACT_POSITION)
                            .map(BusinessPartnerName::new),
                    List.of(),
                    true);
            return mutation(useCases.addContact(
                    context(BusinessPartnerPermissions.MANAGE),
                    new BusinessPartnerCommands.AddContact(id, version, contact)),
                    "Contacto agregado", id);
        }
        if (action.equals(BusinessPartnersScreenContract.ASSIGN_CLIENT)) {
            return assignRole(id, version, BusinessPartnerRole.CLIENT);
        }
        if (action.equals(BusinessPartnersScreenContract.ASSIGN_SUPPLIER)) {
            return assignRole(id, version, BusinessPartnerRole.SUPPLIER);
        }
        if (action.equals(BusinessPartnersScreenContract.ACTIVATE_CLIENT)) {
            return changeRole(id, version, BusinessPartnerRole.CLIENT, BusinessPartnerState.ACTIVE);
        }
        if (action.equals(BusinessPartnersScreenContract.DEACTIVATE_CLIENT)) {
            return changeRole(id, version, BusinessPartnerRole.CLIENT, BusinessPartnerState.INACTIVE);
        }
        if (action.equals(BusinessPartnersScreenContract.ACTIVATE_SUPPLIER)) {
            return changeRole(id, version, BusinessPartnerRole.SUPPLIER, BusinessPartnerState.ACTIVE);
        }
        if (action.equals(BusinessPartnersScreenContract.DEACTIVATE_SUPPLIER)) {
            return changeRole(id, version, BusinessPartnerRole.SUPPLIER, BusinessPartnerState.INACTIVE);
        }
        if (action.equals(BusinessPartnersScreenContract.DEACTIVATE_PARTNER)) {
            return lifecycle(id, version, BusinessPartnerState.INACTIVE);
        }
        if (action.equals(BusinessPartnersScreenContract.REACTIVATE_PARTNER)) {
            return lifecycle(id, version, BusinessPartnerState.ACTIVE);
        }
        throw new IllegalArgumentException("Unsupported screen action");
    }

    private Mutation assignRole(
            BusinessPartnerId id, long version, BusinessPartnerRole role) {
        return mutation(useCases.assignRole(
                context(BusinessPartnerPermissions.ROLES_MANAGE),
                new BusinessPartnerCommands.AssignRole(id, version, role, Optional.empty())),
                role == BusinessPartnerRole.CLIENT
                        ? "Rol cliente asignado"
                        : "Rol proveedor asignado", id);
    }

    private Mutation changeRole(
            BusinessPartnerId id,
            long version,
            BusinessPartnerRole role,
            BusinessPartnerState state) {
        return mutation(useCases.changeRoleState(
                context(BusinessPartnerPermissions.ROLES_MANAGE),
                new BusinessPartnerCommands.ChangeRoleState(id, version, role, state)),
                "Estado del rol actualizado", id);
    }

    private Mutation lifecycle(
            BusinessPartnerId id, long version, BusinessPartnerState state) {
        return mutation(useCases.changeLifecycle(
                context(BusinessPartnerPermissions.LIFECYCLE_MANAGE),
                new BusinessPartnerCommands.ChangeLifecycle(id, version, state)),
                state == BusinessPartnerState.ACTIVE
                        ? "Socio comercial reactivado"
                        : "Socio comercial inactivado", id);
    }

    private Mutation mutation(
            BusinessPartnerOperationResult<BusinessPartnerSnapshot> result,
            String successSummary) {
        return mutation(result, successSummary, null);
    }

    private Mutation mutation(
            BusinessPartnerOperationResult<BusinessPartnerSnapshot> result,
            String successSummary,
            BusinessPartnerId fallbackId) {
        if (!result.successful()) {
            return new Mutation(
                    Optional.ofNullable(fallbackId).map(BusinessPartnerId::toString),
                    List.of(error("No se pudo completar la operación", failureMessage(result.code()))),
                    false);
        }
        BusinessPartnerSnapshot snapshot = result.value().orElseThrow();
        List<ScreenInteraction.Notice> notices = new ArrayList<>();
        notices.add(new ScreenInteraction.Notice(
                ScreenInteraction.NoticeLevel.SUCCESS,
                successSummary,
                "El cambio fue confirmado y auditado por el servidor."));
        if (!result.warnings().isEmpty()) {
            notices.add(new ScreenInteraction.Notice(
                    ScreenInteraction.NoticeLevel.WARNING,
                    "Posible identificación duplicada",
                    "El alta se conservó y requiere revisión humana; no se fusionaron registros."));
        }
        return new Mutation(Optional.of(snapshot.id().toString()), notices, true);
    }

    private ScreenInteraction.Result load(
            Map<ScreenElementId, String> inputs,
            Optional<String> selectedId,
            List<ScreenInteraction.Notice> notices) {
        String stage = "authorization";
        try {
            BusinessPartnerOperationContext viewContext = context(BusinessPartnerPermissions.VIEW);
            stage = "reference-countries";
            List<CountryReference> countries = selectedCountry(
                    viewContext, inputs).stream().toList();
            stage = "company-definitions";
            List<BusinessPartnerDefinition> activeChannelKinds = activeDefinitions(
                    viewContext, BusinessPartnerDefinitionKind.CHANNEL_KIND);
            List<BusinessPartnerDefinition> activeIdentificationTypes = activeDefinitions(
                    viewContext, BusinessPartnerDefinitionKind.IDENTIFICATION_TYPE);
            List<BusinessPartnerDefinition> activeAddressTypes = activeDefinitions(
                    viewContext, BusinessPartnerDefinitionKind.ADDRESS_TYPE);
            List<BusinessPartnerDefinition> activeAddressPurposes = activeDefinitions(
                    viewContext, BusinessPartnerDefinitionKind.ADDRESS_PURPOSE);
            normalizeSelection(inputs, BusinessPartnersScreenContract.CHANNEL_KIND,
                    activeChannelKinds);
            normalizeSelection(inputs, BusinessPartnersScreenContract.IDENTIFICATION_TYPE,
                    activeIdentificationTypes);
            normalizeSelection(inputs, BusinessPartnersScreenContract.ADDRESS_TYPE,
                    activeAddressTypes);
            normalizeSelection(inputs, BusinessPartnersScreenContract.ADDRESS_PURPOSE,
                    activeAddressPurposes);
            stage = "criteria";
            BusinessPartnerSearchCriteria criteria = new BusinessPartnerSearchCriteria(
                    filter(inputs, BusinessPartnersScreenContract.SEARCH_TEXT),
                    filterEnum(inputs, BusinessPartnersScreenContract.SEARCH_ROLE, BusinessPartnerRole.class),
                    filterEnum(inputs, BusinessPartnersScreenContract.SEARCH_STATE, BusinessPartnerState.class),
                    0,
                    PAGE_SIZE);
            stage = "search";
            BusinessPartnerOperationResult<BusinessPartnerSearchPage> search =
                    useCases.search(viewContext, criteria);
            if (!search.successful()) {
                throw new IllegalStateException("Authorized business partner search failed");
            }

            stage = "result";
            BusinessPartnerSearchPage page = search.value().orElseThrow();
            ScreenInteraction.Table table = table(page);
            Optional<ScreenInteraction.Detail> detail = Optional.empty();
            Optional<Long> selectedVersion = Optional.empty();
            if (selectedId.isPresent()) {
                BusinessPartnerId id = BusinessPartnerId.parse(selectedId.orElseThrow());
                BusinessPartnerOperationResult<BusinessPartnerSnapshot> found =
                        useCases.detail(viewContext, id);
                if (found.successful()) {
                    BusinessPartnerSnapshot snapshot = found.value().orElseThrow();
                    populateEditableValues(inputs, snapshot);
                    detail = Optional.of(detail(snapshot));
                    selectedVersion = Optional.of(snapshot.version());
                } else {
                    selectedId = Optional.empty();
                    notices.add(error(
                            "Socio comercial no disponible",
                            "El registro ya no existe o dejó de estar disponible en esta empresa."));
                }
            }

            return new ScreenInteraction.Result(
                    inputs,
                    options(
                            activeChannelKinds,
                            activeIdentificationTypes,
                            countries,
                            activeAddressTypes,
                            activeAddressPurposes),
                    Optional.of(table),
                    detail,
                    notices,
                    selectedId,
                    selectedVersion);
        } catch (RuntimeException failure) {
            LOGGER.log(Level.ERROR,
                    "event=business_partner_screen_load_failed stage={0} type={1}",
                    stage,
                    failure.getClass().getName());
            throw failure;
        }
    }

    private BusinessPartnerOperationContext context(ContributionId permission) {
        return BusinessPartnerOperationContext.from(authorization.require(
                BusinessPartnersPluginDefinition.ID.value(), permission.value()));
    }

    private static ScreenInteraction.Table table(BusinessPartnerSearchPage page) {
        List<ScreenInteraction.Column> columns = List.of(
                new ScreenInteraction.Column("code", "Código"),
                new ScreenInteraction.Column("name", "Nombre"),
                new ScreenInteraction.Column("kind", "Tipo"),
                new ScreenInteraction.Column("roles", "Roles"),
                new ScreenInteraction.Column("state", "Estado"));
        List<ScreenInteraction.Row> rows = page.items().stream()
                .map(BusinessPartnerScreenHandler::row)
                .toList();
        return new ScreenInteraction.Table(
                BusinessPartnersScreenContract.RESULTS,
                columns,
                rows,
                page.total(),
                "No encontramos socios comerciales",
                "Ajusta los filtros o registra el primer participante de esta empresa.");
    }

    private static ScreenInteraction.Row row(BusinessPartnerReference reference) {
        String roles = reference.roles().isEmpty()
                ? "Sin rol comercial"
                : reference.roles().stream().map(BusinessPartnerScreenHandler::roleLabel)
                        .sorted().reduce((left, right) -> left + ", " + right).orElseThrow();
        return new ScreenInteraction.Row(reference.id().toString(), List.of(
                reference.code(),
                reference.displayName(),
                kindLabel(reference.kind()),
                roles,
                stateLabel(reference.state())));
    }

    private static ScreenInteraction.Detail detail(BusinessPartnerSnapshot snapshot) {
        String roles = snapshot.roles().isEmpty()
                ? "Sin roles"
                : snapshot.roles().stream()
                        .map(role -> roleLabel(role.type()) + " · " + stateLabel(role.state()))
                        .reduce((left, right) -> left + ", " + right)
                        .orElseThrow();
        return new ScreenInteraction.Detail(
                snapshot.id().toString(),
                snapshot.displayName().value(),
                List.of(
                        new ScreenInteraction.DetailItem("Código", snapshot.code().value()),
                        new ScreenInteraction.DetailItem("Tipo", kindLabel(snapshot.kind())),
                        new ScreenInteraction.DetailItem("Estado", stateLabel(snapshot.state())),
                        new ScreenInteraction.DetailItem("Roles", roles),
                        new ScreenInteraction.DetailItem(
                                "Identificaciones", Integer.toString(snapshot.identifications().size())),
                        new ScreenInteraction.DetailItem(
                                "Direcciones", Integer.toString(snapshot.addresses().size())),
                        new ScreenInteraction.DetailItem(
                                "Canales", Integer.toString(snapshot.channels().size())),
                        new ScreenInteraction.DetailItem(
                                "Contactos", Integer.toString(snapshot.contacts().size())),
                        new ScreenInteraction.DetailItem("Versión", Long.toString(snapshot.version()))));
    }

    private static Map<ScreenElementId, List<ScreenInteraction.Option>> options(
            List<BusinessPartnerDefinition> channelKinds,
            List<BusinessPartnerDefinition> identificationTypes,
            List<CountryReference> countries,
            List<BusinessPartnerDefinition> addressTypes,
            List<BusinessPartnerDefinition> addressPurposes) {
        Map<ScreenElementId, List<ScreenInteraction.Option>> options = new LinkedHashMap<>();
        options.put(BusinessPartnersScreenContract.SEARCH_ROLE, List.of(
                new ScreenInteraction.Option(ALL, "Todos los roles"),
                new ScreenInteraction.Option(BusinessPartnerRole.CLIENT.name(), "Cliente"),
                new ScreenInteraction.Option(BusinessPartnerRole.SUPPLIER.name(), "Proveedor")));
        options.put(BusinessPartnersScreenContract.SEARCH_STATE, List.of(
                new ScreenInteraction.Option(ALL, "Todos los estados"),
                new ScreenInteraction.Option(BusinessPartnerState.ACTIVE.name(), "Activo"),
                new ScreenInteraction.Option(BusinessPartnerState.INACTIVE.name(), "Inactivo")));
        options.put(BusinessPartnersScreenContract.NEW_KIND, List.of(
                new ScreenInteraction.Option(BusinessPartnerKind.ORGANIZATION.name(), "Organización"),
                new ScreenInteraction.Option(BusinessPartnerKind.NATURAL_PERSON.name(), "Persona física")));
        options.put(BusinessPartnersScreenContract.CHANNEL_KIND, channelKinds.stream()
                .map(BusinessPartnerScreenHandler::option).toList());
        options.put(BusinessPartnersScreenContract.IDENTIFICATION_TYPE,
                identificationTypes.stream().map(BusinessPartnerScreenHandler::option).toList());
        options.put(BusinessPartnersScreenContract.IDENTIFICATION_COUNTRY,
                countries.stream()
                        .map(country -> new ScreenInteraction.Option(
                                country.code().value(), country.displayName()))
                        .toList());
        options.put(BusinessPartnersScreenContract.ADDRESS_TYPE,
                addressTypes.stream().map(BusinessPartnerScreenHandler::option).toList());
        options.put(BusinessPartnersScreenContract.ADDRESS_PURPOSE,
                addressPurposes.stream().map(BusinessPartnerScreenHandler::option).toList());
        return Map.copyOf(options);
    }

    private List<BusinessPartnerDefinition> activeDefinitions(
            BusinessPartnerOperationContext context, BusinessPartnerDefinitionKind kind) {
        BusinessPartnerOperationResult<List<BusinessPartnerDefinition>> result =
                definitionUseCases.definitions(context, kind);
        if (!result.successful()) {
            throw new IllegalStateException("Authorized definition query failed for " + kind);
        }
        return result.value().orElseThrow().stream()
                .filter(value -> value.state() == BusinessPartnerState.ACTIVE)
                .toList();
    }

    private static ScreenInteraction.Option option(BusinessPartnerDefinition definition) {
        return new ScreenInteraction.Option(
                definition.code().value(), definition.displayName().value());
    }

    private static void normalizeSelection(
            Map<ScreenElementId, String> inputs,
            ScreenElementId field,
            List<BusinessPartnerDefinition> definitions) {
        String selected = inputs.get(field);
        boolean valid = definitions.stream()
                .anyMatch(value -> value.code().value().equals(selected));
        if (!valid) {
            if (definitions.isEmpty()) {
                inputs.remove(field);
            } else {
                inputs.put(field, definitions.getFirst().code().value());
            }
        }
    }

    private Optional<CountryReference> selectedCountry(
            BusinessPartnerOperationContext context,
            Map<ScreenElementId, String> inputs) {
        String selected = inputs.get(BusinessPartnersScreenContract.IDENTIFICATION_COUNTRY);
        if (selected == null || selected.isBlank()) {
            return Optional.empty();
        }
        Optional<CountryReference> found;
        try {
            found = referenceDataDirectory.findCountry(
                    context.companyContext().companyId(), new CountryCode(selected));
        } catch (IllegalArgumentException invalid) {
            found = Optional.empty();
        }
        if (found.isEmpty() || !found.orElseThrow().enabled()) {
            inputs.remove(BusinessPartnersScreenContract.IDENTIFICATION_COUNTRY);
            return Optional.empty();
        }
        return found;
    }

    private static Map<ScreenElementId, String> defaults(
            Map<ScreenElementId, String> submitted) {
        Map<ScreenElementId, String> inputs = new HashMap<>(submitted);
        inputs.putIfAbsent(BusinessPartnersScreenContract.SEARCH_ROLE, ALL);
        inputs.putIfAbsent(BusinessPartnersScreenContract.SEARCH_STATE, ALL);
        inputs.putIfAbsent(
                BusinessPartnersScreenContract.NEW_KIND, BusinessPartnerKind.ORGANIZATION.name());
        return inputs;
    }

    private static void populateEditableValues(
            Map<ScreenElementId, String> inputs, BusinessPartnerSnapshot snapshot) {
        inputs.put(BusinessPartnersScreenContract.EDIT_CODE, snapshot.code().value());
        inputs.put(BusinessPartnersScreenContract.EDIT_DISPLAY_NAME, snapshot.displayName().value());
        inputs.put(BusinessPartnersScreenContract.EDIT_LEGAL_NAME,
                snapshot.legalName().map(BusinessPartnerName::value).orElse(""));
        inputs.put(BusinessPartnersScreenContract.EDIT_TRADE_NAME,
                snapshot.tradeName().map(BusinessPartnerName::value).orElse(""));
    }

    private static void clearMutationInputs(
            ScreenElementId action, Map<ScreenElementId, String> inputs) {
        if (action.equals(BusinessPartnersScreenContract.REGISTER)) {
            clear(inputs,
                    BusinessPartnersScreenContract.NEW_CODE,
                    BusinessPartnersScreenContract.NEW_DISPLAY_NAME,
                    BusinessPartnersScreenContract.NEW_LEGAL_NAME,
                    BusinessPartnersScreenContract.NEW_TRADE_NAME);
        } else if (action.equals(BusinessPartnersScreenContract.ADD_IDENTIFICATION)) {
            clear(inputs,
                    BusinessPartnersScreenContract.IDENTIFICATION_COUNTRY,
                    BusinessPartnersScreenContract.IDENTIFICATION_VALUE);
        } else if (action.equals(BusinessPartnersScreenContract.ADD_ADDRESS)) {
            clear(inputs,
                    BusinessPartnersScreenContract.ADDRESS_LINE,
                    BusinessPartnersScreenContract.ADDRESS_LOCALITY);
        } else if (action.equals(BusinessPartnersScreenContract.ADD_CHANNEL)) {
            clear(inputs, BusinessPartnersScreenContract.CHANNEL_VALUE);
        } else if (action.equals(BusinessPartnersScreenContract.ADD_CONTACT)) {
            clear(inputs,
                    BusinessPartnersScreenContract.CONTACT_NAME,
                    BusinessPartnersScreenContract.CONTACT_POSITION);
        }
    }

    private static void clear(
            Map<ScreenElementId, String> inputs, ScreenElementId... fields) {
        for (ScreenElementId field : fields) {
            inputs.remove(field);
        }
    }

    private static String required(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return optional(inputs, field).orElseThrow(
                () -> new IllegalArgumentException("Missing required screen value"));
    }

    private static Optional<String> optional(
            Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return Optional.ofNullable(inputs.get(field)).map(String::strip).filter(value -> !value.isEmpty());
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

    private static BusinessPartnerDetailId detailId() {
        return new BusinessPartnerDetailId(UUID.randomUUID());
    }

    private static String kindLabel(BusinessPartnerKind kind) {
        return kind == BusinessPartnerKind.ORGANIZATION ? "Organización" : "Persona física";
    }

    private static String roleLabel(BusinessPartnerRole role) {
        return role == BusinessPartnerRole.CLIENT ? "Cliente" : "Proveedor";
    }

    private static String stateLabel(BusinessPartnerState state) {
        return state == BusinessPartnerState.ACTIVE ? "Activo" : "Inactivo";
    }

    private static String failureMessage(BusinessPartnerResultCode code) {
        return switch (code) {
            case VERSION_CONFLICT -> "El registro cambió desde que fue abierto. Revisa la versión actual.";
            case GENERAL_CODE_CONFLICT -> "El código ya está utilizado dentro de esta empresa.";
            case ROLE_CODE_CONFLICT -> "El código del rol ya está utilizado dentro de esta empresa.";
            case NOT_FOUND -> "El socio comercial ya no está disponible.";
            case ACCESS_DENIED -> "La autorización actual no permite esta operación.";
            case INVALID_OPERATION -> "El estado actual no admite la operación solicitada.";
            case SUCCESS -> throw new IllegalArgumentException("SUCCESS is not a failure");
        };
    }

    private static ScreenInteraction.Notice error(String summary, String detail) {
        return new ScreenInteraction.Notice(
                ScreenInteraction.NoticeLevel.ERROR, summary, detail);
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
