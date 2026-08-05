package py.com.logixone.kernel.application.company.contribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyContext;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.CompanyPluginQueryService;
import py.com.logixone.kernel.application.company.PluginOperationDeniedException;
import py.com.logixone.kernel.application.company.PluginOperationGuard;
import py.com.logixone.kernel.application.company.audit.CompanyAuditActor;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.company.port.PluginActivationRepository;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyPluginDiagnosticCode;
import py.com.logixone.kernel.domain.company.CompanyPluginResolver;
import py.com.logixone.kernel.domain.company.CompanyStatus;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.kernel.domain.company.PluginActivationState;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.MenuContribution;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

class CompanyContributionServiceTest {

    private static final CompanyId COMPANY_A = new CompanyId(new UUID(0, 101));
    private static final CompanyId COMPANY_B = new CompanyId(new UUID(0, 102));
    private static final PluginId FUNCTIONAL = new PluginId("functional");
    private static final PluginId CUSTOM_A = new PluginId("custom_a");
    private static final PluginId CUSTOM_B = new PluginId("custom_b");
    private static final VersionRange API_RANGE = new VersionRange(
            SemanticVersion.parse("0.4.0"), SemanticVersion.parse("0.5.0"));
    private static final VersionRange VERSION_ONE = new VersionRange(
            SemanticVersion.parse("1.0.0"), SemanticVersion.parse("2.0.0"));

    @Test
    void isolatesTwoCompaniesAndPlacesOnlyTheirCustomizationLast() {
        Repositories repositories = new Repositories();
        repositories.addCompany(company(COMPANY_A, CompanyStatus.ACTIVE, CUSTOM_A));
        repositories.addCompany(company(COMPANY_B, CompanyStatus.ACTIVE, CUSTOM_B));
        repositories.addActivation(enabled(COMPANY_A, FUNCTIONAL));
        repositories.addActivation(disabled(COMPANY_B, FUNCTIONAL));
        PluginDescriptor functional = descriptor(
                FUNCTIONAL.value(),
                PluginKind.FUNCTIONAL,
                List.of(),
                List.of("functional.orders", "functional.reports"));
        PluginDescriptor customA = descriptor(
                CUSTOM_A.value(), PluginKind.CUSTOMIZATION, List.of(), List.of("custom.a"));
        PluginDescriptor customB = descriptor(
                CUSTOM_B.value(), PluginKind.CUSTOMIZATION, List.of(), List.of("custom.b"));
        CompanyContributionService service = service(
                repositories,
                List.of(definition(customB), definition(functional), definition(customA)));

        CompanyContributions first = service.compose(COMPANY_A);
        CompanyContributions second = service.compose(COMPANY_B);

        assertTrue(first.operational());
        assertTrue(second.operational());
        assertEquals(List.of("functional", "custom_a"), pluginIds(first));
        assertEquals(List.of("custom_b"), pluginIds(second));
        assertEquals(
                List.of("functional.orders", "functional.reports", "custom.a"),
                contributionIds(first.capabilities()));
        assertEquals(List.of("custom.b"), contributionIds(second.capabilities()));
        assertFalse(pluginIds(first).contains("custom_b"));
        assertFalse(pluginIds(second).contains("custom_a"));
        assertEquals(
                List.of("functional.menu", "custom_a.menu"),
                contributionIds(first.menuContributions().stream()
                        .map(MenuContribution::id)
                        .toList()));
    }

    @Test
    void returnsEmptySafeViewsForNotFoundInactiveAndMissingCustomization() {
        Repositories repositories = new Repositories();
        repositories.addCompany(company(COMPANY_A, CompanyStatus.INACTIVE, CUSTOM_A));
        repositories.addCompany(company(COMPANY_B, CompanyStatus.ACTIVE, CUSTOM_B));
        CompanyContributionService service = service(
                repositories,
                List.of(definition(descriptor(
                        CUSTOM_A.value(), PluginKind.CUSTOMIZATION, List.of(), List.of("custom.a")))));

        CompanyContributions notFound = service.compose(new CompanyId(new UUID(0, 999)));
        CompanyContributions inactive = service.compose(COMPANY_A);
        CompanyContributions missingCustomization = service.compose(COMPANY_B);

        assertEquals(
                CompanyPluginDiagnosticCode.COMPANY_NOT_FOUND,
                notFound.failure().orElseThrow());
        assertEmpty(notFound);
        assertDiagnostic(inactive, CompanyPluginDiagnosticCode.COMPANY_INACTIVE);
        assertEmpty(inactive);
        assertDiagnostic(
                missingCustomization,
                CompanyPluginDiagnosticCode.CUSTOMIZATION_NOT_PRESENT);
        assertEmpty(missingCustomization);
    }

