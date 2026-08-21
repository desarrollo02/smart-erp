package py.com.logixone.plugins.sales;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.*;

class SalesPluginDefinitionTest {
    @Test void exposesAuthorizedApplicationWithoutUi(){var d=new SalesPluginDefinition().descriptor(); assertEquals("sales",d.id().value()); assertEquals(List.of("business_partners","commercial_catalog","reference_data","inventory"),d.dependencies().stream().map(x->x.pluginId().value()).toList()); assertEquals(4,d.capabilities().size()); assertEquals(11,d.permissions().size()); assertTrue(d.menuContributions().isEmpty()); assertEquals("plg_sales",d.migrations().getFirst().schema()); assertTrue(d.screenDefinitions().isEmpty());}
    @Test void isServiceProvider(){assertTrue(ServiceLoader.load(PluginDefinition.class).stream().map(ServiceLoader.Provider::type).anyMatch(SalesPluginDefinition.class::equals));}
}
