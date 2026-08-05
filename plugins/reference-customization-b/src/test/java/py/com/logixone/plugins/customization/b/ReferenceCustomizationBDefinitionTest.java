package py.com.logixone.plugins.customization.b;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.PluginKind;

class ReferenceCustomizationBDefinitionTest {

    @Test
    void publishesOneTypedOverlayAndNoFunctionalScreens() {
        var descriptor = new ReferenceCustomizationBDefinition().descriptor();

        assertEquals(PluginKind.CUSTOMIZATION, descriptor.kind());
        assertEquals(1, descriptor.dependencies().size());
        assertEquals(1, descriptor.screenOverlays().size());
        assertEquals(0, descriptor.screenDefinitions().size());
    }
}
