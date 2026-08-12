package py.com.logixone.plugins.purchasing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.PluginApiVersion;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugins.purchasing.api.PurchasingContractVersion;
import py.com.logixone.plugins.purchasing.application.PurchasingPermissions;

class PurchasingPluginDefinitionTest {
    @Test
    void exposesOnlyPublicCapabilitiesAndFourRequiredDependencies() {
        PluginDescriptor descriptor = new PurchasingPluginDefinition().descriptor();

        assertEquals("purchasing", descriptor.id().value());
        assertEquals(PurchasingContractVersion.CURRENT, descriptor.version().toString());
        assertEquals(PluginKind.FUNCTIONAL, descriptor.kind());
        assertTrue(descriptor.pluginApiCompatibility().contains(PluginApiVersion.CURRENT));
        assertEquals(List.of(
                        "business_partners", "commercial_catalog", "reference_data", "inventory"),
                descriptor.dependencies().stream()
                        .map(dependency -> dependency.pluginId().value()).toList());
        assertTrue(descriptor.dependencies().stream().allMatch(dependency ->
                dependency.kind() == DependencyKind.REQUIRED));
        assertMinimumVersion(descriptor, "business_partners", "1.1.0");
        assertMinimumVersion(descriptor, "inventory", "1.1.0");
        assertMinimumVersion(descriptor, "commercial_catalog", "1.1.0");
        assertMinimumVersion(descriptor, "reference_data", "1.0.0");
        assertEquals(5, descriptor.capabilities().size());
        assertEquals(PurchasingPermissions.all(), descriptor.permissions());
        assertEquals(List.of(
                        "/purchasing/requests", "/purchasing/orders",
                        "/purchasing/receipts", "/purchasing/returns",
                        "/purchasing/tracking"),
                descriptor.menuContributions().stream().map(menu -> menu.route()).toList());
        assertEquals(1, descriptor.migrations().size());
        assertEquals("plg_purchasing", descriptor.migrations().getFirst().schema());
        assertEquals("classpath:db/migration/purchasing",
                descriptor.migrations().getFirst().location());
        assertEquals(5, descriptor.screenDefinitions().size());
        assertTrue(descriptor.screenOverlays().isEmpty());
    }

    @Test
    void isDiscoverableThroughCdiAndJavaServiceProvider() {
        assertTrue(PurchasingPluginDefinition.class.isAnnotationPresent(ApplicationScoped.class));
        assertFalse(Modifier.isFinal(PurchasingPluginDefinition.class.getModifiers()));

        List<Class<? extends PluginDefinition>> providers = ServiceLoader.load(PluginDefinition.class).stream()
                .map(ServiceLoader.Provider::type)
                .filter(type -> type.equals(PurchasingPluginDefinition.class))
                .toList();

        assertEquals(List.of(PurchasingPluginDefinition.class), providers);
    }

    private static void assertMinimumVersion(
            PluginDescriptor descriptor, String pluginId, String minimum) {
        var dependency = descriptor.dependencies().stream()
                .filter(candidate -> candidate.pluginId().value().equals(pluginId))
                .findFirst().orElseThrow();
        var minimumVersion = py.com.logixone.plugin.api.SemanticVersion.parse(minimum);
        assertTrue(dependency.compatibleVersions().contains(minimumVersion));
        if (!"1.0.0".equals(minimum)) {
            assertFalse(dependency.compatibleVersions().contains(
                    py.com.logixone.plugin.api.SemanticVersion.parse("1.0.0")));
        }
    }
}
