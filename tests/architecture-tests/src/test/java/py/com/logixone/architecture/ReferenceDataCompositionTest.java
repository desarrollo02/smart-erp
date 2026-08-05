package py.com.logixone.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.application.plugin.InvalidPluginCatalogException;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode;
import py.com.logixone.plugins.businesspartners.BusinessPartnersPluginDefinition;
import py.com.logixone.plugins.commercialcatalog.CommercialCatalogPluginDefinition;
import py.com.logixone.plugins.referencedata.ReferenceDataPluginDefinition;

class ReferenceDataCompositionTest {

    @Test
    void ordersReferenceDataBeforeBothNormativeConsumers() {
        PluginRegistry registry = PluginRegistry.create(List.of(
                new CommercialCatalogPluginDefinition(),
                new BusinessPartnersPluginDefinition(),
                new ReferenceDataPluginDefinition()));

        assertEquals(
                List.of("reference_data", "business_partners", "commercial_catalog"),
                registry.orderedPlugins().stream()
                        .map(plugin -> plugin.id().value())
                        .toList());
    }

    @Test
    void rejectsBothConsumersWhenThePhysicalProviderIsMissing() {
        InvalidPluginCatalogException failure = assertThrows(
                InvalidPluginCatalogException.class,
                () -> PluginRegistry.create(List.of(
                        new BusinessPartnersPluginDefinition(),
                        new CommercialCatalogPluginDefinition())));

        assertEquals(
                List.of(
                        PluginDiagnosticCode.MISSING_REQUIRED_DEPENDENCY,
                        PluginDiagnosticCode.MISSING_REQUIRED_DEPENDENCY),
                failure.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());
    }
}
