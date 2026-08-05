package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.application.company.screen.ComposedScreen;
import py.com.logixone.kernel.application.company.screen.ComposedScreenElement;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugins.referencedata.ReferenceDataPluginDefinition;
import py.com.logixone.plugins.referencedata.ReferenceDataScreenContract;

class ReferenceDataScreenRendererTest {

    @Test
    void rendersTheNormativeDirectoryAndGovernedPolicyJourney() {
        ShellScreenRegistry registry = new ShellScreenRegistry();

        assertEquals(
                ReferenceDataScreenContract.CATALOGS,
                registry.screenFor(
                                ReferenceDataPluginDefinition.ID,
                                ReferenceDataScreenContract.ROUTE)
                        .orElseThrow());

        ShellScreenView view = registry.render(
                        composed(ReferenceDataScreenContract.definition()),
                        new ShellTextCatalog())
                .orElseThrow();

        assertEquals("Datos de referencia", view.getTitle());
        assertEquals("Países y monedas", view.getTableElement().getLabel());
        assertFalse(view.acceptsAction("register"));
        assertEquals("Abrir", view.getRowAction().getLabel());
        assertEquals(2, view.getDetailTabs().size());
        assertEquals(1, view.getDirectorySections().size());
        assertEquals("search", view.getDirectorySections().getFirst().getId());
        assertEquals(2, view.getDirectorySections().getFirst().getFields().size());
        assertEquals(2, view.getDetailSections().size());
        assertEquals("Historial empresarial", view.getDetailSections().getFirst().getTitle());
        assertEquals("Disponibilidad para nuevas operaciones",
                view.getDetailSections().getLast().getTitle());
    }

    private static ComposedScreen composed(ScreenDefinition definition) {
        return new ComposedScreen(
                definition.id(),
                definition.contractVersion(),
                definition.elements().stream()
                        .map(element -> new ComposedScreenElement(
                                element.id(),
                                element.type(),
                                element.regionId(),
                                element.order(),
                                element.labelKey(),
                                element.helpKey(),
                                element.visible(),
                                element.enabled(),
                                element.required()))
                        .toList(),
                definition.slots(),
                List.of());
    }
}
