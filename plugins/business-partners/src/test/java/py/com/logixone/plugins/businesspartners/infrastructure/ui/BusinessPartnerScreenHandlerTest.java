package py.com.logixone.plugins.businesspartners.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import py.com.logixone.plugins.businesspartners.BusinessPartnersScreenContract;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerKind;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerRole;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerOperationContext;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerOperationResult;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerDefinitionUseCases;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerPermissions;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerUseCases;
import py.com.logixone.plugins.businesspartners.application.command.BusinessPartnerCommands;
import py.com.logixone.plugins.businesspartners.application.command.ChangeBusinessPartnerDefinitionState;
import py.com.logixone.plugins.businesspartners.application.command.RegisterBusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.application.command.ReviseBusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchCriteria;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchPage;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartner;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionRevision;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerIdentificationKey;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerName;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerSnapshot;
import py.com.logixone.plugins.referencedata.api.CountryCode;
import py.com.logixone.plugins.referencedata.api.CountryReference;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.referencedata.api.CurrencyReference;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;
import py.com.logixone.plugins.referencedata.api.ReferenceDataRelease;

class BusinessPartnerScreenHandlerTest {

    private static final CompanyId COMPANY = new CompanyId(
            UUID.fromString("00000000-0000-0000-0000-000000000101"));
    private static final BusinessPartnerId PARTNER_ID = new BusinessPartnerId(
            UUID.fromString("00000000-0000-0000-0000-000000000201"));

    private RecordingAuthorization authorization;
    private FakeUseCases useCases;
    private BusinessPartnerScreenHandler handler;

    @BeforeEach
    void setUp() {
        authorization = new RecordingAuthorization();
        useCases = new FakeUseCases();
        handler = new BusinessPartnerScreenHandler();
        handler.authorization = authorization;
        handler.useCases = useCases;
        handler.definitionUseCases = new FakeDefinitionUseCases();
        handler.referenceDataDirectory = new FakeReferenceDataDirectory();
    }

