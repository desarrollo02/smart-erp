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
import py.com.logixone.kernel.api.audit.TechnicalAuditOutcome;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.businesspartners.application.command.ChangeBusinessPartnerDefinitionState;
import py.com.logixone.plugins.businesspartners.application.command.RegisterBusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.application.command.ReviseBusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerDefinitionRepository;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerPersistenceCode;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerPersistenceException;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionRevision;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerName;

class BusinessPartnerDefinitionServiceTest {

    private static final CompanyId COMPANY_A = new CompanyId(new UUID(0, 1));
    private static final CompanyId COMPANY_B = new CompanyId(new UUID(0, 2));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-01T18:00:00Z"), ZoneOffset.UTC);

    @Test
    void registersACompanyScopedChannelKindAndAuditsOnlyStableCodes() {
        MemoryRepository repository = new MemoryRepository();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        BusinessPartnerDefinitionService service = new BusinessPartnerDefinitionService(
                repository, audit::add, CLOCK);

        var result = service.registerDefinition(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                command("Telegram", "Telegram empresarial"));

        assertTrue(result.successful());
        assertEquals("telegram", result.value().orElseThrow().code().value());
        assertEquals(BusinessPartnerState.ACTIVE, result.value().orElseThrow().state());
        assertEquals(1, repository.findAll(COMPANY_A, kind()).size());
        assertTrue(repository.findAll(COMPANY_B, kind()).isEmpty());
        assertEquals("business_partner_definition", audit.getFirst().resourceType());
        assertEquals(Optional.of("CHANNEL_KIND:telegram"), audit.getFirst().resourceId());
        assertFalse(audit.getFirst().toString().contains("empresarial"));
    }

    @Test
    void rejectsDuplicateCodesWithoutOverwritingTheExistingDefinition() {
        MemoryRepository repository = new MemoryRepository();
        BusinessPartnerDefinitionService service = new BusinessPartnerDefinitionService(
                repository, ignored -> { }, CLOCK);

        var first = service.registerDefinition(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                command("email", "Correo electrónico"));
        var duplicate = service.registerDefinition(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                command("EMAIL", "Otro nombre"));

        assertTrue(first.successful());
        assertEquals(BusinessPartnerResultCode.GENERAL_CODE_CONFLICT, duplicate.code());
        assertEquals("Correo electrónico", repository.findAll(COMPANY_A, kind())
                .getFirst().displayName().value());
    }

    @Test
    void requiresViewOrManageBeforeReadingAndManageBeforeWriting() {
        MemoryRepository repository = new MemoryRepository();
        BusinessPartnerDefinitionService service = new BusinessPartnerDefinitionService(
                repository, ignored -> { }, CLOCK);

        var deniedWrite = service.registerDefinition(
                context(BusinessPartnerPermissions.VIEW, COMPANY_A),
                command("email", "Correo electrónico"));
        var allowedRead = service.definitions(
                context(BusinessPartnerPermissions.VIEW, COMPANY_A), kind());
        var deniedRead = service.definitions(
                context(BusinessPartnerPermissions.ROLES_MANAGE, COMPANY_A), kind());

        assertEquals(BusinessPartnerResultCode.ACCESS_DENIED, deniedWrite.code());
        assertTrue(allowedRead.successful());
        assertEquals(BusinessPartnerResultCode.ACCESS_DENIED, deniedRead.code());
        assertTrue(repository.findAll(COMPANY_A, kind()).isEmpty());
    }

    @Test
    void inactivatesAndReactivatesWithoutDeletingTheDefinition() {
        MemoryRepository repository = new MemoryRepository();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        BusinessPartnerDefinitionService service = new BusinessPartnerDefinitionService(
                repository, audit::add, CLOCK);
        service.registerDefinition(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                command("telegram", "Telegram empresarial"));

        var inactive = service.changeDefinitionState(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                stateCommand("telegram", BusinessPartnerState.INACTIVE, 0));
        var unchanged = service.changeDefinitionState(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                stateCommand("telegram", BusinessPartnerState.INACTIVE, 1));
        var active = service.changeDefinitionState(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                stateCommand("telegram", BusinessPartnerState.ACTIVE, 1));

        assertEquals(BusinessPartnerState.INACTIVE, inactive.value().orElseThrow().state());
        assertEquals(1, inactive.value().orElseThrow().version());
        assertEquals(1, unchanged.value().orElseThrow().version());
        assertEquals(BusinessPartnerState.ACTIVE, active.value().orElseThrow().state());
        assertEquals(2, active.value().orElseThrow().version());
        assertEquals(1, repository.findAll(COMPANY_A, kind()).size());
        assertEquals(TechnicalAuditOutcome.UNCHANGED, audit.get(2).outcome());
        assertEquals(Optional.of("CHANNEL_KIND:telegram"), audit.getLast().resourceId());
        assertEquals(Optional.of(2L), audit.getLast().resultingVersion());
    }

    @Test
    void rejectsStaleMissingAndUnauthorizedDefinitionLifecycleChanges() {
        MemoryRepository repository = new MemoryRepository();
        BusinessPartnerDefinitionService service = new BusinessPartnerDefinitionService(
                repository, ignored -> { }, CLOCK);
        service.registerDefinition(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                command("email", "Correo electronico"));

        var stale = service.changeDefinitionState(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                stateCommand("email", BusinessPartnerState.INACTIVE, 4));
        var missing = service.changeDefinitionState(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                stateCommand("missing", BusinessPartnerState.INACTIVE, 0));
        var denied = service.changeDefinitionState(
                context(BusinessPartnerPermissions.VIEW, COMPANY_A),
                stateCommand("email", BusinessPartnerState.INACTIVE, 0));

        assertEquals(BusinessPartnerResultCode.VERSION_CONFLICT, stale.code());
        assertEquals(BusinessPartnerResultCode.NOT_FOUND, missing.code());
        assertEquals(BusinessPartnerResultCode.ACCESS_DENIED, denied.code());
        assertEquals(BusinessPartnerState.ACTIVE, repository.findAll(COMPANY_A, kind())
                .getFirst().state());
    }