    @Test
    void ignoresPersistedDecisionsForPhysicallyAbsentPluginsWithoutGhostContributions() {
        PluginId absent = new PluginId("absent_plugin");
        Repositories repositories = new Repositories();
        repositories.addCompany(company(COMPANY_A, CompanyStatus.ACTIVE, CUSTOM_A));
        repositories.addActivation(enabled(COMPANY_A, absent));
        CompanyContributionService service = service(
                repositories,
                List.of(definition(descriptor(
                        CUSTOM_A.value(), PluginKind.CUSTOMIZATION, List.of(), List.of("custom.a")))));

        CompanyContributions result = service.compose(COMPANY_A);

        assertTrue(result.operational());
        assertEquals(List.of("custom_a"), pluginIds(result));
        assertEquals(List.of("custom.a"), contributionIds(result.capabilities()));
        assertDiagnostic(result, CompanyPluginDiagnosticCode.PLUGIN_NOT_PRESENT);
    }

    @Test
    void respectsRequiredDependenciesAndIgnoresAnAbsentOptionalDependency() {
        PluginId baseId = new PluginId("base");
        PluginId featureId = new PluginId("feature");
        PluginDependency requiredBase = new PluginDependency(
                baseId, VERSION_ONE, DependencyKind.REQUIRED);
        PluginDependency optionalAbsent = new PluginDependency(
                new PluginId("optional_absent"), VERSION_ONE, DependencyKind.OPTIONAL);
        PluginDescriptor base = descriptor(
                baseId.value(), PluginKind.FUNCTIONAL, List.of(), List.of("base.capability"));
        PluginDescriptor feature = descriptor(
                featureId.value(),
                PluginKind.FUNCTIONAL,
                List.of(requiredBase, optionalAbsent),
                List.of("feature.capability"));
        PluginDescriptor customA = descriptor(
                CUSTOM_A.value(), PluginKind.CUSTOMIZATION, List.of(), List.of("custom.a"));
        Repositories repositories = new Repositories();
        repositories.addCompany(company(COMPANY_A, CompanyStatus.ACTIVE, CUSTOM_A));
        repositories.addCompany(company(COMPANY_B, CompanyStatus.ACTIVE, CUSTOM_B));
        repositories.addActivation(enabled(COMPANY_A, baseId));
        repositories.addActivation(enabled(COMPANY_A, featureId));
        repositories.addActivation(enabled(COMPANY_B, featureId));
        PluginDescriptor customB = descriptor(
                CUSTOM_B.value(), PluginKind.CUSTOMIZATION, List.of(), List.of("custom.b"));
        List<PluginDefinition> definitions = List.of(
                definition(feature), definition(customB), definition(base), definition(customA));
        CompanyContributionService service = service(repositories, definitions);

        CompanyContributions first = service.compose(COMPANY_A);
        CompanyContributions second = service.compose(COMPANY_B);
        CompanyContributionService permutedService = service(
                repositories,
                List.of(definition(customA), definition(base), definition(feature), definition(customB)));

        assertEquals(List.of("base", "feature", "custom_a"), pluginIds(first));
        assertEquals(pluginIds(first), pluginIds(permutedService.compose(COMPANY_A)));
        assertEquals(List.of("custom_b"), pluginIds(second));
        assertDiagnostic(
                second,
                CompanyPluginDiagnosticCode.REQUIRED_DEPENDENCY_NOT_EFFECTIVE);
    }

    @Test
    void collectionsAreImmutableAndMenuFilteringDoesNotReplaceTheOperationGuard() {
        Repositories repositories = new Repositories();
        repositories.addCompany(company(COMPANY_A, CompanyStatus.ACTIVE, CUSTOM_A));
        PluginDescriptor functional = descriptor(
                FUNCTIONAL.value(), PluginKind.FUNCTIONAL, List.of(), List.of("functional.capability"));
        PluginDescriptor customization = descriptor(
                CUSTOM_A.value(), PluginKind.CUSTOMIZATION, List.of(), List.of("custom.a"));
        List<PluginDefinition> definitions = List.of(definition(functional), definition(customization));
        CompanyPluginQueryService query = queryService(repositories, definitions);
        CompanyContributions result = new CompanyContributionService(query).compose(COMPANY_A);

        assertEquals(List.of("custom_a"), pluginIds(result));
        assertEquals(List.of("custom_a.menu"), contributionIds(result.menuContributions().stream()
                .map(MenuContribution::id)
                .toList()));
        assertThrows(UnsupportedOperationException.class, () -> result.plugins().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.capabilities().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> result.plugins().getFirst().menuContributions().clear());

        CompanyContext context = () -> COMPANY_A;
        PluginOperationGuard guard = new PluginOperationGuard(
                context,
                query,
                event -> { },
                Clock.systemUTC(),
                CompanyAuditActor.TEST);
        AtomicBoolean invoked = new AtomicBoolean();

        assertThrows(
                PluginOperationDeniedException.class,
                () -> guard.execute(FUNCTIONAL, () -> {
                    invoked.set(true);
                    return "forbidden";
                }));
        assertFalse(invoked.get());
    }

