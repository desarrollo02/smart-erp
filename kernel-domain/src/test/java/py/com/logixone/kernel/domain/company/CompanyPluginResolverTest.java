package py.com.logixone.kernel.domain.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

class CompanyPluginResolverTest {

    private static final VersionRange API_RANGE = range("0.4.0", "0.5.0");
    private static final VersionRange VERSION_ONE = range("1.0.0", "2.0.0");
    private static final CompanyId COMPANY_A = companyId(1);
    private static final CompanyId COMPANY_B = companyId(2);
    private static final PluginId CUSTOM_A = new PluginId("custom_a");
    private final CompanyPluginResolver resolver = new CompanyPluginResolver();

    @Test
    void inactiveCompanyHasNoEffectivePlugins() {
        Company company = company(COMPANY_A, CompanyStatus.INACTIVE, CUSTOM_A);

        CompanyPluginResolution result = resolver.resolve(
                company, false, List.of(), List.of(customization("custom_a", List.of())));

        assertFalse(result.operational());
        assertTrue(result.orderedPlugins().isEmpty());
        assertCodes(result, CompanyPluginDiagnosticCode.COMPANY_INACTIVE);
    }

    @Test
    void missingWrongOrSharedCustomizationQuarantinesOnlyTheCompany() {
        Company companyA = company(COMPANY_A, CompanyStatus.ACTIVE, CUSTOM_A);

        CompanyPluginResolution missing = resolver.resolve(
                companyA, false, List.of(), List.of());
        CompanyPluginResolution wrongKind = resolver.resolve(
                companyA, false, List.of(), List.of(functional("custom_a", List.of())));
        CompanyPluginResolution shared = resolver.resolve(
                companyA,
                true,
                List.of(),
                List.of(customization("custom_a", List.of())));

        assertCodes(missing, CompanyPluginDiagnosticCode.CUSTOMIZATION_NOT_PRESENT);
        assertCodes(wrongKind, CompanyPluginDiagnosticCode.CUSTOMIZATION_WRONG_KIND);
        assertCodes(shared, CompanyPluginDiagnosticCode.CUSTOMIZATION_ALREADY_ASSIGNED);
    }

    @Test
    void composesEffectiveFunctionalPluginsBeforeTheAssignedCustomization() {
        PluginDescriptor inventory = functional("inventory", List.of());
        PluginDescriptor sales = functional(
                "sales", List.of(requiredDependency("inventory")));
        PluginDescriptor customA = customization("custom_a", List.of());
        Company companyA = company(COMPANY_A, CompanyStatus.ACTIVE, CUSTOM_A);

        CompanyPluginResolution result = resolver.resolve(
                companyA,
                false,
                List.of(
                        enabled(COMPANY_B, "foreign_plugin"),
                        enabled(COMPANY_A, "sales"),
                        enabled(COMPANY_A, "inventory")),
                List.of(customA, sales, inventory));

        assertTrue(result.operational());
        assertEquals(List.of("inventory", "sales", "custom_a"), ids(result));
        assertTrue(result.diagnostics().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.orderedPlugins().clear());
    }

    @Test
    void absentDecisionIsDisabledAndBrokenFunctionalDependencyDoesNotLeakPartialPlugin() {
        PluginDescriptor inventory = functional("inventory", List.of());
        PluginDescriptor sales = functional(
                "sales", List.of(requiredDependency("inventory")));
        PluginDescriptor reports = functional("reports", List.of());
        PluginDescriptor customA = customization("custom_a", List.of());
        Company company = company(COMPANY_A, CompanyStatus.ACTIVE, CUSTOM_A);

        CompanyPluginResolution result = resolver.resolve(
                company,
                false,
                List.of(enabled(COMPANY_A, "sales"), enabled(COMPANY_A, "reports")),
                List.of(sales, customA, reports, inventory));

        assertTrue(result.operational());
        assertEquals(List.of("reports", "custom_a"), ids(result));
        assertCodes(result, CompanyPluginDiagnosticCode.REQUIRED_DEPENDENCY_NOT_EFFECTIVE);
    }

