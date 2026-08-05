package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.application.company.screen.ComposedScreen;
import py.com.logixone.kernel.application.company.screen.ComposedScreenElement;
import py.com.logixone.plugins.businesspartners.BusinessPartnersPluginDefinition;
import py.com.logixone.plugins.businesspartners.BusinessPartnersScreenContract;

class BusinessPartnerScreenRendererTest {

    @Test
    void rendersThePublicContractThroughAClosedShellAdapter() {
        var definition = BusinessPartnersScreenContract.definition();
        ComposedScreen composed = new ComposedScreen(
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

        ShellScreenRegistry registry = new ShellScreenRegistry();
        assertEquals(BusinessPartnersScreenContract.DIRECTORY,
                registry.screenFor(
                        BusinessPartnersPluginDefinition.ID,
                        BusinessPartnersScreenContract.ROUTE).orElseThrow());
        ShellScreenView view = registry.render(composed, new ShellTextCatalog()).orElseThrow();

        assertTrue(view.isInteractive());
        assertEquals("Socios comerciales", view.getTitle());
        assertEquals(2, view.getDirectorySections().size());
        assertEquals(8, view.getDetailSections().size());
        assertEquals(5, view.getDetailTabs().size());
        assertTrue(view.isHasTableElement());
        assertTrue(view.isHasRowAction());
        assertTrue(view.acceptsAction(BusinessPartnersScreenContract.REGISTER.value()));
    }

    @Test
    void rendersTheCompanyOwnedBusinessPartnerDefinitionsDirectory() {
        var definition = BusinessPartnersScreenContract.definitions();
        ComposedScreen composed = new ComposedScreen(
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

        ShellScreenRegistry registry = new ShellScreenRegistry();
        assertEquals(BusinessPartnersScreenContract.DEFINITIONS,
                registry.screenFor(
                        BusinessPartnersPluginDefinition.ID,
                        BusinessPartnersScreenContract.DEFINITIONS_ROUTE).orElseThrow());
        ShellScreenView view = registry.render(composed, new ShellTextCatalog()).orElseThrow();

        assertTrue(view.isInteractive());
        assertEquals("Definiciones de socios", view.getTitle());
        assertEquals(2, view.getDirectorySections().size());
        assertTrue(view.isHasTableElement());
        assertEquals("Definiciones disponibles", view.getTableElement().getLabel());
        assertEquals("Clase de definición",
                view.getDirectorySections().getFirst().getFields().getFirst().getLabel());
        assertEquals("Clase de definición",
                view.getDirectorySections().get(1).getFields().getFirst().getLabel());
        assertTrue(view.isHasRowAction());
        assertTrue(view.acceptsAction(
                BusinessPartnersScreenContract.REGISTER_DEFINITION.value()));
        assertTrue(view.acceptsAction(
                BusinessPartnersScreenContract.REVISE_DEFINITION.value()));
        assertTrue(view.acceptsAction(
                BusinessPartnersScreenContract.ACTIVATE_DEFINITION.value()));
        assertTrue(view.acceptsAction(
                BusinessPartnersScreenContract.INACTIVATE_DEFINITION.value()));
        assertEquals(List.of("history", "revision", "lifecycle"),
                view.getDetailTabs().stream().map(ShellDetailTabView::getId).toList());
    }

    @Test
    void rejectsUnknownContractsAndKeepsXhtmlAndResponsiveCssParseable() throws Exception {
        var definition = BusinessPartnersScreenContract.definition();
        ComposedScreen unknownSlots = new ComposedScreen(
                definition.id(),
                definition.contractVersion(),
                List.of(),
                List.of(),
                List.of());
        assertTrue(new ShellScreenRegistry()
                .render(unknownSlots, new ShellTextCatalog()).isEmpty());

        try (InputStream input = resource("META-INF/resources/app/view.xhtml")) {
            String xhtml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(xhtml.contains("styleClass=\"directory-view\""));
            assertTrue(xhtml.contains("styleClass=\"create-view\""));
            assertTrue(xhtml.contains("class=\"detail-tabs\""));
            assertTrue(xhtml.contains("value=\"#{shellView.activeScreen.detailTabs}\""));
            assertTrue(xhtml.contains("rendered=\"#{section.tabId eq shellView.requestedTab}\""));
            assertFalse(xhtml.contains("shellView.rolesTab"));
            assertTrue(xhtml.contains("styleClass=\"screen-section-form compact-business-form detail-tab-form\""));
            assertTrue(xhtml.contains("styleClass=\"screen-field detail-display-summary\""));
            assertTrue(xhtml.contains("styleClass=\"detail-display-summary-value\""));
            assertTrue(xhtml.contains("<h:inputHidden value=\"#{shellView.inputValues[field.id]}\"/>"));
            assertTrue(xhtml.contains("pt:aria-required=\"#{field.required}\""));
            assertTrue(xhtml.contains("styleClass=\"app-navigation\""));
            assertTrue(xhtml.contains("<f:param name=\"mode\" value=\"detail\"/>"));
            assertTrue(xhtml.contains("xmlns:pt=\"jakarta.faces.passthrough\""));
            assertTrue(xhtml.contains("pt:aria-label=\"Abrir #{row.cells[1]}\""));
            assertFalse(xhtml.contains("Slot público: directory_extensions"));
            assertFalse(xhtml.contains("Slot público: detail_extensions"));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            assertEquals("html", factory.newDocumentBuilder().parse(
                            new java.io.ByteArrayInputStream(xhtml.getBytes(StandardCharsets.UTF_8)))
                    .getDocumentElement().getLocalName());
        }

        String css;
        try (InputStream input = resource("META-INF/resources/resources/logixone/shell.css")) {
            css = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(css.contains("@media (max-width: 599px)"));
        assertTrue(css.contains("@media (min-width: 600px) and (max-width: 839px)"));
        assertTrue(css.contains(".responsive-table-shell"));
        assertTrue(css.contains("overflow-x: auto"));
        assertTrue(css.contains(".composed-screen-business"));
        assertTrue(css.contains(".business-mobile-list"));
        assertTrue(css.contains("overflow-wrap: anywhere"));
        assertTrue(css.contains(".screen-notice { min-width: 0"));
        assertTrue(css.contains(".app-navigation"));
        assertTrue(css.contains(".session-panel > form:not(.company-switcher)"));
        assertTrue(css.contains("grid-template-columns: minmax(0, 1fr) auto"));
        assertTrue(css.contains(".welcome-panel > * { position: relative; z-index: 1; min-width: 0; max-width: 100%; }"));
        assertFalse(css.contains("http://"));
        assertFalse(css.contains("https://"));
    }

    private static InputStream resource(String name) {
        InputStream input = BusinessPartnerScreenRendererTest.class
                .getClassLoader().getResourceAsStream(name);
        assertNotNull(input, name);
        return input;
    }
}
