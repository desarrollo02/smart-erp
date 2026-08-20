package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

class ShellFloorplanResourceTest {

    @Test
    void rendersV2RegionsInsideOneShellOwnedFormAndKeepsLegacySeparate()
            throws Exception {
        String view = resource("app/view.xhtml");

        assertTrue(view.contains("rendered=\"#{shellView.activeScreen.floorplanV2}\""));
        assertTrue(view.contains("styleClass=\"floorplan-form\""));
        assertTrue(view.contains("onsubmit=\"return LogixoneFloorplan.transport(this);\""));
        assertTrue(view.contains("name=\"mode\" value=\"#{shellView.requestedMode}\""));
        assertTrue(view.contains("name=\"tab\" value=\"#{shellView.requestedTab}\""));
        assertTrue(view.contains(
                "name=\"resource\" value=\"#{shellView.selectedResourceId}\""));
        assertTrue(view.contains(
                "name=\"version\" value=\"#{shellView.selectedResourceVersion}\""));
        assertTrue(view.contains("value=\"#{shellView.activeScreen.floorplanRegions}\""));
        assertTrue(view.contains("shellView.activeInteraction.elementStates[element.id].visible"));
        assertTrue(view.contains("shellView.activeInteraction.elementStates[element.id].enabled"));
        assertTrue(view.contains("styleClass=\"#{element.actionClass}\""));
        assertTrue(view.contains(
                "not (shellView.activeScreen.hasFloorplanRowAction and element.navigateIntent)"));
        assertTrue(view.contains(
                "name=\"floorplanActionRequest\" value=\"false\""));
        assertTrue(view.contains(
                "name=\"floorplanRequestedAction\" value=\"\""));
        assertTrue(view.contains("shellView.floorplanActionRequest"));
        assertTrue(view.contains(
                "floorplanForm.elements['floorplanActionRequest'].value='true';"));
        assertTrue(view.contains(
                "floorplanForm.elements['floorplanRequestedAction'].value='#{element.id}';"));
        assertTrue(view.contains("pt:data-floorplan-command-bridge=\"true\""));
        assertTrue(view.contains("pt:data-floorplan-context-bridge=\"true\""));
        assertTrue(view.contains("action=\"#{shellView.refreshFloorplanContext}\""));
        assertTrue(view.contains("return LogixoneFloorplan.refresh(this);"));
        assertTrue(view.contains("floorplan.js?ln=logixone"));
        assertTrue(view.contains("immediate=\"true\""));
        assertTrue(view.contains("pt:formnovalidate=\"formnovalidate\""));
        assertTrue(view.contains(
                "shellView.activeScreen.hasFloorplanRowAction and not shellView.activeInteraction.hasDetail"));
        assertTrue(view.contains(
                "styleClass=\"responsive-table-shell business-table-view\""));
        assertTrue(view.contains(
                "styleClass=\"business-mobile-item business-mobile-item-static\""));
        assertTrue(view.contains(
                "not shellView.activeScreen.hasFloorplanRowAction or shellView.activeInteraction.hasDetail"));
        assertTrue(view.contains(
                "floorplanForm.querySelectorAll('[data-screen-input]')"));
        assertTrue(view.contains(
                "submitted.name='floorplanInput.'+control.getAttribute('data-screen-input');"));
        assertTrue(view.contains("floorplanForm.requestSubmit(bridge);"));
        assertTrue(view.contains("else{bridge.click();}"));
        assertTrue(view.contains("LogixoneFloorplan.markActionSubmission();"));
        assertTrue(view.contains(
                "<h1 id=\"composed-screen-title\" tabindex=\"-1\">"));
        assertTrue(view.contains("type=\"button\""));
        assertTrue(view.contains(
                "rendered=\"#{element.select and shellView.activeInteraction.selectorSources[element.id].searchOnDemand}\""));
        assertTrue(view.contains("name=\"selectorValue:#{element.id}\""));
        assertTrue(view.contains("name=\"selectorSearch:#{element.id}\""));
        assertTrue(view.contains("name=\"selectorOption:#{element.id}\""));
        assertTrue(view.contains("#{shellView.selectedOptionLabel(element.id)}"));
        assertTrue(view.contains("LogixoneSelectorReturn.capture(this);"));
        assertTrue(view.contains("not shellView.selectorInteractionRequested"));
        assertTrue(view.contains("<f:param name=\"mode\" value=\"detail\"/>"));
        assertTrue(view.contains("rendered=\"#{element.technicalToken}\""));
        assertTrue(view.contains("id=\"floorplan-token\""));
        assertTrue(view.contains(
                "shellView.activeScreen.interactive and not shellView.activeScreen.floorplanV2"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try (InputStream input = resourceStream("app/view.xhtml")) {
            factory.newDocumentBuilder().parse(input);
        }

        String script = resource("resources/logixone/floorplan.js");
        assertTrue(script.contains("[data-floorplan-context-bridge]"));
        assertTrue(script.contains("floorplanInput."));
        assertTrue(script.contains("form.requestSubmit(bridge)"));
        assertTrue(script.contains("logixone.floorplan.action-focus"));
        assertTrue(script.contains("logixone.floorplan.control-focus"));
        assertTrue(script.contains("[aria-invalid='true']"));
        assertTrue(script.contains(".screen-notice[role='alert']"));
        assertTrue(script.contains("DOMContentLoaded"));
        assertTrue(script.contains("markActionSubmission: markActionSubmission"));
        assertTrue(script.contains("transport: transport"));
    }

    @Test
    void declaresClosedResponsiveLayoutsForEveryPurpose() throws IOException {
        String css = resource("resources/logixone/shell.css");

        assertTrue(css.contains(".floorplan-master-data"));
        assertTrue(css.contains(".floorplan-worklist"));
        assertTrue(css.contains(".floorplan-transaction-editor"));
        assertTrue(css.contains(".floorplan-guided-operation"));
        assertTrue(css.contains(".floorplan-inquiry"));
        assertTrue(css.contains(".floorplan-region-content"));
        assertTrue(css.contains(".business-mobile-item-static"));
        assertTrue(css.contains("@media (min-width: 600px) and (max-width: 839px)"));
        assertTrue(css.contains("@media (max-width: 599px)"));
        assertTrue(css.contains(".button-destructive"));
        assertTrue(css.contains("@media (prefers-reduced-motion: reduce)"));
        assertTrue(css.contains("transition: none !important"));
    }

    private String resource(String relativePath) throws IOException {
        try (InputStream input = resourceStream(relativePath)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private InputStream resourceStream(String relativePath) {
        String classpath = "/META-INF/resources/" + relativePath;
        InputStream input = getClass().getResourceAsStream(classpath);
        assertNotNull(input, classpath);
        return input;
    }
}