    private static CompanyContributionService service(
            Repositories repositories,
            List<PluginDefinition> definitions) {
        return new CompanyContributionService(queryService(repositories, definitions));
    }

    private static CompanyPluginQueryService queryService(
            Repositories repositories,
            List<PluginDefinition> definitions) {
        return new CompanyPluginQueryService(
                repositories,
                repositories,
                PluginRegistry.create(definitions),
                new CompanyPluginResolver());
    }

    private static Company company(
            CompanyId id,
            CompanyStatus status,
            PluginId customizationId) {
        return new Company(id, status, customizationId, 0);
    }

    private static PluginActivationDecision enabled(CompanyId companyId, PluginId pluginId) {
        return new PluginActivationDecision(
                companyId, pluginId, PluginActivationState.ENABLED, 0);
    }

    private static PluginActivationDecision disabled(CompanyId companyId, PluginId pluginId) {
        return new PluginActivationDecision(
                companyId, pluginId, PluginActivationState.DISABLED, 0);
    }

    private static PluginDescriptor descriptor(
            String id,
            PluginKind kind,
            List<PluginDependency> dependencies,
            List<String> capabilities) {
        ContributionId permission = new ContributionId(id + ".permission");
        return new PluginDescriptor(
                new PluginId(id),
                kind,
                SemanticVersion.parse("1.0.0"),
                API_RANGE,
                id,
                dependencies,
                capabilities.stream().map(ContributionId::new).toList(),
                List.of(permission),
                List.of(new MenuContribution(
                        new ContributionId(id + ".menu"),
                        id + ".menu",
                        "/" + id,
                        Optional.of(permission))),
                List.of());
    }

    private static PluginDefinition definition(PluginDescriptor descriptor) {
        return () -> descriptor;
    }

    private static List<String> pluginIds(CompanyContributions contributions) {
        return contributions.plugins().stream()
                .map(plugin -> plugin.pluginId().value())
                .toList();
    }

    private static List<String> contributionIds(List<ContributionId> contributions) {
        return contributions.stream().map(ContributionId::value).toList();
    }

    private static void assertEmpty(CompanyContributions contributions) {
        assertFalse(contributions.operational());
        assertTrue(contributions.plugins().isEmpty());
        assertTrue(contributions.capabilities().isEmpty());
        assertTrue(contributions.permissions().isEmpty());
        assertTrue(contributions.menuContributions().isEmpty());
    }

    private static void assertDiagnostic(
            CompanyContributions contributions,
            CompanyPluginDiagnosticCode expected) {
        assertTrue(contributions.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code() == expected));
    }

    private static final class Repositories implements CompanyRepository, PluginActivationRepository {
        private final Map<CompanyId, Company> companies = new HashMap<>();
        private final List<PluginActivationDecision> activations = new ArrayList<>();

        void addCompany(Company company) {
            companies.put(company.id(), company);
        }

        void addActivation(PluginActivationDecision decision) {
            activations.add(decision);
        }

        @Override
        public List<Company> findAll() {
            return companies.values().stream().toList();
        }

        @Override
        public Optional<Company> findById(CompanyId companyId) {
            return Optional.ofNullable(companies.get(companyId));
        }

        @Override
        public Company save(Company company) {
            companies.put(company.id(), company);
            return company;
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
            return activations.stream()
                    .filter(decision -> decision.companyId().equals(companyId))
                    .toList();
        }

        @Override
        public Optional<PluginActivationDecision> findByCompanyAndPlugin(
                CompanyId companyId,
                PluginId pluginId) {
            return activations.stream()
                    .filter(decision -> decision.companyId().equals(companyId)
                            && decision.pluginId().equals(pluginId))
                    .findFirst();
        }

        @Override
        public PluginActivationDecision save(PluginActivationDecision decision) {
            activations.add(decision);
            return decision;
        }
    }
}
