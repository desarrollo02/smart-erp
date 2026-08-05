package py.com.logixone.kernel.application.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.audit.CompanyAuditActor;
import py.com.logixone.kernel.application.company.audit.CompanyAuditEvent;
import py.com.logixone.kernel.application.company.audit.CompanyAuditOperation;
import py.com.logixone.kernel.application.company.audit.CompanyAuditOutcome;
import py.com.logixone.kernel.application.company.command.ChangeCompanyStatusCommand;
import py.com.logixone.kernel.application.company.command.ChangePluginActivationCommand;
import py.com.logixone.kernel.application.company.command.RegisterCompanyCommand;
import py.com.logixone.kernel.application.company.command.ReplaceCustomizationCommand;
import py.com.logixone.kernel.application.company.port.CompanyAuditPort;
import py.com.logixone.kernel.application.company.port.CompanyIdGenerator;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.company.port.PersistenceConflictCode;
import py.com.logixone.kernel.application.company.port.PersistenceConflictException;
import py.com.logixone.kernel.application.company.port.PluginActivationRepository;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyPluginResolver;
import py.com.logixone.kernel.domain.company.CompanyStatus;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.kernel.domain.company.PluginActivationPolicy;
import py.com.logixone.kernel.domain.company.PluginActivationState;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

class CompanyUseCasesTest {

    private static final PluginId INVENTORY = new PluginId("inventory");
    private static final PluginId SALES = new PluginId("sales");
    private static final PluginId CUSTOM_A = new PluginId("custom_a");
    private static final PluginId CUSTOM_B = new PluginId("custom_b");
    private static final PluginId CUSTOM_C = new PluginId("custom_c");
    private static final PluginId CUSTOM_BAD = new PluginId("custom_bad");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-27T12:00:00Z"), ZoneOffset.UTC);

    private InMemoryRepositories repositories;
    private RecordingAudit audit;
    private SequentialIds ids;

    @BeforeEach
    void setUp() {
        repositories = new InMemoryRepositories();
        audit = new RecordingAudit();
        ids = new SequentialIds();
    }

    @Test
    void registrationRequiresAPresentCustomizationOfTheRightKindAndExclusiveOwner() {
        CompanyAdministrationService service = administration(registry(
                functional(INVENTORY), customization(CUSTOM_A)));

        CompanyOperationResult<Company> registered = service.register(
                new RegisterCompanyCommand(CUSTOM_A));
        CompanyOperationResult<Company> duplicate = service.register(
                new RegisterCompanyCommand(CUSTOM_A));
        CompanyOperationResult<Company> wrongKind = service.register(
                new RegisterCompanyCommand(INVENTORY));
        CompanyOperationResult<Company> absent = service.register(
                new RegisterCompanyCommand(new PluginId("missing_custom")));

        assertEquals(CompanyOperationStatus.CHANGED, registered.status());
        assertEquals(CompanyStatus.INACTIVE, registered.value().orElseThrow().status());
        assertFailure(duplicate, CompanyOperationCode.CUSTOMIZATION_ALREADY_ASSIGNED);
        assertFailure(wrongKind, CompanyOperationCode.CUSTOMIZATION_WRONG_KIND);
        assertFailure(absent, CompanyOperationCode.CUSTOMIZATION_NOT_PRESENT);
        assertEquals(1, repositories.companies.size());
        assertEquals(4, audit.events.size());
        assertEquals(CompanyAuditActor.TEST, audit.events.getFirst().actor());
        assertEquals(Instant.parse("2026-07-27T12:00:00Z"), audit.events.getFirst().occurredAt());
    }

    @Test
    void activatingACompanyRequiresItsCustomizationDependenciesToBeEffective() {
        PluginRegistry registry = registry(
                functional(INVENTORY),
                customization(CUSTOM_A, required(INVENTORY)));
        Company company = company(1, CompanyStatus.INACTIVE, CUSTOM_A, 0);
        repositories.seed(company);
        CompanyAdministrationService administration = administration(registry);
        PluginActivationService activations = activation(registry);

        CompanyOperationResult<Company> rejected = administration.changeStatus(
                new ChangeCompanyStatusCommand(company.id(), CompanyStatus.ACTIVE, 0));
        CompanyOperationResult<PluginActivationDecision> inventory = activations.change(
                new ChangePluginActivationCommand(
                        company.id(), INVENTORY, PluginActivationState.ENABLED, 0));
        CompanyOperationResult<Company> activated = administration.changeStatus(
                new ChangeCompanyStatusCommand(company.id(), CompanyStatus.ACTIVE, 0));

        assertFailure(rejected, CompanyOperationCode.CUSTOMIZATION_INCOMPATIBLE);
        assertTrue(inventory.changed());
        assertTrue(activated.changed());
        assertEquals(CompanyStatus.ACTIVE, repositories.company(company.id()).status());
        assertEquals(1, repositories.company(company.id()).version());
    }

