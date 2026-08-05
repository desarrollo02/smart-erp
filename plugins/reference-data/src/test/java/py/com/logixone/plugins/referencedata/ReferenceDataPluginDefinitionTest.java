package py.com.logixone.plugins.referencedata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.PluginApiVersion;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugins.referencedata.api.ReferenceDataContractVersion;
import py.com.logixone.plugins.referencedata.application.ReferenceDataPermissions;

class ReferenceDataPluginDefinitionTest {

    @Test
    void exposesFunctionalProviderProvenanceAndPrivateMigration() {
        PluginDescriptor descriptor = new ReferenceDataPluginDefinition().descriptor();

        assertEquals("reference_data", descriptor.id().value());
        assertEquals(ReferenceDataContractVersion.CURRENT, descriptor.version().toString());
        assertEquals(PluginKind.FUNCTIONAL, descriptor.kind());
        assertTrue(descriptor.pluginApiCompatibility().contains(PluginApiVersion.CURRENT));
        assertTrue(descriptor.dependencies().isEmpty());
        assertEquals(ReferenceDataPermissions.all(), descriptor.permissions());
        assertEquals(2, descriptor.capabilities().size());
        assertEquals(1, descriptor.menuContributions().size());
        assertEquals(Optional.of(ReferenceDataPermissions.POLICY_MANAGE),
                descriptor.menuContributions().getFirst().requiredPermission());
        assertEquals(ReferenceDataScreenContract.ROUTE,
                descriptor.menuContributions().getFirst().route());
        assertEquals("plg_reference_data", descriptor.migrations().getFirst().schema());
        assertEquals("classpath:db/migration/reference_data",
                descriptor.migrations().getFirst().location());
        assertEquals(List.of(ReferenceDataScreenContract.definition()),
                descriptor.screenDefinitions());
    }

    @Test
    void exposesTheSameDefinitionThroughJavaServiceProvider() {
        List<Class<? extends PluginDefinition>> providers =
                ServiceLoader.load(PluginDefinition.class).stream()
                        .map(ServiceLoader.Provider::type)
                        .toList();

        assertEquals(List.of(ReferenceDataPluginDefinition.class), providers);
    }
}
