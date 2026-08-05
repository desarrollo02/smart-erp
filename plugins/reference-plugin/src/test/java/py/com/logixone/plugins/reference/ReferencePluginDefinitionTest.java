package py.com.logixone.plugins.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.PluginApiVersion;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginKind;

class ReferencePluginDefinitionTest {

    @Test
    void exposesACompatibleDescriptorWithMinimalContributions() {
        PluginDescriptor descriptor = new ReferencePluginDefinition().descriptor();

        assertEquals("reference_plugin", descriptor.id().value());
        assertEquals("1.0.0", descriptor.version().toString());
        assertEquals(PluginKind.FUNCTIONAL, descriptor.kind());
        assertTrue(descriptor.pluginApiCompatibility().contains(PluginApiVersion.CURRENT));
        assertEquals(1, descriptor.capabilities().size());
        assertEquals(1, descriptor.permissions().size());
        assertEquals(1, descriptor.menuContributions().size());
        assertEquals(1, descriptor.screenDefinitions().size());
        assertEquals(0, descriptor.screenOverlays().size());
        assertEquals(1, descriptor.migrations().size());
        assertEquals("plg_reference_plugin", descriptor.migrations().getFirst().schema());
        assertEquals(
                "classpath:db/migration/reference_plugin",
                descriptor.migrations().getFirst().location());
    }

    @Test
    void isAnApplicationScopedCdiBean() {
        assertTrue(ReferencePluginDefinition.class.isAnnotationPresent(ApplicationScoped.class));
    }
}