    @Test
    void loadsACompanyScopedTableOptionsAndDetailUsingViewPermission() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.empty(),
                Map.of(),
                Optional.of(PARTNER_ID.toString()),
                Optional.of(0L)));

        assertEquals(List.of(BusinessPartnerPermissions.VIEW.value()), authorization.permissions);
        assertEquals(1, result.table().orElseThrow().rows().size());
        assertEquals("Acme", result.detail().orElseThrow().title());
        assertEquals("ORGANIZATION",
                result.inputs().get(BusinessPartnersScreenContract.NEW_KIND));
        assertEquals(3, result.options().get(BusinessPartnersScreenContract.SEARCH_ROLE).size());
        assertEquals(List.of("email", "phone"), result.options()
                .get(BusinessPartnersScreenContract.CHANNEL_KIND).stream()
                .map(ScreenInteraction.Option::value)
                .toList());
        assertEquals(List.of("national_id", "tax_id"), result.options()
                .get(BusinessPartnersScreenContract.IDENTIFICATION_TYPE).stream()
                .map(ScreenInteraction.Option::value)
                .toList());
        assertEquals(List.of("PY"), result.options()
                .get(BusinessPartnersScreenContract.IDENTIFICATION_COUNTRY).stream()
                .map(ScreenInteraction.Option::value)
                .toList());
        assertEquals(List.of("physical", "postal"), result.options()
                .get(BusinessPartnersScreenContract.ADDRESS_TYPE).stream()
                .map(ScreenInteraction.Option::value)
                .toList());
        assertEquals(List.of("billing", "general"), result.options()
                .get(BusinessPartnersScreenContract.ADDRESS_PURPOSE).stream()
                .map(ScreenInteraction.Option::value)
                .toList());
    }

    @Test
    void registrationUsesManageThenRevalidatesViewAndReturnsSuccess() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(BusinessPartnersScreenContract.REGISTER),
                Map.of(
                        BusinessPartnersScreenContract.NEW_CODE, "NEW-1",
                        BusinessPartnersScreenContract.NEW_KIND, "NATURAL_PERSON",
                        BusinessPartnersScreenContract.NEW_DISPLAY_NAME, "Ana Demo"),
                Optional.empty(),
                Optional.empty()));

        assertEquals(List.of(
                BusinessPartnerPermissions.MANAGE.value(),
                BusinessPartnerPermissions.VIEW.value()), authorization.permissions);
        assertTrue(result.notices().stream()
                .anyMatch(notice -> notice.level() == ScreenInteraction.NoticeLevel.SUCCESS));
        assertEquals("Ana Demo", result.detail().orElseThrow().title());
        assertEquals("", result.inputs().getOrDefault(
                BusinessPartnersScreenContract.NEW_DISPLAY_NAME, ""));
    }

    @Test
    void lifecycleActionUsesItsDedicatedPermissionAndRefreshesTheVersion() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(BusinessPartnersScreenContract.DEACTIVATE_PARTNER),
                Map.of(),
                Optional.of(PARTNER_ID.toString()),
                Optional.of(0L)));

        assertEquals(List.of(
                BusinessPartnerPermissions.LIFECYCLE_MANAGE.value(),
                BusinessPartnerPermissions.VIEW.value()), authorization.permissions);
        assertEquals(1L, result.selectedResourceVersion().orElseThrow());
        assertTrue(result.detail().orElseThrow().items().stream()
                .anyMatch(item -> item.label().equals("Estado") && item.value().equals("Inactivo")));
    }

    @Test
    void validGeneralContactChannelReachesTheUseCaseAndRefreshesTheVersion() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(BusinessPartnersScreenContract.ADD_CHANNEL),
                Map.of(
                        BusinessPartnersScreenContract.CHANNEL_KIND, "email",
                        BusinessPartnersScreenContract.CHANNEL_VALUE,
                        "demo-61a147ba@example.invalid"),
                Optional.of(PARTNER_ID.toString()),
                Optional.of(0L)));

        assertEquals(List.of(
                BusinessPartnerPermissions.MANAGE.value(),
                BusinessPartnerPermissions.VIEW.value()), authorization.permissions);
        assertEquals(1L, result.selectedResourceVersion().orElseThrow());
        assertTrue(result.notices().stream()
                .anyMatch(notice -> notice.level() == ScreenInteraction.NoticeLevel.SUCCESS));
        assertEquals(1, result.detail().orElseThrow().items().stream()
                .filter(item -> item.label().equals("Canales") && item.value().equals("1"))
                .count());
    }

    @Test
    void identificationAndAddressUseGovernedTypeAndPurposeSelections() {
        ScreenInteraction.Result identified = handler.interact(new ScreenInteraction.Request(
                Optional.of(BusinessPartnersScreenContract.ADD_IDENTIFICATION),
                Map.of(
                        BusinessPartnersScreenContract.IDENTIFICATION_TYPE, "tax_id",
                        BusinessPartnersScreenContract.IDENTIFICATION_VALUE, "80012345-6"),
                Optional.of(PARTNER_ID.toString()),
                Optional.of(0L)));

        assertEquals(1L, identified.selectedResourceVersion().orElseThrow());
        assertTrue(identified.detail().orElseThrow().items().stream()
                .anyMatch(item -> item.label().equals("Identificaciones")
                        && item.value().equals("1")));

        ScreenInteraction.Result addressed = handler.interact(new ScreenInteraction.Request(
                Optional.of(BusinessPartnersScreenContract.ADD_ADDRESS),
                Map.of(
                        BusinessPartnersScreenContract.ADDRESS_TYPE, "postal",
                        BusinessPartnersScreenContract.ADDRESS_PURPOSE, "billing",
                        BusinessPartnersScreenContract.ADDRESS_LINE, "Calle Demo 123"),
                Optional.of(PARTNER_ID.toString()),
                Optional.of(1L)));

        assertEquals(2L, addressed.selectedResourceVersion().orElseThrow());
        assertTrue(addressed.detail().orElseThrow().items().stream()
                .anyMatch(item -> item.label().equals("Direcciones")
                        && item.value().equals("1")));
    }

    @Test
    void invalidRegistrationIsRejectedBeforeManageAuthorization() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(BusinessPartnersScreenContract.REGISTER),
                Map.of(BusinessPartnersScreenContract.NEW_KIND, "ORGANIZATION"),
                Optional.empty(),
                Optional.empty()));

        assertEquals(List.of(BusinessPartnerPermissions.VIEW.value()), authorization.permissions);
        assertEquals(ScreenInteraction.NoticeLevel.ERROR, result.notices().getFirst().level());
    }

    private static final class RecordingAuthorization implements CurrentCompanyAuthorization {

        private final List<String> permissions = new ArrayList<>();

        @Override
        public AuthorizedCompanyOperation require(String pluginId, String permissionId) {
            permissions.add(permissionId);
            return new AuthorizedCompanyOperation(
                    new AuthenticatedCompanyContext(
                            new AuthenticatedActor(new AppUserId(UUID.fromString(
                                    "00000000-0000-0000-0000-000000000301"))),
                            COMPANY),
                    pluginId,
                    permissionId,
                    "ui:test");
        }
    }

    private static final class FakeUseCases implements BusinessPartnerUseCases {

        private BusinessPartner partner = newPartner(PARTNER_ID, "BP-1", "Acme");

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerSnapshot> register(
                BusinessPartnerOperationContext context, BusinessPartnerCommands.Register command) {
            partner = BusinessPartner.create(
                    COMPANY,
                    new BusinessPartnerId(UUID.fromString(
                            "00000000-0000-0000-0000-000000000202")),
                    command.code().orElse(new BusinessPartnerCode("BP-2")),
                    command.kind(),
                    command.displayName(),
                    command.legalName(),
                    command.tradeName());
            return success();
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerSnapshot> changeLifecycle(
                BusinessPartnerOperationContext context,
                BusinessPartnerCommands.ChangeLifecycle command) {
            if (command.state() == BusinessPartnerState.ACTIVE) {
                partner.reactivate(command.expectedVersion());
            } else {
                partner.inactivate(command.expectedVersion());
            }
            return success();
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerSearchPage> search(
                BusinessPartnerOperationContext context, BusinessPartnerSearchCriteria criteria) {
            return BusinessPartnerOperationResult.success(
                    new BusinessPartnerSearchPage(
                            List.of(partner.toReference()), 1, criteria.offset(), criteria.limit()),
                    List.of());
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerSnapshot> detail(
                BusinessPartnerOperationContext context, BusinessPartnerId id) {
            return id.equals(partner.id())
                    ? success()
                    : BusinessPartnerOperationResult.failure(
                            py.com.logixone.plugins.businesspartners.application.BusinessPartnerResultCode.NOT_FOUND);
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerSnapshot> rename(
                BusinessPartnerOperationContext context, BusinessPartnerCommands.Rename command) {
            throw unsupported();
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerSnapshot> changeCode(
                BusinessPartnerOperationContext context, BusinessPartnerCommands.ChangeCode command) {
            throw unsupported();
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerSnapshot> addIdentification(
                BusinessPartnerOperationContext context,
                BusinessPartnerCommands.AddIdentification command) {
            partner.addIdentification(command.expectedVersion(), command.identification());
            return success();
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerSnapshot> addAddress(
                BusinessPartnerOperationContext context, BusinessPartnerCommands.AddAddress command) {
            partner.addAddress(command.expectedVersion(), command.address());
            return success();
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerSnapshot> addChannel(
                BusinessPartnerOperationContext context, BusinessPartnerCommands.AddChannel command) {
            partner.addContactChannel(command.expectedVersion(), command.channel());
            return success();
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerSnapshot> addContact(
                BusinessPartnerOperationContext context, BusinessPartnerCommands.AddContact command) {
            throw unsupported();
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerSnapshot> assignRole(
                BusinessPartnerOperationContext context, BusinessPartnerCommands.AssignRole command) {
            throw unsupported();
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerSnapshot> changeRoleState(
                BusinessPartnerOperationContext context,
                BusinessPartnerCommands.ChangeRoleState command) {
            throw unsupported();
        }

        @Override
        public BusinessPartnerOperationResult<List<BusinessPartnerId>> duplicateCandidates(
                BusinessPartnerOperationContext context,
                BusinessPartnerIdentificationKey candidate) {
            throw unsupported();
        }

        private BusinessPartnerOperationResult<BusinessPartnerSnapshot> success() {
            return BusinessPartnerOperationResult.success(partner.snapshot(), List.of());
        }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("Not needed by this screen test");
        }
    }

    private static final class FakeDefinitionUseCases
            implements BusinessPartnerDefinitionUseCases {

        @Override
        public BusinessPartnerOperationResult<List<BusinessPartnerDefinition>> definitions(
                BusinessPartnerOperationContext context, BusinessPartnerDefinitionKind kind) {
            List<BusinessPartnerDefinition> values = switch (kind) {
                case CHANNEL_KIND -> List.of(
                        definition(kind, "email", "Correo electrónico"),
                        definition(kind, "phone", "Teléfono"),
                        definition(kind, "legacy_fax", "Fax histórico")
                                .changeState(BusinessPartnerState.INACTIVE, 0));
                case IDENTIFICATION_TYPE -> List.of(
                        definition(kind, "national_id", "Documento nacional"),
                        definition(kind, "tax_id", "Identificación tributaria"));
                case ADDRESS_TYPE -> List.of(
                        definition(kind, "physical", "Física"),
                        definition(kind, "postal", "Postal"));
                case ADDRESS_PURPOSE -> List.of(
                        definition(kind, "billing", "Facturación"),
                        definition(kind, "general", "General"));
            };
            return BusinessPartnerOperationResult.success(values, List.of());
        }

        private static BusinessPartnerDefinition definition(
                BusinessPartnerDefinitionKind kind, String code, String name) {
            return BusinessPartnerDefinition.create(
                    COMPANY, kind, new BusinessPartnerAttributeCode(code),
                    new BusinessPartnerName(name));
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerDefinition> registerDefinition(
                BusinessPartnerOperationContext context,
                RegisterBusinessPartnerDefinition command) {
            throw new UnsupportedOperationException("Not needed by the directory screen test");
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerDefinition> reviseDefinition(
                BusinessPartnerOperationContext context,
                ReviseBusinessPartnerDefinition command) {
            throw new UnsupportedOperationException("Not needed by the directory screen test");
        }

        @Override
        public BusinessPartnerOperationResult<List<BusinessPartnerDefinitionRevision>>
                definitionHistory(
                        BusinessPartnerOperationContext context,
                        BusinessPartnerDefinitionKind kind,
                        BusinessPartnerAttributeCode code) {
            throw new UnsupportedOperationException("Not needed by the directory screen test");
        }

        @Override
        public BusinessPartnerOperationResult<BusinessPartnerDefinition> changeDefinitionState(
                BusinessPartnerOperationContext context,
                ChangeBusinessPartnerDefinitionState command) {
            throw new UnsupportedOperationException("Not needed by the directory screen test");
        }
    }

    private static final class FakeReferenceDataDirectory implements ReferenceDataDirectory {

        @Override
        public ReferenceDataRelease currentRelease(
                CompanyId companyId, ReferenceDataCatalog catalog) {
            throw new UnsupportedOperationException("Not needed by the directory screen test");
        }

        @Override
        public List<CountryReference> countries(CompanyId companyId) {
            return List.of(new CountryReference(
                    new CountryCode("PY"), "PRY", "600", "Paraguay", "un-m49-2026-08-04", true));
        }

        @Override
        public Optional<CountryReference> findCountry(CompanyId companyId, CountryCode code) {
            return countries(companyId).stream().filter(country -> country.code().equals(code)).findFirst();
        }

        @Override
        public List<CurrencyReference> currencies(CompanyId companyId) {
            return List.of();
        }

        @Override
        public Optional<CurrencyReference> findCurrency(CompanyId companyId, CurrencyCode code) {
            return Optional.empty();
        }
    }

    private static BusinessPartner newPartner(BusinessPartnerId id, String code, String name) {
        return BusinessPartner.create(
                COMPANY,
                id,
                new BusinessPartnerCode(code),
                BusinessPartnerKind.ORGANIZATION,
                new BusinessPartnerName(name),
                Optional.empty(),
                Optional.empty());
    }
}
