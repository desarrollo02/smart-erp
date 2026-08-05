package py.com.logixone.plugins.businesspartners.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.kernel.api.security.AuthorizedCompanyOperation;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugins.businesspartners.BusinessPartnersPluginDefinition;
import py.com.logixone.plugins.businesspartners.BusinessPartnersScreenContract;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerDefinitionUseCases;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerOperationContext;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerOperationResult;
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

class BusinessPartnerDefinitionScreenHandlerTest {

    private static final CompanyId COMPANY = new CompanyId(new UUID(0, 701));

    private RecordingAuthorization authorization;
    private FakeUseCases useCases;
    private BusinessPartnerDefinitionScreenHandler handler;

    @BeforeEach
    void setUp() {
        authorization = new RecordingAuthorization();
        useCases = new FakeUseCases();
        handler = new BusinessPartnerDefinitionScreenHandler();
        handler.authorization = authorization;
        handler.useCases = useCases;
    }

    @Test
    void loadsACompanyScopedDirectoryWithManagePermission() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.empty(), Map.of(), Optional.empty(), Optional.empty()));

        assertEquals(List.of(BusinessPartnerPermissions.MANAGE.value()),
                authorization.permissions);
        assertEquals(1, result.table().orElseThrow().rows().size());
        assertEquals(3, result.options().get(
                BusinessPartnersScreenContract.DEFINITION_SEARCH_STATE).size());
        assertEquals(4, result.options().get(
                BusinessPartnersScreenContract.DEFINITION_KIND).size());
        assertEquals(4, result.options().get(
                BusinessPartnersScreenContract.DEFINITION_NEW_KIND).size());
        assertEquals(BusinessPartnersScreenContract.DEFINITIONS, handler.screenId());
        assertEquals(3, handler.selectorSources().size());
    }

    @Test
    void registersAChannelKindThenOpensItsDetail() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(BusinessPartnersScreenContract.REGISTER_DEFINITION),
                Map.of(
                        BusinessPartnersScreenContract.DEFINITION_NEW_CODE, "telegram",
                        BusinessPartnersScreenContract.DEFINITION_NEW_NAME, "Telegram empresarial"),
                Optional.empty(),
                Optional.empty()));

        assertEquals(List.of(
                BusinessPartnerPermissions.MANAGE.value(),
                BusinessPartnerPermissions.MANAGE.value()), authorization.permissions);
        assertEquals("Telegram empresarial", result.detail().orElseThrow().title());
        assertEquals(BusinessPartnersScreenContract.DEFINITION_HISTORY,
                result.table().orElseThrow().elementId());
        assertEquals(1, result.table().orElseThrow().rows().size());
        assertTrue(result.notices().stream()
                .anyMatch(notice -> notice.level() == ScreenInteraction.NoticeLevel.SUCCESS));
        assertEquals("", result.inputs().getOrDefault(
                BusinessPartnersScreenContract.DEFINITION_NEW_CODE, ""));
    }

    @Test
    void registersAnIdentificationTypeInItsOwnDefinitionKind() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(BusinessPartnersScreenContract.REGISTER_DEFINITION),
                Map.of(
                        BusinessPartnersScreenContract.DEFINITION_KIND, "IDENTIFICATION_TYPE",
                        BusinessPartnersScreenContract.DEFINITION_NEW_CODE, "passport",
                        BusinessPartnersScreenContract.DEFINITION_NEW_NAME, "Pasaporte",
                        BusinessPartnersScreenContract.DEFINITION_NEW_KIND, "IDENTIFICATION_TYPE"),
                Optional.empty(),
                Optional.empty()));

        assertEquals("Pasaporte", result.detail().orElseThrow().title());
        assertTrue(result.detail().orElseThrow().items().stream()
                .anyMatch(item -> item.label().equals("Clase")
                        && item.value().equals("Tipo de identificación")));
        assertEquals("IDENTIFICATION_TYPE", result.inputs().get(
                BusinessPartnersScreenContract.DEFINITION_KIND));
        assertEquals("IDENTIFICATION_TYPE", result.inputs().get(
                BusinessPartnersScreenContract.DEFINITION_NEW_KIND));
        assertEquals("IDENTIFICATION_TYPE:passport",
                result.selectedResourceId().orElseThrow());
    }

    @Test
    void opensANonChannelDefinitionFromItsScopedRowIdentityWithoutFormState() {
        handler.interact(new ScreenInteraction.Request(
                Optional.of(BusinessPartnersScreenContract.REGISTER_DEFINITION),
                Map.of(
                        BusinessPartnersScreenContract.DEFINITION_NEW_CODE, "passport",
                        BusinessPartnersScreenContract.DEFINITION_NEW_NAME, "Pasaporte",
                        BusinessPartnersScreenContract.DEFINITION_NEW_KIND, "IDENTIFICATION_TYPE"),
                Optional.empty(),
                Optional.empty()));

        ScreenInteraction.Result opened = handler.interact(new ScreenInteraction.Request(
                Optional.empty(),
                Map.of(),
                Optional.of("IDENTIFICATION_TYPE:passport"),
                Optional.empty()));

        assertEquals("Pasaporte", opened.detail().orElseThrow().title());
        assertEquals("IDENTIFICATION_TYPE", opened.inputs().get(
                BusinessPartnersScreenContract.DEFINITION_KIND));
        assertEquals("IDENTIFICATION_TYPE:passport",
                opened.selectedResourceId().orElseThrow());
    }

    @Test
    void reportsDuplicateAndDoesNotReplaceTheExistingValue() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(BusinessPartnersScreenContract.REGISTER_DEFINITION),
                Map.of(
                        BusinessPartnersScreenContract.DEFINITION_NEW_CODE, "email",
                        BusinessPartnersScreenContract.DEFINITION_NEW_NAME, "Otro correo"),
                Optional.empty(),
                Optional.empty()));

        assertEquals(ScreenInteraction.NoticeLevel.ERROR, result.notices().getFirst().level());
        assertEquals("Correo electrónico", result.table().orElseThrow().rows()
                .getFirst().cells().get(1));
    }

    @Test
    void inactivatesASelectedChannelKindAndReloadsItsVersionedDetail() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(BusinessPartnersScreenContract.INACTIVATE_DEFINITION),
                Map.of(),
                Optional.of("email"),
                Optional.of(0L)));

        assertEquals("CHANNEL_KIND:email", result.selectedResourceId().orElseThrow());
        assertEquals(1, result.selectedResourceVersion().orElseThrow());
        assertTrue(result.detail().orElseThrow().items().stream()
                .anyMatch(item -> item.label().equals("Estado")
                        && item.value().equals("Inactivo")));
        assertTrue(result.notices().stream()
                .anyMatch(notice -> notice.level() == ScreenInteraction.NoticeLevel.SUCCESS));
    }

    @Test
    void reportsAStaleLifecycleChangeWithoutLosingTheSelectedDefinition() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(BusinessPartnersScreenContract.INACTIVATE_DEFINITION),
                Map.of(),
                Optional.of("email"),
                Optional.of(9L)));

        assertEquals("CHANNEL_KIND:email", result.selectedResourceId().orElseThrow());
        assertEquals(0, result.selectedResourceVersion().orElseThrow());
        assertEquals(ScreenInteraction.NoticeLevel.ERROR, result.notices().getFirst().level());
    }

    @Test
    void revisesTheVisibleNameWithoutChangingCodeAndShowsNewestHistoryFirst() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(BusinessPartnersScreenContract.REVISE_DEFINITION),
                Map.of(
                        BusinessPartnersScreenContract.DEFINITION_EDIT_NAME,
                        "Correo principal"),
                Optional.of("email"),
                Optional.of(0L)));

        assertEquals("CHANNEL_KIND:email", result.selectedResourceId().orElseThrow());
        assertEquals(1, result.selectedResourceVersion().orElseThrow());
        assertEquals("Correo principal", result.detail().orElseThrow().title());
        assertEquals("Correo principal", result.inputs().get(
                BusinessPartnersScreenContract.DEFINITION_EDIT_NAME));
        assertEquals(BusinessPartnersScreenContract.DEFINITION_HISTORY,
                result.table().orElseThrow().elementId());
        assertEquals(List.of("1", "Actual", "Correo principal", "Activo",
                        "2026-08-01T18:00:01Z"),
                result.table().orElseThrow().rows().getFirst().cells());
        assertEquals(2, result.table().orElseThrow().rows().size());
    }

    private static final class RecordingAuthorization implements CurrentCompanyAuthorization {
        private final List<String> permissions = new ArrayList<>();

        @Override
        public AuthorizedCompanyOperation require(String pluginId, String permissionId) {
            assertEquals(BusinessPartnersPluginDefinition.ID.value(), pluginId);
            permissions.add(permissionId);
            return new AuthorizedCompanyOperation(
                    new AuthenticatedCompanyContext(
                            new AuthenticatedActor(new AppUserId(new UUID(0, 702))), COMPANY),
                    pluginId,
                    permissionId,
                    "ui:definition-test");
        }
    }

    private static final class FakeUseCases implements BusinessPartnerDefinitionUseCases {
        private final List<BusinessPartnerDefinition> values = new ArrayList<>(List.of(
                BusinessPartnerDefinition.create(
                        COMPANY,
                        BusinessPartnerDefinitionKind.CHANNEL_KIND,
                        new BusinessPartnerAttributeCode("email"),
                        new BusinessPartnerName("Correo electrónico"))));
        private final Map<String, List<BusinessPartnerDefinitionRevision>> revisions =
                new HashMap<>();

        private FakeUseCases() {
            record(values.getFirst());
        }

        @Override
        public BusinessPartnerOperationResult<List<BusinessPartnerDefinition>> definitions(
                BusinessPartnerOperationContext context, BusinessPartnerDefinitionKind kind) {
            return BusinessPartnerOperationResult.success(values.stream()
                    .filter(value -> value.kind() == kind)
                    .toList(), List.of());
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerDefinition> registerDefinition(
                BusinessPartnerOperationContext context,
                RegisterBusinessPartnerDefinition command) {
            if (values.stream().anyMatch(value -> value.kind() == command.kind()
                    && value.code().equals(command.code()))) {
                return BusinessPartnerOperationResult.failure(
                        BusinessPartnerResultCode.GENERAL_CODE_CONFLICT);
            }
            BusinessPartnerDefinition created = BusinessPartnerDefinition.create(
                    COMPANY, command.kind(), command.code(), command.displayName());
            values.add(created);
            record(created);
            return BusinessPartnerOperationResult.success(created, List.of());
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerDefinition> reviseDefinition(
                BusinessPartnerOperationContext context,
                ReviseBusinessPartnerDefinition command) {
            for (int index = 0; index < values.size(); index++) {
                BusinessPartnerDefinition current = values.get(index);
                if (current.kind() == command.kind() && current.code().equals(command.code())) {
                    final BusinessPartnerDefinition revised;
                    try {
                        revised = current.reviseDisplayName(
                                command.displayName(), command.expectedVersion());
                    } catch (py.com.logixone.plugins.businesspartners.domain
                            .ConcurrentBusinessPartnerChangeException failure) {
                        return BusinessPartnerOperationResult.failure(
                                BusinessPartnerResultCode.VERSION_CONFLICT);
                    }
                    values.set(index, revised);
                    if (!revised.equals(current)) {
                        record(revised);
                    }
                    return BusinessPartnerOperationResult.success(revised, List.of());
                }
            }
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.NOT_FOUND);
        }

        @Override
        public BusinessPartnerOperationResult<List<BusinessPartnerDefinitionRevision>>
                definitionHistory(
                        BusinessPartnerOperationContext context,
                        BusinessPartnerDefinitionKind kind,
                        BusinessPartnerAttributeCode code) {
            return BusinessPartnerOperationResult.success(
                    List.copyOf(revisions.getOrDefault(key(kind, code), List.of())),
                    List.of());
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerDefinition> changeDefinitionState(
                BusinessPartnerOperationContext context,
                ChangeBusinessPartnerDefinitionState command) {
            for (int index = 0; index < values.size(); index++) {
                BusinessPartnerDefinition current = values.get(index);
                if (current.kind() == command.kind() && current.code().equals(command.code())) {
                    final BusinessPartnerDefinition changed;
                    try {
                        changed = current.changeState(
                                command.targetState(), command.expectedVersion());
                    } catch (py.com.logixone.plugins.businesspartners.domain
                            .ConcurrentBusinessPartnerChangeException failure) {
                        return BusinessPartnerOperationResult.failure(
                                BusinessPartnerResultCode.VERSION_CONFLICT);
                    }
                    values.set(index, changed);
                    if (!changed.equals(current)) {
                        record(changed);
                    }
                    return BusinessPartnerOperationResult.success(changed, List.of());
                }
            }
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.NOT_FOUND);
        }

        private void record(BusinessPartnerDefinition definition) {
            revisions.computeIfAbsent(
                            key(definition.kind(), definition.code()),
                            ignored -> new ArrayList<>())
                    .addFirst(new BusinessPartnerDefinitionRevision(
                            definition.companyId(),
                            definition.kind(),
                            definition.code(),
                            definition.displayName(),
                            definition.state(),
                            definition.version(),
                            Instant.parse("2026-08-01T18:00:00Z")
                                    .plusSeconds(definition.version())));
        }

        private static String key(
                BusinessPartnerDefinitionKind kind, BusinessPartnerAttributeCode code) {
            return kind + ":" + code.value();
        }
    }
}
