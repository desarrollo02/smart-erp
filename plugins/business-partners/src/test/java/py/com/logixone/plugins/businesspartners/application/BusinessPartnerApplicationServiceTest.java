package py.com.logixone.plugins.businesspartners.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerKind;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerRole;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.businesspartners.application.command.BusinessPartnerCommands;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerRepository;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerDefinitionRepository;
import py.com.logixone.plugins.businesspartners.application.port.CountryReferencePolicy;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchCriteria;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchPage;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartner;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAddress;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDetailId;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionRevision;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerIdentification;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerIdentificationKey;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerName;

class BusinessPartnerApplicationServiceTest {

    private static final CompanyId COMPANY_A = company(1);
    private static final CompanyId COMPANY_B = company(2);
    private static final BusinessPartnerId PARTNER_ID = partner(10);
    private static final BusinessPartnerId CANDIDATE_ID = partner(11);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-29T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void registersWithTransactionalSequenceAndAuditsOnlyTechnicalData() {
        MemoryRepository repository = new MemoryRepository();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        BusinessPartnerCommandService service = service(repository, audit);

        var result = service.register(context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                new BusinessPartnerCommands.Register(
                        Optional.empty(),
                        BusinessPartnerKind.ORGANIZATION,
                        new BusinessPartnerName("Acme Comercial"),
                        Optional.of(new BusinessPartnerName("Acme S.A.")),
                        Optional.empty()));

        assertTrue(result.successful());
        assertEquals("BP-00000001", result.value().orElseThrow().code().value());
        assertEquals(0, result.value().orElseThrow().version());
        assertEquals(1, repository.insertions);
        assertEquals("REGISTER_BUSINESS_PARTNER", audit.getFirst().operation());
        assertEquals("business_partner", audit.getFirst().resourceType());
        assertEquals(Optional.of(PARTNER_ID.value().toString()), audit.getFirst().resourceId());
        assertFalse(audit.getFirst().toString().contains("Acme"));
    }

    @Test
    void rejectsForeignPluginOrPermissionBeforeTouchingBusinessRepository() {
        MemoryRepository repository = new MemoryRepository();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        BusinessPartnerCommandService service = service(repository, audit);
        BusinessPartnerOperationContext wrongPlugin = new BusinessPartnerOperationContext(
                authenticated(COMPANY_A),
                new PluginId("inventory"),
                BusinessPartnerPermissions.MANAGE,
                "request:denied-1");

        var result = service.register(wrongPlugin, new BusinessPartnerCommands.Register(
                Optional.of(new BusinessPartnerCode("MANUAL-1")),
                BusinessPartnerKind.NATURAL_PERSON,
                new BusinessPartnerName("Persona"),
                Optional.empty(),
                Optional.empty()));

        assertEquals(BusinessPartnerResultCode.ACCESS_DENIED, result.code());
        assertEquals(0, repository.insertions);
        assertEquals("ACCESS_DENIED", audit.getFirst().resultCode());
    }

    @Test
    void optimisticConflictDoesNotOverwriteAndIsAuditedAsRejected() {
        MemoryRepository repository = new MemoryRepository();
        repository.insert(newPartner(COMPANY_A, PARTNER_ID));
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        BusinessPartnerCommandService service = service(repository, audit);

        var result = service.rename(context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                new BusinessPartnerCommands.Rename(
                        PARTNER_ID,
                        7,
                        new BusinessPartnerName("Nuevo nombre"),
                        Optional.empty(),
                        Optional.empty()));

        assertEquals(BusinessPartnerResultCode.VERSION_CONFLICT, result.code());
        assertEquals("Nombre inicial", repository.findById(COMPANY_A, PARTNER_ID)
                .orElseThrow().displayName().value());
        assertEquals("VERSION_CONFLICT", audit.getFirst().resultCode());
    }

    @Test
    void duplicateIdentificationProducesWarningButPersistsTheChange() {
        MemoryRepository repository = new MemoryRepository();
        repository.insert(newPartner(COMPANY_A, PARTNER_ID));
        repository.candidates = List.of(PARTNER_ID, CANDIDATE_ID, CANDIDATE_ID);
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        BusinessPartnerCommandService service = service(repository, audit);
        BusinessPartnerIdentification identification = BusinessPartnerIdentification.create(
                detail(20),
                new BusinessPartnerAttributeCode("RUC"),
                Optional.of("PY"),
                "80012345-6",
                Optional.of("6"),
                Optional.empty());

        var result = service.addIdentification(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                new BusinessPartnerCommands.AddIdentification(PARTNER_ID, 0, identification));

        assertTrue(result.successful());
        assertEquals(1, result.warnings().size());
        assertEquals(List.of(CANDIDATE_ID), result.warnings().getFirst().candidateIds());
        assertEquals(1, result.value().orElseThrow().identifications().size());
        assertEquals("SUCCESS", audit.getFirst().resultCode());
    }