    @Test
    void statusChangesAreIdempotentBeforeVersionChecksAndConflictsAreStable() {
        PluginRegistry registry = registry(customization(CUSTOM_A));
        Company company = company(1, CompanyStatus.INACTIVE, CUSTOM_A, 3);
        repositories.seed(company);
        CompanyAdministrationService service = administration(registry);

        CompanyOperationResult<Company> unchanged = service.changeStatus(
                new ChangeCompanyStatusCommand(company.id(), CompanyStatus.INACTIVE, 0));
        CompanyOperationResult<Company> conflict = service.changeStatus(
                new ChangeCompanyStatusCommand(company.id(), CompanyStatus.ACTIVE, 2));

        assertEquals(CompanyOperationStatus.UNCHANGED, unchanged.status());
        assertEquals(3, unchanged.value().orElseThrow().version());
        assertFailure(conflict, CompanyOperationCode.COMPANY_VERSION_CONFLICT);
        assertEquals(0, repositories.companyWrites);
    }

    @Test
    void activationHonorsDependenciesAndRejectsPartialDisableOrCustomizationChanges() {
        PluginRegistry registry = registry(
                functional(INVENTORY),
                functional(SALES, required(INVENTORY)),
                customization(CUSTOM_A));
        Company company = company(1, CompanyStatus.INACTIVE, CUSTOM_A, 0);
        repositories.seed(company);
        PluginActivationService service = activation(registry);

        CompanyOperationResult<PluginActivationDecision> salesWithoutInventory = service.change(
                command(company, SALES, PluginActivationState.ENABLED, 0));
        CompanyOperationResult<PluginActivationDecision> inventory = service.change(
                command(company, INVENTORY, PluginActivationState.ENABLED, 0));
        CompanyOperationResult<PluginActivationDecision> sales = service.change(
                command(company, SALES, PluginActivationState.ENABLED, 0));
        CompanyOperationResult<PluginActivationDecision> disableInventory = service.change(
                command(company, INVENTORY, PluginActivationState.DISABLED, 0));
        CompanyOperationResult<PluginActivationDecision> customization = service.change(
                command(company, CUSTOM_A, PluginActivationState.DISABLED, 0));

        assertFailure(salesWithoutInventory, CompanyOperationCode.REQUIRED_DEPENDENCY_NOT_EFFECTIVE);
        assertTrue(inventory.changed());
        assertTrue(sales.changed());
        assertFailure(disableInventory, CompanyOperationCode.ACTIVE_DEPENDENT_EXISTS);
        assertFailure(customization, CompanyOperationCode.CUSTOMIZATION_REQUIRED);
        assertEquals(2, repositories.activationWrites);
        assertTrue(repositories.activation(company.id(), INVENTORY).isEnabled());
        assertTrue(repositories.activation(company.id(), SALES).isEnabled());
    }

    @Test
    void activationIsIdempotentAndTransformsConcurrentConflicts() {
        PluginRegistry registry = registry(functional(INVENTORY), customization(CUSTOM_A));
        Company company = company(1, CompanyStatus.INACTIVE, CUSTOM_A, 0);
        repositories.seed(company);
        repositories.seed(new PluginActivationDecision(
                company.id(), INVENTORY, PluginActivationState.ENABLED, 4));
        PluginActivationService service = activation(registry);

        CompanyOperationResult<PluginActivationDecision> unchanged = service.change(
                command(company, INVENTORY, PluginActivationState.ENABLED, 0));
        CompanyOperationResult<PluginActivationDecision> conflict = service.change(
                command(company, INVENTORY, PluginActivationState.DISABLED, 3));

        assertEquals(CompanyOperationStatus.UNCHANGED, unchanged.status());
        assertEquals(4, unchanged.value().orElseThrow().version());
        assertFailure(conflict, CompanyOperationCode.ACTIVATION_VERSION_CONFLICT);
        assertEquals(0, repositories.activationWrites);
    }

