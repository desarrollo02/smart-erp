package py.com.logixone.plugins.inventory;

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
import py.com.logixone.plugins.inventory.api.InventoryContractVersion;
import py.com.logixone.plugins.inventory.application.InventoryPermissions;

class InventoryPluginDefinitionTest {
    @Test
    void exposesIdentityRequiredCatalogDependencyScreensAndPrivateMigration() {
        PluginDescriptor descriptor = new InventoryPluginDefinition().descriptor();

        assertEquals("inventory", descriptor.id().value());
        assertEquals(InventoryContractVersion.CURRENT, descriptor.version().toString());
        assertEquals(3, descriptor.capabilities().size());
        assertEquals(InventoryPermissions.all(), descriptor.permissions());
        assertEquals(PluginKind.FUNCTIONAL, descriptor.kind());
        assertTrue(descriptor.pluginApiCompatibility().contains(PluginApiVersion.CURRENT));
        assertEquals(1, descriptor.dependencies().size());
        assertEquals("commercial_catalog", descriptor.dependencies().getFirst().pluginId().value());
        assertEquals(DependencyKind.REQUIRED, descriptor.dependencies().getFirst().kind());
        assertTrue(descriptor.dependencies().getFirst().compatibleVersions()
                .contains(py.com.logixone.plugin.api.SemanticVersion.parse("1.0.0")));
        assertEquals(3, descriptor.menuContributions().size());
        assertEquals(List.of(
                        InventoryScreenContract.STOCK_ROUTE,
                        InventoryScreenContract.WAREHOUSES_ROUTE,
                        InventoryScreenContract.COUNTS_ROUTE),
                descriptor.menuContributions().stream().map(menu -> menu.route()).toList());
        assertTrue(descriptor.menuContributions().stream()
                .allMatch(menu -> menu.requiredPermission().orElseThrow()
                        .equals(InventoryPermissions.VIEW)));
        assertEquals(1, descriptor.migrations().size());
        assertEquals("plg_inventory", descriptor.migrations().getFirst().schema());
        assertEquals("classpath:db/migration/inventory", descriptor.migrations().getFirst().location());
        assertEquals(List.of(
                        InventoryScreenContract.stockDefinition(),
                        InventoryScreenContract.warehousesDefinition(),
                        InventoryScreenContract.countsDefinition()),
                descriptor.screenDefinitions());
        assertEquals(2, descriptor.screenDefinitions().stream()
                .filter(screen -> screen.contractVersion().major().intValueExact() == 1)
                .filter(screen -> screen.slots().size() == 2)
                .count());
        assertEquals("2.0.0", InventoryScreenContract.stockDefinition()
                .contractVersion().toString());
        assertTrue(InventoryScreenContract.stockDefinition().slots().isEmpty());
        assertTrue(descriptor.screenOverlays().isEmpty());
    }

    @Test
    void isDiscoverableThroughCdiAndJavaServiceProvider() {
        assertTrue(InventoryPluginDefinition.class.isAnnotationPresent(ApplicationScoped.class));
        assertFalse(Modifier.isFinal(InventoryPluginDefinition.class.getModifiers()));

        List<Class<? extends PluginDefinition>> providers = ServiceLoader.load(PluginDefinition.class).stream()
                .map(ServiceLoader.Provider::type)
                .toList();

        assertEquals(List.of(InventoryPluginDefinition.class), providers);
    }
}
