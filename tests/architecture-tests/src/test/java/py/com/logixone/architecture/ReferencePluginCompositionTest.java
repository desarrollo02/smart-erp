package py.com.logixone.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.CompanyPluginQueryService;
import py.com.logixone.kernel.application.company.contribution.CompanyContributionService;
import py.com.logixone.kernel.application.company.contribution.CompanyContributions;
import py.com.logixone.kernel.application.company.screen.CompanyScreenComposer;
import py.com.logixone.kernel.application.company.screen.CompanyScreenComposition;
import py.com.logixone.kernel.application.company.screen.CompanyScreenService;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.company.port.PluginActivationRepository;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyPluginResolver;
import py.com.logixone.kernel.domain.company.CompanyStatus;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.kernel.domain.company.PluginActivationState;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.MenuContribution;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;
import py.com.logixone.plugins.reference.ReferencePluginDefinition;
import py.com.logixone.plugins.customization.a.ReferenceCustomizationADefinition;
import py.com.logixone.plugins.customization.b.ReferenceCustomizationBDefinition;

class ReferencePluginCompositionTest {

    private static final CompanyId COMPANY_ID = new CompanyId(new UUID(0, 501));
    private static final PluginId REFERENCE_ID = new PluginId("reference_plugin");
    private static final PluginId CUSTOMIZATION_ID = new PluginId("reference_customization");

    @Test
    void composesTheContributionsDeclaredByThePhysicallyRealReferencePlugin() {
        Company company = new Company(
                COMPANY_ID,
                CompanyStatus.ACTIVE,
                CUSTOMIZATION_ID,
                0);
        PluginActivationDecision activation = new PluginActivationDecision(
                COMPANY_ID,
                REFERENCE_ID,
                PluginActivationState.ENABLED,
                0);
        CompanyRepository companies = new FixedCompanyRepository(company);
        PluginActivationRepository activations = new FixedActivationRepository(activation);
        PluginDefinition customization = () -> customizationDescriptor();
        PluginRegistry registry = PluginRegistry.create(List.of(
                customization,
                new ReferencePluginDefinition()));
        CompanyContributionService service = new CompanyContributionService(
                new CompanyPluginQueryService(
                        companies,
                        activations,
                        registry,
                        new CompanyPluginResolver()));

        CompanyContributions result = service.compose(COMPANY_ID);

        assertTrue(result.operational());
        assertEquals(
                List.of("reference_plugin", "reference_customization"),
                result.plugins().stream()
                        .map(plugin -> plugin.pluginId().value())
                        .toList());
        assertEquals(
                List.of("reference.dashboard", "reference.custom.banner"),
                result.capabilities().stream().map(ContributionId::value).toList());
        assertEquals(
                List.of("reference.dashboard.view", "reference.custom.view"),
                result.permissions().stream().map(ContributionId::value).toList());
        assertEquals(
                List.of("reference.menu", "reference.custom.menu"),
                result.menuContributions().stream()
                        .map(MenuContribution::id)
                        .map(ContributionId::value)
                        .toList());
    }

    @Test
    void appliesTwoPhysicalCustomizationPluginsToTheReferenceScreenWithoutCrossLeakage() {
        CompanyScreenComposition first = composeScreens(
                new ReferenceCustomizationADefinition().descriptor().id());
        CompanyScreenComposition second = composeScreens(
                new ReferenceCustomizationBDefinition().descriptor().id());

        assertTrue(first.operational());
        assertTrue(second.operational());
        var firstScreen = first.screens().getFirst();
        var secondScreen = second.screens().getFirst();
        var firstSummary = firstScreen.elements().stream()
                .filter(element -> element.id().value().equals("summary"))
                .findFirst()
                .orElseThrow();
        var secondSummary = secondScreen.elements().stream()
                .filter(element -> element.id().value().equals("summary"))
                .findFirst()
                .orElseThrow();

        assertEquals("reference_custom_a.dashboard.summary", firstSummary.labelKey().value());
        assertTrue(firstSummary.visible());
        assertTrue(firstSummary.required());
        assertEquals("reference_custom_b.dashboard.summary", secondSummary.labelKey().value());
        assertTrue(!secondSummary.visible());
        assertEquals("reference_custom_a", firstScreen.slotContents().getFirst()
                .fragmentId().ownerPluginId().value());
        assertEquals("reference_custom_b", secondScreen.slotContents().getFirst()
                .fragmentId().ownerPluginId().value());
    }

    private static CompanyScreenComposition composeScreens(PluginId customizationId) {
        Company company = new Company(COMPANY_ID, CompanyStatus.ACTIVE, customizationId, 0);
        PluginActivationDecision activation = new PluginActivationDecision(
                COMPANY_ID,
                REFERENCE_ID,
                PluginActivationState.ENABLED,
                0);
        CompanyRepository companies = new FixedCompanyRepository(company);
        PluginActivationRepository activations = new FixedActivationRepository(activation);
        PluginRegistry registry = PluginRegistry.create(List.of(
                new ReferenceCustomizationBDefinition(),
                new ReferencePluginDefinition(),
                new ReferenceCustomizationADefinition()));
        CompanyContributionService contributionService = new CompanyContributionService(
                new CompanyPluginQueryService(
                        companies,
                        activations,
                        registry,
                        new CompanyPluginResolver()));
        return new CompanyScreenService(contributionService, new CompanyScreenComposer())
                .compose(COMPANY_ID);
    }

    private static PluginDescriptor customizationDescriptor() {
        ContributionId permission = new ContributionId("reference.custom.view");
        return new PluginDescriptor(
                CUSTOMIZATION_ID,
                PluginKind.CUSTOMIZATION,
                SemanticVersion.parse("1.0.0"),
                new VersionRange(
                        SemanticVersion.parse("0.4.0"),
                        SemanticVersion.parse("0.5.0")),
                "Reference customization fixture",
                List.of(),
                List.of(new ContributionId("reference.custom.banner")),
                List.of(permission),
                List.of(new MenuContribution(
                        new ContributionId("reference.custom.menu"),
                        "reference.custom.menu",
                        "/reference/custom",
                        Optional.of(permission))),
                List.of());
    }

    private record FixedCompanyRepository(Company company) implements CompanyRepository {

        @Override
        public List<Company> findAll() {
            return List.of(company);
        }

        @Override
        public Optional<Company> findById(CompanyId companyId) {
            return company.id().equals(companyId) ? Optional.of(company) : Optional.empty();
        }

        @Override
        public Company save(Company candidate) {
            throw new UnsupportedOperationException("read-only fixture");
        }

        @Override
        public boolean isCustomizationAssignedToAnotherCompany(
                PluginId customizationPluginId,
                CompanyId companyId) {
            return false;
        }
    }

    private record FixedActivationRepository(
            PluginActivationDecision activation) implements PluginActivationRepository {

        @Override
        public List<PluginActivationDecision> findByCompanyId(CompanyId companyId) {
            return activation.companyId().equals(companyId)
                    ? List.of(activation)
                    : List.of();
        }

        @Override
        public Optional<PluginActivationDecision> findByCompanyAndPlugin(
                CompanyId companyId,
                PluginId pluginId) {
            return activation.companyId().equals(companyId)
                            && activation.pluginId().equals(pluginId)
                    ? Optional.of(activation)
                    : Optional.empty();
        }

        @Override
        public PluginActivationDecision save(PluginActivationDecision decision) {
            throw new UnsupportedOperationException("read-only fixture");
        }
    }
}