    @Test
    void twoCompaniesKeepIndependentDesiredAndEffectiveActivation() {
        PluginRegistry registry = registry(
                functional(INVENTORY), customization(CUSTOM_A), customization(CUSTOM_B));
        Company first = company(1, CompanyStatus.ACTIVE, CUSTOM_A, 0);
        Company second = company(2, CompanyStatus.ACTIVE, CUSTOM_B, 0);
        repositories.seed(first);
        repositories.seed(second);
        PluginActivationService activations = activation(registry);
        activations.change(command(first, INVENTORY, PluginActivationState.ENABLED, 0));

        CompanyPluginQueryService query = query(registry);

        assertEquals(
                List.of(INVENTORY, CUSTOM_A),
                query.resolve(first.id()).resolution().orElseThrow().orderedPlugins().stream()
                        .map(PluginDescriptor::id)
                        .toList());
        assertEquals(
                List.of(CUSTOM_B),
                query.resolve(second.id()).resolution().orElseThrow().orderedPlugins().stream()
                        .map(PluginDescriptor::id)
                        .toList());
        assertTrue(repositories.findByCompanyAndPlugin(second.id(), INVENTORY).isEmpty());
    }

    @Test
    void replacementValidatesEverythingBeforeWriteAndKeepsExactlyOneCustomization() {
        PluginRegistry registry = registry(
                functional(INVENTORY),
                functional(SALES),
                customization(CUSTOM_A),
                customization(CUSTOM_B, required(INVENTORY)),
                customization(CUSTOM_C),
                customization(CUSTOM_BAD, required(SALES)));
        Company first = company(1, CompanyStatus.ACTIVE, CUSTOM_A, 0);
        Company second = company(2, CompanyStatus.INACTIVE, CUSTOM_C, 0);
        repositories.seed(first);
        repositories.seed(second);
        repositories.seed(new PluginActivationDecision(
                first.id(), INVENTORY, PluginActivationState.ENABLED, 0));
        CompanyAdministrationService service = administration(registry);

        CompanyOperationResult<Company> absent = service.replaceCustomization(
                new ReplaceCustomizationCommand(first.id(), new PluginId("missing_custom"), 0));
        CompanyOperationResult<Company> owned = service.replaceCustomization(
                new ReplaceCustomizationCommand(first.id(), CUSTOM_C, 0));
        CompanyOperationResult<Company> incompatible = service.replaceCustomization(
                new ReplaceCustomizationCommand(first.id(), CUSTOM_BAD, 0));
        CompanyOperationResult<Company> changed = service.replaceCustomization(
                new ReplaceCustomizationCommand(first.id(), CUSTOM_B, 0));
        CompanyOperationResult<Company> unchanged = service.replaceCustomization(
                new ReplaceCustomizationCommand(first.id(), CUSTOM_B, 0));

        assertFailure(absent, CompanyOperationCode.CUSTOMIZATION_NOT_PRESENT);
        assertFailure(owned, CompanyOperationCode.CUSTOMIZATION_ALREADY_ASSIGNED);
        assertFailure(incompatible, CompanyOperationCode.CUSTOMIZATION_INCOMPATIBLE);
        assertTrue(changed.changed());
        assertEquals(CUSTOM_B, repositories.company(first.id()).customizationPluginId());
        assertEquals(1, repositories.company(first.id()).version());
        assertEquals(CompanyOperationStatus.UNCHANGED, unchanged.status());
        assertEquals(1, repositories.companyWrites);
    }

    @Test
    void guardNeverInvokesTheCallbackWhenCompanyOrPluginIsNotEffective() {
        PluginRegistry registry = registry(
                functional(INVENTORY), customization(CUSTOM_A), customization(CUSTOM_B));
        Company active = company(1, CompanyStatus.ACTIVE, CUSTOM_A, 0);
        Company inactive = company(2, CompanyStatus.INACTIVE, CUSTOM_B, 0);
        repositories.seed(active);
        repositories.seed(inactive);
        repositories.seed(new PluginActivationDecision(
                active.id(), INVENTORY, PluginActivationState.ENABLED, 0));
        AtomicBoolean invoked = new AtomicBoolean();

        PluginOperationGuard inactiveGuard = guard(inactive.id(), registry);
        PluginOperationDeniedException inactiveFailure = assertThrows(
                PluginOperationDeniedException.class,
                () -> inactiveGuard.execute(INVENTORY, () -> {
                    invoked.set(true);
                    return "forbidden";
                }));
        PluginOperationGuard disabledGuard = guard(active.id(), registry);
        assertThrows(
                PluginOperationDeniedException.class,
                () -> disabledGuard.execute(SALES, () -> {
                    invoked.set(true);
                    return "forbidden";
                }));
        PluginOperationGuard missingCompanyGuard = guard(companyId(99), registry);
        assertThrows(
                PluginOperationDeniedException.class,
                () -> missingCompanyGuard.execute(INVENTORY, () -> {
                    invoked.set(true);
                    return "forbidden";
                }));

        assertEquals("PLUGIN_OPERATION_DENIED", inactiveFailure.getMessage());
        assertFalse(invoked.get());
    }

