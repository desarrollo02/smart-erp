package py.com.logixone.plugins.businesspartners;

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
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerContractVersion;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerPermissions;

class BusinessPartnersPluginDefinitionTest {

    @Test
    void exposesACompatibleFunctionalDescriptorWithItsPrivateMigration() {
        PluginDescriptor descriptor = new BusinessPartnersPluginDefinition().descriptor();

        assertEquals("business_partners", descriptor.id().value());
        assertEquals("1.1.0", descriptor.version().toString());
        assertEquals(BusinessPartnerContractVersion.CURRENT, descriptor.version().toString());
        assertEquals(PluginKind.FUNCTIONAL, descriptor.kind());
        assertTrue(descriptor.pluginApiCompatibility().contains(PluginApiVersion.CURRENT));
        assertEquals(1, descriptor.dependencies().size());
        assertEquals("reference_data", descriptor.dependencies().getFirst().pluginId().value());
        assertEquals(DependencyKind.REQUIRED, descriptor.dependencies().getFirst().kind());
        assertEquals(2, descriptor.capabilities().size());
        assertTrue(descriptor.capabilities().stream()
                .anyMatch(value -> value.value().equals("business_partners.directory")));
        assertEquals(BusinessPartnerPermissions.all(), descriptor.permissions());
        assertEquals(2, descriptor.menuContributions().size());
        assertEquals("/business-partners", descriptor.menuContributions().getFirst().route());
        assertEquals(BusinessPartnerPermissions.VIEW,
                descriptor.menuContributions().getFirst().requiredPermission().orElseThrow());
        assertEquals(1, descriptor.migrations().size());
        assertEquals("plg_business_partners", descriptor.migrations().getFirst().schema());
        assertEquals(
                "classpath:db/migration/business_partners",
                descriptor.migrations().getFirst().location());
        assertEquals(List.of(
                BusinessPartnersScreenContract.definition(),
                BusinessPartnersScreenContract.definitions()), descriptor.screenDefinitions());
        assertEquals(2, descriptor.screenDefinitions().getFirst().slots().size());
        assertTrue(descriptor.screenDefinitions().getFirst().elements().stream()
                .anyMatch(element -> element.id().equals(BusinessPartnersScreenContract.RESULTS)));
        assertEquals(BusinessPartnersScreenContract.DEFINITIONS_ROUTE,
                descriptor.menuContributions().get(1).route());
        assertEquals(BusinessPartnerPermissions.MANAGE,
                descriptor.menuContributions().get(1).requiredPermission().orElseThrow());
        assertTrue(descriptor.screenOverlays().isEmpty());
    }

    @Test
    void isAnApplicationScopedCdiBean() {
        assertTrue(BusinessPartnersPluginDefinition.class.isAnnotationPresent(ApplicationScoped.class));
        assertFalse(Modifier.isFinal(BusinessPartnersPluginDefinition.class.getModifiers()));
    }

    @Test
    void exposesTheSameDefinitionThroughTheJavaServiceProvider() {
        List<Class<? extends PluginDefinition>> providers =
                ServiceLoader.load(PluginDefinition.class).stream()
                .map(ServiceLoader.Provider::type)
                .toList();

        assertEquals(List.of(BusinessPartnersPluginDefinition.class), providers);
    }
}
