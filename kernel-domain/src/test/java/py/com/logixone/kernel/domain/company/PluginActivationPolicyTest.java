package py.com.logixone.kernel.domain.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class PluginActivationPolicyTest {

    private static final CompanyId COMPANY_ID = new CompanyId(new UUID(0, 1));
    private static final PluginId CUSTOMIZATION_ID = new PluginId("custom_a");
    private static final VersionRange API_RANGE = range("0.4.0", "0.5.0");
    private static final VersionRange VERSION_ONE = range("1.0.0", "2.0.0");
    private final PluginActivationPolicy policy = new PluginActivationPolicy();
    private final Company company = new Company(
            COMPANY_ID, CompanyStatus.INACTIVE, CUSTOMIZATION_ID, 0);

    @Test
    void enablingRequiresDesiredRequiredDependenciesButNotOptionalOnes() {
        PluginDescriptor inventory = functional("inventory", List.of());
        PluginDescriptor sales = functional(
                "sales",
                List.of(
                        dependency("inventory", DependencyKind.REQUIRED),
                        dependency("analytics", DependencyKind.OPTIONAL)));

        PluginActivationChangeResult rejected = policy.evaluate(
                company,
                sales.id(),
                PluginActivationState.ENABLED,
                List.of(),
                List.of(sales, inventory));
        PluginActivationChangeResult allowed = policy.evaluate(
                company,
                sales.id(),
                PluginActivationState.ENABLED,
                List.of(enabled("inventory")),
                List.of(inventory, sales));

        assertFalse(rejected.allowed());
        assertCodes(rejected, CompanyPluginDiagnosticCode.REQUIRED_DEPENDENCY_NOT_EFFECTIVE);
        assertTrue(allowed.allowed());
    }

    @Test
    void disablingIsBlockedByDesiredFunctionalDependent() {
        PluginDescriptor inventory = functional("inventory", List.of());
        PluginDescriptor sales = functional(
                "sales", List.of(dependency("inventory", DependencyKind.REQUIRED)));

        PluginActivationChangeResult result = policy.evaluate(
                company,
                inventory.id(),
                PluginActivationState.DISABLED,
                List.of(enabled("inventory"), enabled("sales")),
                List.of(sales, inventory));

        assertFalse(result.allowed());
        assertCodes(result, CompanyPluginDiagnosticCode.ACTIVE_DEPENDENT_EXISTS);
        assertEquals("sales", result.diagnostics().getFirst().subject());
    }

    @Test
    void disablingIsBlockedByTheAssignedCustomizationRequiredDependency() {
        PluginDescriptor inventory = functional("inventory", List.of());
        PluginDescriptor customization = descriptor(
                "custom_a",
                PluginKind.CUSTOMIZATION,
                List.of(dependency("inventory", DependencyKind.REQUIRED)));

        PluginActivationChangeResult result = policy.evaluate(
                company,
                inventory.id(),
                PluginActivationState.DISABLED,
                List.of(enabled("inventory")),
                List.of(customization, inventory));

        assertFalse(result.allowed());
        assertCodes(result, CompanyPluginDiagnosticCode.ACTIVE_DEPENDENT_EXISTS);
        assertEquals("custom_a", result.diagnostics().getFirst().subject());
    }

    @Test
    void commonActivationCannotTargetCustomizationOrMissingPlugin() {
        PluginDescriptor customization = descriptor(
                "custom_a", PluginKind.CUSTOMIZATION, List.of());

        PluginActivationChangeResult customizationResult = policy.evaluate(
                company,
                customization.id(),
                PluginActivationState.ENABLED,
                List.of(),
                List.of(customization));
        PluginActivationChangeResult missingResult = policy.evaluate(
                company,
                new PluginId("missing"),
                PluginActivationState.ENABLED,
                List.of(),
                List.of(customization));

        assertCodes(customizationResult, CompanyPluginDiagnosticCode.PLUGIN_NOT_FUNCTIONAL);
        assertCodes(missingResult, CompanyPluginDiagnosticCode.PLUGIN_NOT_PRESENT);
    }

    private static PluginActivationDecision enabled(String pluginId) {
        return new PluginActivationDecision(
                COMPANY_ID,
                new PluginId(pluginId),
                PluginActivationState.ENABLED,
                0);
    }

    private static PluginDescriptor functional(String id, List<PluginDependency> dependencies) {
        return descriptor(id, PluginKind.FUNCTIONAL, dependencies);
    }

    private static PluginDescriptor descriptor(
            String id,
            PluginKind kind,
            List<PluginDependency> dependencies) {
        return new PluginDescriptor(
                new PluginId(id),
                kind,
                SemanticVersion.parse("1.0.0"),
                API_RANGE,
                id,
                dependencies,
                List.of(), List.of(), List.of(), List.of());
    }

    private static PluginDependency dependency(String id, DependencyKind kind) {
        return new PluginDependency(new PluginId(id), VERSION_ONE, kind);
    }

    private static VersionRange range(String minimum, String maximum) {
        return new VersionRange(SemanticVersion.parse(minimum), SemanticVersion.parse(maximum));
    }

    private static void assertCodes(
            PluginActivationChangeResult result,
            CompanyPluginDiagnosticCode... expected) {
        assertEquals(
                List.of(expected),
                result.diagnostics().stream()
                        .map(CompanyPluginDiagnostic::code)
                        .toList());
    }
}