    @Test
    void revisesOnlyTheDisplayNameAndExposesAppendOnlyCompanyHistory() {
        MemoryRepository repository = new MemoryRepository();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        BusinessPartnerDefinitionService service = new BusinessPartnerDefinitionService(
                repository, audit::add, CLOCK);
        service.registerDefinition(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                command("telegram", "Telegram"));

        var revised = service.reviseDefinition(
                context(BusinessPartnerPermissions.MANAGE, COMPANY_A),
                reviseCommand("telegram", "Telegram empresarial", 0));
        var history = service.definitionHistory(
                context(BusinessPartnerPermissions.VIEW, COMPANY_A),
                kind(),
                new BusinessPartnerAttributeCode("telegram"));
        var otherCompanyHistory = service.definitionHistory(
                context(BusinessPartnerPermissions.VIEW, COMPANY_B),
                kind(),
                new BusinessPartnerAttributeCode("telegram"));

        assertTrue(revised.successful());
        assertEquals("telegram", revised.value().orElseThrow().code().value());
        assertEquals("Telegram empresarial",
                revised.value().orElseThrow().displayName().value());
        assertEquals(1, revised.value().orElseThrow().version());
        assertEquals(List.of(1L, 0L), history.value().orElseThrow().stream()
                .map(BusinessPartnerDefinitionRevision::version)
                .toList());
        assertEquals("Telegram", history.value().orElseThrow().getLast()
                .displayName().value());
        assertEquals(BusinessPartnerResultCode.NOT_FOUND, otherCompanyHistory.code());
        assertEquals("REVISE_BUSINESS_PARTNER_DEFINITION", audit.get(1).operation());
        assertEquals(Optional.of("CHANNEL_KIND:telegram"), audit.get(1).resourceId());
        assertFalse(audit.get(1).toString().contains("empresarial"));
    }

    private static RegisterBusinessPartnerDefinition command(String code, String name) {
        return new RegisterBusinessPartnerDefinition(
                kind(), new BusinessPartnerAttributeCode(code), new BusinessPartnerName(name));
    }

    private static ChangeBusinessPartnerDefinitionState stateCommand(
            String code, BusinessPartnerState state, long expectedVersion) {
        return new ChangeBusinessPartnerDefinitionState(
                kind(), new BusinessPartnerAttributeCode(code), state, expectedVersion);
    }

    private static ReviseBusinessPartnerDefinition reviseCommand(
            String code, String name, long expectedVersion) {
        return new ReviseBusinessPartnerDefinition(
                kind(),
                new BusinessPartnerAttributeCode(code),
                new BusinessPartnerName(name),
                expectedVersion);
    }

    private static BusinessPartnerDefinitionKind kind() {
        return BusinessPartnerDefinitionKind.CHANNEL_KIND;
    }

    private static BusinessPartnerOperationContext context(
            ContributionId permission, CompanyId companyId) {
        return new BusinessPartnerOperationContext(
                new AuthenticatedCompanyContext(
                        new AuthenticatedActor(new AppUserId(new UUID(0, 99))), companyId),
                BusinessPartnersIdentity.PLUGIN_ID,
                permission,
                "request:definition-test");
    }

    private static final class MemoryRepository implements BusinessPartnerDefinitionRepository {
        private final Map<String, BusinessPartnerDefinition> values = new HashMap<>();
        private final Map<String, List<BusinessPartnerDefinitionRevision>> revisions =
                new HashMap<>();

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
            return List.copyOf(revisions.getOrDefault(
                    key(companyId, kind, code), List.of()));
        }

        @Override
        public BusinessPartnerDefinition insert(BusinessPartnerDefinition definition) {
            String key = key(definition.companyId(), definition.kind(), definition.code());
            if (values.putIfAbsent(key, definition) != null) {
                throw new BusinessPartnerPersistenceException(
                        BusinessPartnerPersistenceCode.GENERAL_CODE_ALREADY_EXISTS);
            }
            revisions.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .addFirst(revision(definition));
            return definition;
        }

        @Override
        public BusinessPartnerDefinition update(
                BusinessPartnerDefinition definition, long expectedPersistedVersion) {
            String key = key(definition.companyId(), definition.kind(), definition.code());
            BusinessPartnerDefinition current = values.get(key);
            if (current == null) {
                throw new BusinessPartnerPersistenceException(
                        BusinessPartnerPersistenceCode.DEFINITION_NOT_FOUND);
            }
            if (current.version() != expectedPersistedVersion) {
                throw new BusinessPartnerPersistenceException(
                        BusinessPartnerPersistenceCode.VERSION_CONFLICT);
            }
            values.put(key, definition);
            revisions.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .addFirst(revision(definition));
            return definition;
        }

        private static BusinessPartnerDefinitionRevision revision(
                BusinessPartnerDefinition definition) {
            return new BusinessPartnerDefinitionRevision(
                    definition.companyId(),
                    definition.kind(),
                    definition.code(),
                    definition.displayName(),
                    definition.state(),
                    definition.version(),
                    CLOCK.instant());
        }

        private static String key(
                CompanyId companyId,
                BusinessPartnerDefinitionKind kind,
                BusinessPartnerAttributeCode code) {
            return companyId + ":" + kind + ":" + code.value();
        }
    }
}