    @Test
    void ineffectiveRequiredDependencyOfCustomizationQuarantinesTheCompany() {
        PluginDescriptor inventory = functional("inventory", List.of());
        PluginDescriptor customA = customization(
                "custom_a", List.of(requiredDependency("inventory")));
        Company company = company(COMPANY_A, CompanyStatus.ACTIVE, CUSTOM_A);

        CompanyPluginResolution result = resolver.resolve(
                company, false, List.of(), List.of(customA, inventory));

        assertFalse(result.operational());
        assertTrue(result.orderedPlugins().isEmpty());
        assertCodes(result, CompanyPluginDiagnosticCode.CUSTOMIZATION_INCOMPATIBLE);
    }

    @Test
    void staleDecisionsAreDiagnosedWithoutAffectingOtherCompanies() {
        PluginDescriptor customA = customization("custom_a", List.of());
        Company company = company(COMPANY_A, CompanyStatus.ACTIVE, CUSTOM_A);

        CompanyPluginResolution result = resolver.resolve(
                company,
                false,
                List.of(
                        enabled(COMPANY_A, "missing_plugin"),
                        enabled(COMPANY_A, "custom_a"),
                        enabled(COMPANY_B, "another_missing_plugin")),
                List.of(customA));

        assertTrue(result.operational());
        assertEquals(List.of("custom_a"), ids(result));
        assertCodes(
                result,
                CompanyPluginDiagnosticCode.PLUGIN_NOT_PRESENT,
                CompanyPluginDiagnosticCode.PLUGIN_NOT_FUNCTIONAL);
    }

    @Test
    void resultIsDeterministicForAnyInputOrderAndDuplicateDecisionsAreRejected() {
        PluginDescriptor first = functional("first", List.of());
        PluginDescriptor second = functional("second", List.of());
        PluginDescriptor customA = customization("custom_a", List.of());
        Company company = company(COMPANY_A, CompanyStatus.ACTIVE, CUSTOM_A);
        List<PluginActivationDecision> decisions =
                List.of(enabled(COMPANY_A, "second"), enabled(COMPANY_A, "first"));

        CompanyPluginResolution firstResult = resolver.resolve(
                company, false, decisions, List.of(second, customA, first));
        CompanyPluginResolution secondResult = resolver.resolve(
                company, false, decisions.reversed(), List.of(first, second, customA));

        assertEquals(ids(firstResult), ids(secondResult));
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(
                        company,
                        false,
                        List.of(enabled(COMPANY_A, "first"), enabled(COMPANY_A, "first")),
                        List.of(first, customA)));
    }

    private static Company company(CompanyId id, CompanyStatus status, PluginId customizationId) {
        return new Company(id, status, customizationId, 0);
    }

    private static PluginActivationDecision enabled(CompanyId companyId, String pluginId) {
        return new PluginActivationDecision(
                companyId, new PluginId(pluginId), PluginActivationState.ENABLED, 0);
    }

    private static PluginDescriptor functional(String id, List<PluginDependency> dependencies) {
        return descriptor(id, PluginKind.FUNCTIONAL, dependencies);
    }

    private static PluginDescriptor customization(String id, List<PluginDependency> dependencies) {
        return descriptor(id, PluginKind.CUSTOMIZATION, dependencies);
    }

    private static PluginDescriptor descriptor(
            String id, PluginKind kind, List<PluginDependency> dependencies) {
        return new PluginDescriptor(
                new PluginId(id),
                kind,
                SemanticVersion.parse("1.0.0"),
                API_RANGE,
                id,
                dependencies,
                List.of(), List.of(), List.of(), List.of());
    }

    private static PluginDependency requiredDependency(String id) {
        return new PluginDependency(new PluginId(id), VERSION_ONE, DependencyKind.REQUIRED);
    }

    private static CompanyId companyId(long suffix) {
        return new CompanyId(new UUID(0, suffix));
    }

    private static VersionRange range(String minimum, String maximum) {
        return new VersionRange(SemanticVersion.parse(minimum), SemanticVersion.parse(maximum));
    }

    private static List<String> ids(CompanyPluginResolution resolution) {
        return resolution.orderedPlugins().stream()
                .map(descriptor -> descriptor.id().value())
                .toList();
    }

    private static void assertCodes(
            CompanyPluginResolution resolution,
            CompanyPluginDiagnosticCode... expected) {
        assertEquals(
                List.of(expected),
                resolution.diagnostics().stream()
                        .map(CompanyPluginDiagnostic::code)
                        .toList());
    }
}