    @Test
    void guardInvokesAnEffectiveFunctionalPluginExactlyOnce() {
        PluginRegistry registry = registry(functional(INVENTORY), customization(CUSTOM_A));
        Company active = company(1, CompanyStatus.ACTIVE, CUSTOM_A, 0);
        repositories.seed(active);
        repositories.seed(new PluginActivationDecision(
                active.id(), INVENTORY, PluginActivationState.ENABLED, 0));
        AtomicLong calls = new AtomicLong();

        String value = guard(active.id(), registry).execute(INVENTORY, () -> {
            calls.incrementAndGet();
            return "ok";
        });

        assertEquals("ok", value);
        assertEquals(1, calls.get());
        CompanyAuditEvent event = audit.events.getLast();
        assertEquals(CompanyAuditOperation.VERIFY_PLUGIN_ACCESS, event.operation());
        assertEquals(CompanyAuditOutcome.ALLOWED, event.outcome());
    }

    @Test
    void resultModelRejectsAmbiguousSuccessOrFailureCombinations() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CompanyOperationResult<>(
                        CompanyOperationStatus.REJECTED,
                        Optional.of("value"),
                        Optional.of(CompanyOperationCode.COMPANY_NOT_FOUND)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new CompanyOperationResult<String>(
                        CompanyOperationStatus.CHANGED,
                        Optional.empty(),
                        Optional.empty()));
    }

    private CompanyAdministrationService administration(PluginRegistry registry) {
        return new CompanyAdministrationService(
                repositories,
                repositories,
                ids,
                registry,
                new CompanyPluginResolver(),
                audit,
                CLOCK,
                CompanyAuditActor.TEST);
    }

    private PluginActivationService activation(PluginRegistry registry) {
        return new PluginActivationService(
                repositories,
                repositories,
                registry,
                new PluginActivationPolicy(),
                audit,
                CLOCK,
                CompanyAuditActor.TEST);
    }

    private CompanyPluginQueryService query(PluginRegistry registry) {
        return new CompanyPluginQueryService(
                repositories,
                repositories,
                registry,
                new CompanyPluginResolver());
    }

    private PluginOperationGuard guard(CompanyId companyId, PluginRegistry registry) {
        return new PluginOperationGuard(
                () -> companyId,
                query(registry),
                audit,
                CLOCK,
                CompanyAuditActor.TEST);
    }

    private static ChangePluginActivationCommand command(
            Company company,
            PluginId pluginId,
            PluginActivationState state,
            long expectedVersion) {
        return new ChangePluginActivationCommand(company.id(), pluginId, state, expectedVersion);
    }

    private static void assertFailure(
            CompanyOperationResult<?> result,
            CompanyOperationCode code) {
        assertEquals(CompanyOperationStatus.REJECTED, result.status());
        assertEquals(code, result.failure().orElseThrow());
        assertTrue(result.value().isEmpty());
    }

    private static Company company(long id, CompanyStatus status, PluginId customization, long version) {
        return new Company(companyId(id), status, customization, version);
    }

    private static CompanyId companyId(long id) {
        return new CompanyId(new UUID(0, id));
    }

    private static PluginRegistry registry(PluginDescriptor... descriptors) {
        return PluginRegistry.create(List.of(descriptors).stream()
                .<PluginDefinition>map(descriptor -> () -> descriptor)
                .toList());
    }

    private static PluginDescriptor functional(PluginId id, PluginDependency... dependencies) {
        return descriptor(id, PluginKind.FUNCTIONAL, dependencies);
    }

    private static PluginDescriptor customization(PluginId id, PluginDependency... dependencies) {
        return descriptor(id, PluginKind.CUSTOMIZATION, dependencies);
    }

    private static PluginDescriptor descriptor(
            PluginId id,
            PluginKind kind,
            PluginDependency... dependencies) {
        return new PluginDescriptor(
                id,
                kind,
                SemanticVersion.parse("1.0.0"),
                new VersionRange(
                        SemanticVersion.parse("0.4.0"),
                        SemanticVersion.parse("0.5.0")),
                id.value(),
                List.of(dependencies),
                List.of(), List.of(), List.of(), List.of());
    }

    private static PluginDependency required(PluginId id) {
        return new PluginDependency(
                id,
                new VersionRange(
                        SemanticVersion.parse("1.0.0"),
                        SemanticVersion.parse("2.0.0")),
                DependencyKind.REQUIRED);
    }

    private static final class SequentialIds implements CompanyIdGenerator {
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public CompanyId nextId() {
            return companyId(sequence.incrementAndGet());
        }
    }

    private static final class RecordingAudit implements CompanyAuditPort {
        private final List<CompanyAuditEvent> events = new ArrayList<>();

        @Override
        public void record(CompanyAuditEvent event) {
            events.add(event);
        }
    }

    private static final class InMemoryRepositories
            implements CompanyRepository, PluginActivationRepository {
        private final Map<CompanyId, Company> companies = new HashMap<>();
        private final Map<String, PluginActivationDecision> activations = new HashMap<>();
        private int companyWrites;
        private int activationWrites;

        void seed(Company company) {
            companies.put(company.id(), company);
        }

        void seed(PluginActivationDecision activation) {
            activations.put(key(activation.companyId(), activation.pluginId()), activation);
        }

        Company company(CompanyId id) {
            return companies.get(id);
        }

        PluginActivationDecision activation(CompanyId companyId, PluginId pluginId) {
            return activations.get(key(companyId, pluginId));
        }

        @Override
        public List<Company> findAll() {
            return companies.values().stream().sorted((left, right) -> left.id().compareTo(right.id())).toList();
        }

        @Override
        public Optional<Company> findById(CompanyId companyId) {
            return Optional.ofNullable(companies.get(companyId));
        }

        @Override
        public Company save(Company desired) {
            Company current = companies.get(desired.id());
            if (current == null) {
                if (isCustomizationAssignedToAnotherCompany(
                        desired.customizationPluginId(), desired.id())) {
                    throw new PersistenceConflictException(
                            PersistenceConflictCode.CUSTOMIZATION_ALREADY_ASSIGNED);
                }
                companies.put(desired.id(), desired);
                companyWrites++;
                return desired;
            }
            if (current.version() != desired.version()) {
                throw new PersistenceConflictException(
                        PersistenceConflictCode.COMPANY_VERSION_CONFLICT);
            }
            if (current.status() == desired.status()
                    && current.customizationPluginId().equals(desired.customizationPluginId())) {
                return current;
            }
            if (isCustomizationAssignedToAnotherCompany(
                    desired.customizationPluginId(), desired.id())) {
                throw new PersistenceConflictException(
                        PersistenceConflictCode.CUSTOMIZATION_ALREADY_ASSIGNED);
            }
            Company stored = new Company(
                    desired.id(),
                    desired.status(),
                    desired.customizationPluginId(),
                    current.version() + 1);
            companies.put(stored.id(), stored);
            companyWrites++;
            return stored;
        }

        @Override
        public boolean isCustomizationAssignedToAnotherCompany(
                PluginId customizationPluginId,
                CompanyId companyId) {
            return companies.values().stream()
                    .anyMatch(company -> !company.id().equals(companyId)
                            && company.customizationPluginId().equals(customizationPluginId));
        }

        @Override
        public List<PluginActivationDecision> findByCompanyId(CompanyId companyId) {
            return activations.values().stream()
                    .filter(decision -> decision.companyId().equals(companyId))
                    .sorted((left, right) -> left.pluginId().compareTo(right.pluginId()))
                    .toList();
        }

        @Override
        public Optional<PluginActivationDecision> findByCompanyAndPlugin(
                CompanyId companyId,
                PluginId pluginId) {
            return Optional.ofNullable(activation(companyId, pluginId));
        }

        @Override
        public PluginActivationDecision save(PluginActivationDecision desired) {
            String key = key(desired.companyId(), desired.pluginId());
            PluginActivationDecision current = activations.get(key);
            if (current == null) {
                activations.put(key, desired);
                activationWrites++;
                return desired;
            }
            if (current.version() != desired.version()) {
                throw new PersistenceConflictException(
                        PersistenceConflictCode.ACTIVATION_VERSION_CONFLICT);
            }
            if (current.desiredState() == desired.desiredState()) {
                return current;
            }
            PluginActivationDecision stored = new PluginActivationDecision(
                    desired.companyId(),
                    desired.pluginId(),
                    desired.desiredState(),
                    current.version() + 1);
            activations.put(key, stored);
            activationWrites++;
            return stored;
        }

        private static String key(CompanyId companyId, PluginId pluginId) {
            return companyId + ":" + pluginId;
        }
    }
}
