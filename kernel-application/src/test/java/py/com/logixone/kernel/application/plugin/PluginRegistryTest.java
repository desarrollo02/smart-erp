package py.com.logixone.kernel.application.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

class PluginRegistryTest {

    private static final VersionRange API_RANGE = range("0.4.0", "0.5.0");
    private static final VersionRange VERSION_ONE = range("1.0.0", "2.0.0");

    @Test
    void emptyDistributionProducesAValidEmptyRegistry() {
        PluginRegistry registry = PluginRegistry.create(List.of());

        assertEquals(0, registry.size());
        assertTrue(registry.orderedPlugins().isEmpty());
        assertFalse(registry.contains(new PluginId("reference_plugin")));
    }

    @Test
    void exposesValidatedPluginsInDependencyOrderAndByIdentity() {
        PluginDescriptor base = descriptor("base_plugin", "1.1.0", List.of());
        PluginDescriptor consumer = descriptor(
                "consumer_plugin",
                "1.0.0",
                List.of(new PluginDependency(
                        base.id(), VERSION_ONE, DependencyKind.REQUIRED)));
        PluginRegistry registry = PluginRegistry.create(List.of(definition(consumer), definition(base)));

        assertEquals(List.of("base_plugin", "consumer_plugin"), registry.orderedPlugins().stream()
                .map(plugin -> plugin.id().value())
                .toList());
        assertEquals(base, registry.find(base.id()).orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> registry.orderedPlugins().clear());
    }

    @Test
    void invalidCatalogStopsRegistrationAndKeepsTypedDiagnostics() {
        PluginDescriptor first = descriptor("duplicate_plugin", "1.0.0", List.of());
        PluginDescriptor second = descriptor("duplicate_plugin", "1.1.0", List.of());

        InvalidPluginCatalogException failure = assertThrows(
                InvalidPluginCatalogException.class,
                () -> PluginRegistry.create(List.of(definition(first), definition(second))));

        assertEquals(1, failure.diagnostics().size());
        assertEquals(PluginDiagnosticCode.DUPLICATE_PLUGIN_ID, failure.diagnostics().getFirst().code());
        assertTrue(failure.getMessage().contains("DUPLICATE_PLUGIN_ID"));
    }

    @Test
    void nullDescriptorStopsRegistrationAtTheBoundary() {
        assertThrows(IllegalArgumentException.class, () -> PluginRegistry.create(List.of(() -> null)));
    }

    private static PluginDefinition definition(PluginDescriptor descriptor) {
        return () -> descriptor;
    }

    private static PluginDescriptor descriptor(
            String id, String version, List<PluginDependency> dependencies) {
        return new PluginDescriptor(
                new PluginId(id),
                PluginKind.FUNCTIONAL,
                SemanticVersion.parse(version),
                API_RANGE,
                id,
                dependencies,
                List.of(), List.of(), List.of(), List.of());
    }

    private static VersionRange range(String minimum, String maximum) {
        return new VersionRange(SemanticVersion.parse(minimum), SemanticVersion.parse(maximum));
    }
}
