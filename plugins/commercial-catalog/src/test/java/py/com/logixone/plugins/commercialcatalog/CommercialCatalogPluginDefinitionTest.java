package py.com.logixone.plugins.commercialcatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.PluginApiVersion;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugins.commercialcatalog.api.CatalogContractVersion;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogPermissions;

class CommercialCatalogPluginDefinitionTest {

    @Test
    void exposesACompatibleFunctionalDescriptorWithItsScreensAndPrivateMigration() {
        PluginDescriptor descriptor = new CommercialCatalogPluginDefinition().descriptor();

        assertEquals("commercial_catalog", descriptor.id().value());
        assertEquals(CatalogContractVersion.CURRENT, descriptor.version().toString());
        assertEquals(PluginKind.FUNCTIONAL, descriptor.kind());
        assertTrue(descriptor.pluginApiCompatibility().contains(PluginApiVersion.CURRENT));
        assertEquals(1, descriptor.dependencies().size());
        assertEquals("reference_data", descriptor.dependencies().getFirst().pluginId().value());
        assertEquals(DependencyKind.REQUIRED, descriptor.dependencies().getFirst().kind());
        assertEquals(5, descriptor.capabilities().size());
        assertEquals(CommercialCatalogPermissions.all(), descriptor.permissions());
        assertEquals(5, descriptor.menuContributions().size());
        assertEquals(List.of(
                        CommercialCatalogScreenContract.ITEMS_ROUTE,
                        CommercialCatalogScreenContract.PRICE_LISTS_ROUTE,
                        CommercialCatalogScreenContract.DEFINITIONS_ROUTE,
                        CommercialCatalogScreenContract.VARIANT_FAMILIES_ROUTE,
                        CommercialCatalogScreenContract.TAX_PROFILES_ROUTE),
                descriptor.menuContributions().stream().map(menu -> menu.route()).toList());
        assertEquals(List.of(
                        CommercialCatalogPermissions.VIEW,
                        CommercialCatalogPermissions.VIEW,
                        CommercialCatalogPermissions.DEFINITIONS_MANAGE,
                        CommercialCatalogPermissions.DEFINITIONS_MANAGE,
                        CommercialCatalogPermissions.DEFINITIONS_MANAGE),
                descriptor.menuContributions().stream()
                        .map(menu -> menu.requiredPermission().orElseThrow()).toList());
        assertEquals(1, descriptor.migrations().size());
        assertEquals("plg_commercial_catalog", descriptor.migrations().getFirst().schema());
        assertEquals("classpath:db/migration/commercial_catalog",
                descriptor.migrations().getFirst().location());
        assertEquals(List.of(
                        CommercialCatalogScreenContract.itemsDefinition(),
                        CommercialCatalogScreenContract.priceListsDefinition(),
                        CommercialCatalogScreenContract.definitionsDefinition(),
                        CommercialCatalogScreenContract.variantFamiliesDefinition(),
                        CommercialCatalogScreenContract.taxProfilesDefinition()),
                descriptor.screenDefinitions());
        assertTrue(descriptor.screenDefinitions().stream().allMatch(screen ->
                screen.slots().size() == 2));
        assertTrue(descriptor.screenOverlays().isEmpty());
    }

    @Test
    void isDiscoverableThroughCdiAndJavaServiceProvider() {
        assertTrue(CommercialCatalogPluginDefinition.class.isAnnotationPresent(ApplicationScoped.class));
        assertFalse(Modifier.isFinal(CommercialCatalogPluginDefinition.class.getModifiers()));

        List<Class<? extends PluginDefinition>> providers =
                ServiceLoader.load(PluginDefinition.class).stream()
                        .map(ServiceLoader.Provider::type)
                        .toList();

        assertEquals(List.of(CommercialCatalogPluginDefinition.class), providers);
    }
}
