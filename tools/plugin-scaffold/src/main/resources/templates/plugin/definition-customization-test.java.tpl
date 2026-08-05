package {{PACKAGE_NAME}};

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.PluginApiVersion;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginKind;

class {{DEFINITION_CLASS}}Test {

    @Test
    void exposesAnEmptyCompatibleCustomizationDescriptor() {
        PluginDescriptor descriptor = new {{DEFINITION_CLASS}}().descriptor();

        assertEquals("{{PLUGIN_ID}}", descriptor.id().value());
        assertEquals("{{PLUGIN_VERSION}}", descriptor.version().toString());
        assertEquals(PluginKind.CUSTOMIZATION, descriptor.kind());
        assertTrue(descriptor.pluginApiCompatibility().contains(PluginApiVersion.CURRENT));
        assertEquals(1, descriptor.dependencies().size());
        assertEquals("{{TARGET_PLUGIN_ID}}", descriptor.dependencies().getFirst().pluginId().value());
        assertEquals(DependencyKind.REQUIRED, descriptor.dependencies().getFirst().kind());
        assertTrue(descriptor.capabilities().isEmpty());
        assertTrue(descriptor.permissions().isEmpty());
        assertTrue(descriptor.menuContributions().isEmpty());
        assertTrue(descriptor.migrations().isEmpty());
        assertTrue(descriptor.screenDefinitions().isEmpty());
        assertTrue(descriptor.screenOverlays().isEmpty());
    }

    @Test
    void isAnApplicationScopedCdiBean() {
        assertTrue({{DEFINITION_CLASS}}.class.isAnnotationPresent(ApplicationScoped.class));
        assertFalse(Modifier.isFinal({{DEFINITION_CLASS}}.class.getModifiers()));
    }
}