    @Test
    void rejectsIdentificationWithDisabledCountryBeforeReadingDuplicateCandidates() {
        MemoryRepository repository = new MemoryRepository();
        repository.insert(newPartner(COMPANY_A, PARTNER_ID));
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        BusinessPartnerCommandService service = service(
                repository, definitions(), (company, code) -> false, audit);
        BusinessPartnerIdentification identification = BusinessPartnerIdentification.create(
                detail(24),
                new BusinessPartnerAttributeCode("RUC"),
                Optional.of("PY"),
                "80012345-6",
                Optional.empty(),
                Optional.empty());

        var result = service.addIdentification(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                new BusinessPartnerCommands.AddIdentification(PARTNER_ID, 0, identification));

        assertEquals(BusinessPartnerResultCode.INVALID_OPERATION, result.code());
        assertEquals(0, repository.candidateSearches);
        assertEquals(0, repository.findById(COMPANY_A, PARTNER_ID)
                .orElseThrow().identifications().size());
        assertEquals("INVALID_OPERATION", audit.getFirst().resultCode());
    }

    @Test
    void rejectsMissingOrInactiveOperationalDefinitionsBeforeMutatingThePartner() {
        MemoryRepository repository = new MemoryRepository();
        repository.insert(newPartner(COMPANY_A, PARTNER_ID));
        MemoryDefinitionRepository definitions = definitions();
        definitions.put(BusinessPartnerDefinition.create(
                        COMPANY_A,
                        BusinessPartnerDefinitionKind.IDENTIFICATION_TYPE,
                        new BusinessPartnerAttributeCode("legacy_card"),
                        new BusinessPartnerName("Credencial histórica"))
                .changeState(BusinessPartnerState.INACTIVE, 0));
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        BusinessPartnerCommandService service = service(repository, definitions, audit);

        BusinessPartnerIdentification inactiveIdentification = BusinessPartnerIdentification.create(
                detail(21),
                new BusinessPartnerAttributeCode("legacy_card"),
                Optional.empty(),
                "ABC-123",
                Optional.empty(),
                Optional.empty());
        var inactive = service.addIdentification(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                new BusinessPartnerCommands.AddIdentification(
                        PARTNER_ID, 0, inactiveIdentification));

        BusinessPartnerAddress unknownPurpose = new BusinessPartnerAddress(
                detail(22),
                new BusinessPartnerAttributeCode("postal"),
                new BusinessPartnerAttributeCode("unknown"),
                "Calle Demo 123",
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), true, false);
        var unknown = service.addAddress(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                new BusinessPartnerCommands.AddAddress(PARTNER_ID, 0, unknownPurpose));

