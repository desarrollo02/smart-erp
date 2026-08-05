package py.com.logixone.plugin.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PluginIdTest {

    @Test
    void acceptsSnakeCaseAndDerivesSchemaWithoutNormalization() {
        PluginId id = new PluginId("reference_plugin");

        assertEquals("reference_plugin", id.value());
        assertEquals("plg_reference_plugin", id.schemaName());
    }

    @Test
    void rejectsAmbiguousOrDatabaseUnsafeValues() {
        assertThrows(IllegalArgumentException.class, () -> new PluginId("ReferencePlugin"));
        assertThrows(IllegalArgumentException.class, () -> new PluginId("reference-plugin"));
        assertThrows(IllegalArgumentException.class, () -> new PluginId("reference__plugin"));
        assertThrows(IllegalArgumentException.class, () -> new PluginId("a".repeat(60)));
    }
}
