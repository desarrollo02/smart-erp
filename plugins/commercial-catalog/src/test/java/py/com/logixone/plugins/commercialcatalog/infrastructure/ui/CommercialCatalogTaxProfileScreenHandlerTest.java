package py.com.logixone.plugins.commercialcatalog.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
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
import py.com.logixone.plugins.commercialcatalog.CommercialCatalogScreenContract;
import py.com.logixone.plugins.commercialcatalog.application.CatalogOperationResult;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogPermissions;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogUseCases;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogDefinitionCommands;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;

class CommercialCatalogTaxProfileScreenHandlerTest {

    private static final CompanyId COMPANY = new CompanyId(new UUID(0, 101));
    private RecordingAuthorization authorization;
    private RecordingUseCases recording;
    private CommercialCatalogTaxProfileScreenHandler handler;

    @BeforeEach
    void setUp() {
        authorization = new RecordingAuthorization();
        recording = new RecordingUseCases();
        handler = new CommercialCatalogTaxProfileScreenHandler();
        handler.authorization = authorization;
        handler.useCases = recording.proxy();
    }

    @Test
    void loadsAndFiltersProfilesWithDefinitionsPermission() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.empty(),
                Map.of(CommercialCatalogScreenContract.TAX_PROFILE_SEARCH_TEXT, "reducido"),
                Optional.empty(), Optional.empty()));

        assertEquals(List.of(CommercialCatalogPermissions.DEFINITIONS_MANAGE.value()),
                authorization.permissions);
        assertEquals(1, result.table().orElseThrow().rows().size());
        assertEquals("IVA reducido demo", result.table().orElseThrow().rows()
                .getFirst().cells().get(1));
    }

    @Test
    void registrationUsesDedicatedPermissionAndOpensCreatedDetail() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REGISTER_TAX_PROFILE),
                Map.of(
                        CommercialCatalogScreenContract.TAX_PROFILE_NEW_CODE, "EXENTO_DEMO",
                        CommercialCatalogScreenContract.TAX_PROFILE_NEW_NAME, "Exento demo",
                        CommercialCatalogScreenContract.TAX_PROFILE_NEW_KIND, "EXEMPT",
                        CommercialCatalogScreenContract.TAX_PROFILE_NEW_DESCRIPTION,
                                "Ejemplo interno sin equivalencia fiscal certificada",
                        CommercialCatalogScreenContract.TAX_PROFILE_NEW_VALID_FROM,
                                "2026-08-01T00:00:00Z"),
                Optional.empty(), Optional.empty()));

        assertEquals(List.of(
                        CommercialCatalogPermissions.DEFINITIONS_MANAGE.value(),
                        CommercialCatalogPermissions.DEFINITIONS_MANAGE.value()),
                authorization.permissions);
        assertTrue(recording.invocations.contains("registerTaxProfile"));
        assertEquals("Exento demo", result.detail().orElseThrow().title());
        assertTrue(result.notices().stream().anyMatch(
                notice -> notice.level() == ScreenInteraction.NoticeLevel.SUCCESS));
    }

    @Test
    void invalidValidityRemainsAComprehensibleScreenError() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REGISTER_TAX_PROFILE),
                Map.of(
                        CommercialCatalogScreenContract.TAX_PROFILE_NEW_CODE, "INVALID",
                        CommercialCatalogScreenContract.TAX_PROFILE_NEW_NAME, "Perfil inválido",
                        CommercialCatalogScreenContract.TAX_PROFILE_NEW_KIND, "TAXED_DEMO",
                        CommercialCatalogScreenContract.TAX_PROFILE_NEW_DESCRIPTION,
                                "Entrada ficticia para validar el límite",
                        CommercialCatalogScreenContract.TAX_PROFILE_NEW_VALID_FROM,
                                "01/08/2026"),
                Optional.empty(), Optional.empty()));

        assertTrue(recording.invocations.stream().noneMatch("registerTaxProfile"::equals));
        assertEquals(ScreenInteraction.NoticeLevel.ERROR, result.notices().getFirst().level());
        assertTrue(result.notices().getFirst().detail().contains("ISO-8601"));
    }

    @Test
    void inactivatesAndReactivatesTheSelectedProfileWithItsCurrentVersion() {
        String id = new UUID(0, 301).toString();
        ScreenInteraction.Result inactive = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.INACTIVATE_TAX_PROFILE),
                Map.of(), Optional.of(id), Optional.of(0L)));
        ScreenInteraction.Result active = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.ACTIVATE_TAX_PROFILE),
                Map.of(), Optional.of(id), inactive.selectedResourceVersion()));

        assertTrue(recording.invocations.stream()
                .filter("changeTaxProfileState"::equals).count() == 2);
        assertEquals(1L, inactive.selectedResourceVersion().orElseThrow());
        assertEquals(2L, active.selectedResourceVersion().orElseThrow());
        assertTrue(inactive.detail().orElseThrow().items().stream().anyMatch(item ->
                item.label().equals("Estado") && item.value().equals("Inactivo")));
        assertTrue(active.detail().orElseThrow().items().stream().anyMatch(item ->
                item.label().equals("Estado") && item.value().equals("Activo")));
        assertTrue(active.notices().stream().anyMatch(notice ->
                notice.summary().equals("Perfil tributario reactivado")));
    }

    @Test
    void createsAnExplicitRevisionWithoutChangingTheSelectedProfileIdentity() {
        String id = new UUID(0, 301).toString();
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REVISE_TAX_PROFILE),
                Map.of(
                        CommercialCatalogScreenContract.TAX_PROFILE_REVISION_KIND,
                                "TAXED_STANDARD_V2",
                        CommercialCatalogScreenContract.TAX_PROFILE_REVISION_DESCRIPTION,
                                "Tratamiento interno revisado",
                        CommercialCatalogScreenContract.TAX_PROFILE_REVISION_VALID_FROM,
                                "2026-09-01T00:00:00Z",
                        CommercialCatalogScreenContract.TAX_PROFILE_REVISION_VALID_UNTIL,
                                "2026-12-31T23:59:59Z"),
                Optional.of(id), Optional.of(0L)));

        assertTrue(recording.invocations.contains("reviseTaxProfile"));
        assertEquals(id, result.selectedResourceId().orElseThrow());
        assertEquals(1L, result.selectedResourceVersion().orElseThrow());
        assertEquals("IVA10_DEMO", result.detail().orElseThrow().items().getFirst().value());
        assertTrue(result.detail().orElseThrow().items().stream().anyMatch(item ->
                item.label().equals("Tratamiento interno")
                        && item.value().equals("TAXED_STANDARD_V2")));
        assertEquals("TAXED_STANDARD_V2",
                result.inputs().get(
                        CommercialCatalogScreenContract.TAX_PROFILE_REVISION_KIND));
        assertTrue(recording.invocations.contains("taxProfileHistory"));
        assertEquals(CommercialCatalogScreenContract.TAX_PROFILE_HISTORY,
                result.table().orElseThrow().elementId());
        assertEquals(2, result.table().orElseThrow().rows().size());
        assertEquals(List.of("1", "Actual", "TAXED_STANDARD_V2"),
                result.table().orElseThrow().rows().getFirst().cells().subList(0, 3));
        assertEquals("Histórica",
                result.table().orElseThrow().rows().getLast().cells().get(1));
        assertTrue(result.notices().stream().anyMatch(notice ->
                notice.summary().equals("Revisión tributaria creada")));
    }

    @Test
    void invalidRevisionKeepsTheSelectionAndSafeDraft() {
        String id = new UUID(0, 301).toString();
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REVISE_TAX_PROFILE),
                Map.of(
                        CommercialCatalogScreenContract.TAX_PROFILE_REVISION_KIND,
                                "TAXED_STANDARD_V2",
                        CommercialCatalogScreenContract.TAX_PROFILE_REVISION_DESCRIPTION,
                                "Borrador preservado",
                        CommercialCatalogScreenContract.TAX_PROFILE_REVISION_VALID_FROM,
                                "fecha-invalida"),
                Optional.of(id), Optional.of(0L)));

        assertTrue(recording.invocations.stream().noneMatch("reviseTaxProfile"::equals));
        assertEquals(id, result.selectedResourceId().orElseThrow());
        assertEquals("fecha-invalida", result.inputs().get(
                CommercialCatalogScreenContract.TAX_PROFILE_REVISION_VALID_FROM));
        assertEquals(ScreenInteraction.NoticeLevel.ERROR, result.notices().getFirst().level());
    }

    private static final class RecordingAuthorization implements CurrentCompanyAuthorization {
        private final List<String> permissions = new ArrayList<>();

        @Override
        public AuthorizedCompanyOperation require(String pluginId, String permissionId) {
            permissions.add(permissionId);
            return new AuthorizedCompanyOperation(
                    new AuthenticatedCompanyContext(
                            new AuthenticatedActor(new AppUserId(new UUID(0, 501))), COMPANY),
                    pluginId, permissionId, "ui:tax-profile-test");
        }
    }

    private static final class RecordingUseCases implements InvocationHandler {
        private final List<String> invocations = new ArrayList<>();
        private final List<CatalogDefinitions.TaxProfile> profiles = new ArrayList<>(List.of(
                profile(new UUID(0, 301), "IVA10_DEMO", "IVA general demo", "TAXED_STANDARD"),
                profile(new UUID(0, 302), "IVA5_DEMO", "IVA reducido demo", "TAXED_REDUCED")));
        private final List<CatalogDefinitions.TaxProfileRevision> revisions = new ArrayList<>();

        private RecordingUseCases() {
            profiles.forEach(this::recordRevision);
        }

        CommercialCatalogUseCases proxy() {
            return (CommercialCatalogUseCases) Proxy.newProxyInstance(
                    CommercialCatalogUseCases.class.getClassLoader(),
                    new Class<?>[]{CommercialCatalogUseCases.class}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            invocations.add(method.getName());
            return switch (method.getName()) {
                case "managedDefinitions" -> CatalogOperationResult.success(snapshot());
                case "registerTaxProfile" -> register(
                        (CatalogDefinitionCommands.RegisterTaxProfile) args[1]);
                case "reviseTaxProfile" -> revise(
                        (CatalogDefinitionCommands.ReviseTaxProfile) args[1]);
                case "taxProfileHistory" -> history((TaxProfileId) args[1]);
                case "changeTaxProfileState" -> changeState(
                        (CatalogDefinitionCommands.ChangeTaxProfileState) args[1]);
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private CatalogOperationResult<CatalogDefinitions.TaxProfile> revise(
                CatalogDefinitionCommands.ReviseTaxProfile command) {
            for (int index = 0; index < profiles.size(); index++) {
                CatalogDefinitions.TaxProfile current = profiles.get(index);
                if (!current.id().equals(command.id())) {
                    continue;
                }
                CatalogDefinitions.TaxProfile revised = new CatalogDefinitions.TaxProfile(
                        current.id(), current.code(), current.displayName(),
                        command.internalKindCode(), command.description(), command.validFrom(),
                        command.validUntil(), current.state(), command.expectedVersion() + 1);
                profiles.set(index, revised);
                recordRevision(revised);
                return CatalogOperationResult.success(revised);
            }
            return CatalogOperationResult.failure(
                    py.com.logixone.plugins.commercialcatalog.application.CatalogResultCode.NOT_FOUND);
        }

        private CatalogOperationResult<CatalogDefinitions.TaxProfile> register(
                CatalogDefinitionCommands.RegisterTaxProfile command) {
            CatalogDefinitions.TaxProfile created = new CatalogDefinitions.TaxProfile(
                    new TaxProfileId(new UUID(0, 303)), command.code(), command.displayName(),
                    command.internalKindCode(), command.description(), command.validFrom(),
                    command.validUntil(), CatalogDefinitions.State.ACTIVE, 0);
            profiles.add(created);
            recordRevision(created);
            return CatalogOperationResult.success(created);
        }

        private CatalogOperationResult<CatalogDefinitions.TaxProfile> changeState(
                CatalogDefinitionCommands.ChangeTaxProfileState command) {
            for (int index = 0; index < profiles.size(); index++) {
                CatalogDefinitions.TaxProfile current = profiles.get(index);
                if (!current.id().equals(command.id())) {
                    continue;
                }
                CatalogDefinitions.TaxProfile changed = new CatalogDefinitions.TaxProfile(
                        current.id(), current.code(), current.displayName(),
                        current.internalKindCode(), current.description(), current.validFrom(),
                        current.validUntil(), command.targetState(), command.expectedVersion() + 1);
                profiles.set(index, changed);
                recordRevision(changed);
                return CatalogOperationResult.success(changed);
            }
            return CatalogOperationResult.failure(
                    py.com.logixone.plugins.commercialcatalog.application.CatalogResultCode.NOT_FOUND);
        }

        private CatalogOperationResult<List<CatalogDefinitions.TaxProfileRevision>> history(
                TaxProfileId id) {
            List<CatalogDefinitions.TaxProfileRevision> visible = revisions.stream()
                    .filter(revision -> revision.profileId().equals(id))
                    .sorted((left, right) -> Long.compare(right.version(), left.version()))
                    .toList();
            return visible.isEmpty()
                    ? CatalogOperationResult.failure(
                            py.com.logixone.plugins.commercialcatalog.application
                                    .CatalogResultCode.NOT_FOUND)
                    : CatalogOperationResult.success(visible);
        }

        private void recordRevision(CatalogDefinitions.TaxProfile profile) {
            for (int index = 0; index < revisions.size(); index++) {
                CatalogDefinitions.TaxProfileRevision revision = revisions.get(index);
                if (revision.profileId().equals(profile.id()) && revision.current()) {
                    revisions.set(index, new CatalogDefinitions.TaxProfileRevision(
                            revision.profileId(), revision.version(),
                            revision.internalKindCode(), revision.description(),
                            revision.validFrom(), revision.validUntil(), false));
                }
            }
            revisions.add(new CatalogDefinitions.TaxProfileRevision(
                    profile.id(), profile.version(), profile.internalKindCode(),
                    profile.description(), profile.validFrom(), profile.validUntil(), true));
        }

        private CatalogDefinitions.Snapshot snapshot() {
            return new CatalogDefinitions.Snapshot(
                    List.of(), List.of(), List.of(), List.of(), profiles, List.of());
        }

        private static CatalogDefinitions.TaxProfile profile(
                UUID id, String code, String name, String kind) {
            return new CatalogDefinitions.TaxProfile(
                    new TaxProfileId(id), code, name, kind, "Perfil ficticio de prueba",
                    Instant.parse("2026-01-01T00:00:00Z"), Optional.empty(),
                    CatalogDefinitions.State.ACTIVE, 0);
        }
    }
}