        BusinessPartnerAddress validAddress = new BusinessPartnerAddress(
                detail(23),
                new BusinessPartnerAttributeCode("postal"),
                new BusinessPartnerAttributeCode("billing"),
                "Calle Demo 456",
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), true, false);
        var valid = service.addAddress(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                new BusinessPartnerCommands.AddAddress(PARTNER_ID, 0, validAddress));

        assertEquals(BusinessPartnerResultCode.INVALID_OPERATION, inactive.code());
        assertEquals(BusinessPartnerResultCode.INVALID_OPERATION, unknown.code());
        assertTrue(valid.successful());
        assertEquals(1, valid.value().orElseThrow().addresses().size());
        assertEquals(List.of("INVALID_OPERATION", "INVALID_OPERATION", "SUCCESS"),
                audit.stream().map(TechnicalAuditEvent::resultCode).toList());
    }

    @Test
    void rolesAndLifecycleRequireTheirDedicatedPermissions() {
        MemoryRepository repository = new MemoryRepository();
        repository.insert(newPartner(COMPANY_A, PARTNER_ID));
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        BusinessPartnerCommandService service = service(repository, audit);

        var deniedRole = service.assignRole(context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                new BusinessPartnerCommands.AssignRole(
                        PARTNER_ID, 0, BusinessPartnerRole.CLIENT, Optional.empty()));
        var role = service.assignRole(context(BusinessPartnerPermissions.ROLES_MANAGE, COMPANY_A),
                new BusinessPartnerCommands.AssignRole(
                        PARTNER_ID, 0, BusinessPartnerRole.CLIENT, Optional.empty()));
        var deniedLifecycle = service.changeLifecycle(
                context(BusinessPartnerPermissions.ROLES_MANAGE, COMPANY_A),
                new BusinessPartnerCommands.ChangeLifecycle(
                        PARTNER_ID, 1, BusinessPartnerState.INACTIVE));
        var lifecycle = service.changeLifecycle(
                context(BusinessPartnerPermissions.LIFECYCLE_MANAGE, COMPANY_A),
                new BusinessPartnerCommands.ChangeLifecycle(
                        PARTNER_ID, 1, BusinessPartnerState.INACTIVE));

        assertEquals(BusinessPartnerResultCode.ACCESS_DENIED, deniedRole.code());
        assertTrue(role.successful());
        assertEquals(BusinessPartnerResultCode.ACCESS_DENIED, deniedLifecycle.code());
        assertEquals(BusinessPartnerState.INACTIVE,
                lifecycle.value().orElseThrow().state());
    }

    @Test
    void queryPermissionAndCompanyScopeAreEnforcedBeforeReads() {
        MemoryRepository repository = new MemoryRepository();
        repository.insert(newPartner(COMPANY_A, PARTNER_ID));
        repository.finds = 0;
        BusinessPartnerQueryService queries = new BusinessPartnerQueryService(repository);

        var denied = queries.detail(context(BusinessPartnerPermissions.MANAGE, COMPANY_A), PARTNER_ID);
        var otherCompany = queries.detail(context(BusinessPartnerPermissions.VIEW, COMPANY_B), PARTNER_ID);
        var allowed = queries.detail(context(BusinessPartnerPermissions.VIEW, COMPANY_A), PARTNER_ID);

        assertEquals(BusinessPartnerResultCode.ACCESS_DENIED, denied.code());
        assertEquals(BusinessPartnerResultCode.NOT_FOUND, otherCompany.code());
        assertTrue(allowed.successful());
        assertEquals(2, repository.finds);
    }

    private static BusinessPartnerCommandService service(
            MemoryRepository repository, List<TechnicalAuditEvent> audit) {
        return service(repository, definitions(), audit);
    }

    private static BusinessPartnerCommandService service(
            MemoryRepository repository,
            MemoryDefinitionRepository definitions,
            List<TechnicalAuditEvent> audit) {
        return service(repository, definitions, (company, code) -> true, audit);
    }

    private static BusinessPartnerCommandService service(
            MemoryRepository repository,
            MemoryDefinitionRepository definitions,
            CountryReferencePolicy countries,
            List<TechnicalAuditEvent> audit) {
        return new BusinessPartnerCommandService(
                repository,
                definitions,
                (companyId, scope) -> 1,
                () -> PARTNER_ID,
                countries,
                audit::add,
                CLOCK);
    }

    private static MemoryDefinitionRepository definitions() {
        MemoryDefinitionRepository repository = new MemoryDefinitionRepository();
        repository.put(definition(BusinessPartnerDefinitionKind.IDENTIFICATION_TYPE,
                "ruc", "RUC"));
        repository.put(definition(BusinessPartnerDefinitionKind.IDENTIFICATION_TYPE,
                "tax_id", "Identificación tributaria"));
        repository.put(definition(BusinessPartnerDefinitionKind.ADDRESS_TYPE,
                "postal", "Postal"));
        repository.put(definition(BusinessPartnerDefinitionKind.ADDRESS_PURPOSE,
                "billing", "Facturación"));
        repository.put(definition(BusinessPartnerDefinitionKind.CHANNEL_KIND,
                "email", "Correo electrónico"));
        return repository;
    }

    private static BusinessPartnerDefinition definition(
            BusinessPartnerDefinitionKind kind, String code, String name) {
        return BusinessPartnerDefinition.create(
                COMPANY_A, kind, new BusinessPartnerAttributeCode(code),
                new BusinessPartnerName(name));
    }

    private static BusinessPartnerOperationContext context(
            ContributionId permission, CompanyId companyId) {
        return new BusinessPartnerOperationContext(
                authenticated(companyId),
                BusinessPartnersIdentity.PLUGIN_ID,
                permission,
                "request:test-1");
    }

    private static AuthenticatedCompanyContext authenticated(CompanyId companyId) {
        return new AuthenticatedCompanyContext(
                new AuthenticatedActor(new AppUserId(
                        UUID.fromString("00000000-0000-0000-0000-000000000099"))),
                companyId);
    }

    private static BusinessPartner newPartner(CompanyId companyId, BusinessPartnerId id) {
        return BusinessPartner.create(
                companyId,
                id,
                new BusinessPartnerCode("BP-1"),
                BusinessPartnerKind.ORGANIZATION,
                new BusinessPartnerName("Nombre inicial"),
                Optional.empty(),
                Optional.empty());
    }

    private static CompanyId company(long suffix) {
        return new CompanyId(new UUID(0, suffix));
    }

    private static BusinessPartnerId partner(long suffix) {
        return new BusinessPartnerId(new UUID(0, suffix));
    }

    private static BusinessPartnerDetailId detail(long suffix) {
        return new BusinessPartnerDetailId(new UUID(0, suffix));
    }

    private static final class MemoryRepository implements BusinessPartnerRepository {
        private final Map<String, BusinessPartner> values = new HashMap<>();
        private List<BusinessPartnerId> candidates = List.of();
        private int insertions;
        private int finds;
        private int candidateSearches;

        @Override
        public Optional<BusinessPartner> findById(CompanyId companyId, BusinessPartnerId id) {
            finds++;
            return Optional.ofNullable(values.get(key(companyId, id)));
        }

        @Override
        public BusinessPartner insert(BusinessPartner partner) {
            insertions++;
            values.put(key(partner.companyId(), partner.id()), partner);
            return partner;
        }

        @Override
        public BusinessPartner update(BusinessPartner partner, long expectedPersistedVersion) {
            values.put(key(partner.companyId(), partner.id()), partner);
            return partner;
        }

        @Override
        public List<BusinessPartnerId> findIdentificationCandidates(
                CompanyId companyId, BusinessPartnerIdentificationKey candidate) {
            candidateSearches++;
            return candidates;
        }

        @Override
        public BusinessPartnerSearchPage search(
                CompanyId companyId, BusinessPartnerSearchCriteria criteria) {
            List<py.com.logixone.plugins.businesspartners.api.BusinessPartnerReference> items =
                    values.values().stream()
                            .filter(value -> value.companyId().equals(companyId))
                            .map(BusinessPartner::toReference)
                            .toList();
            return new BusinessPartnerSearchPage(
                    items, items.size(), criteria.offset(), criteria.limit());
        }

        private static String key(CompanyId companyId, BusinessPartnerId id) {
            return companyId + ":" + id;
        }
    }

    private static final class MemoryDefinitionRepository
            implements BusinessPartnerDefinitionRepository {
        private final Map<String, BusinessPartnerDefinition> values = new HashMap<>();

        private void put(BusinessPartnerDefinition definition) {
            values.put(key(definition.companyId(), definition.kind(), definition.code()), definition);
        }

        @Override
        public List<BusinessPartnerDefinition> findAll(
                CompanyId companyId, BusinessPartnerDefinitionKind kind) {
            return values.values().stream()
                    .filter(value -> value.companyId().equals(companyId) && value.kind() == kind)
                    .toList();
        }

        @Override
        public Optional<BusinessPartnerDefinition> findByCode(
                CompanyId companyId,
                BusinessPartnerDefinitionKind kind,
                BusinessPartnerAttributeCode code) {
            return Optional.ofNullable(values.get(key(companyId, kind, code)));
        }

        @Override
        public List<BusinessPartnerDefinitionRevision> history(
                CompanyId companyId,
                BusinessPartnerDefinitionKind kind,
                BusinessPartnerAttributeCode code) {
            return List.of();
        }

        @Override
        public BusinessPartnerDefinition insert(BusinessPartnerDefinition definition) {
            put(definition);
            return definition;
        }

        @Override
        public BusinessPartnerDefinition update(
                BusinessPartnerDefinition definition, long expectedPersistedVersion) {
            put(definition);
            return definition;
        }

        private static String key(
                CompanyId companyId,
                BusinessPartnerDefinitionKind kind,
                BusinessPartnerAttributeCode code) {
            return companyId + ":" + kind + ":" + code.value();
        }
    }
}
