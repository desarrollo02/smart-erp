package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SelectorReturnResourceTest {

    @Test
    void selectorLinksPostToTheShellAndBusinessInputsNeverBecomeQueryParameters()
            throws IOException {
        String source = resource("app/view.xhtml");

        assertEquals(5, occurrences(source,
                "action=\"#{shellView.openSelectorManagement(field.id)}\""));
        assertEquals(2, occurrences(source,
                "action=\"#{shellView.openSelectorManagement(element.id)}\""));
        assertEquals(4, occurrences(source, "name=\"selectorDraft\""));
        assertEquals(4, occurrences(source, "pt:data-screen-input=\"#{field.id}\""));
        assertEquals(6, occurrences(source, "data-screen-input=\"#{field.id}\""));
        assertTrue(source.contains("<select id=\"create-select-#{field.id}\""));
        assertTrue(source.contains("<select id=\"detail-select-#{field.id}\""));
        assertEquals(9, occurrences(source, "immediate=\"true\""));
        assertEquals(7, occurrences(source,
                "onclick=\"LogixoneSelectorReturn.capture(this);\""));
        assertEquals(2, occurrences(source,
                "pt:aria-label=\"#{shellView.activeInteraction.selectorSources[element.id].managementLabel} opciones de #{element.label}\""));
        assertTrue(source.contains("name=\"selectorSearch:#{element.id}\""));
        assertTrue(source.contains("name=\"selectorOption:#{element.id}\""));
        assertTrue(source.contains("name=\"selectorValue:#{element.id}\""));
        assertEquals(6, occurrences(source, "name=\"selectorSearch:#{field.id}\""));
        assertEquals(2, occurrences(source, "name=\"selectorQuery:#{field.id}\""));
        assertEquals(4, occurrences(source, "name=\"selectorValue:#{field.id}\""));
        assertTrue(source.contains(
                "listener=\"#{shellView.searchRequestedSelectorOptions}\""));
        assertTrue(source.contains("aria-required=\"#{field.required}\""));
        assertEquals(2, occurrences(source,
                "selected=\"#{shellView.inputValues[field.id] eq option.value ? 'selected' : null}\""));
        assertTrue(source.contains("id=\"create-selector-field\""));
        assertTrue(source.contains("id=\"detail-selector-field\""));
        assertEquals(2, occurrences(source,
                "name=\"selectorOption:#{field.id}\""));
        assertTrue(source.contains(
                "listener=\"#{shellView.selectRequestedSelectorOption}\""));
        assertFalse(source.contains("selectSelectorOption(field.id"));
        assertFalse(source.contains("requestedSelectorOption"));
        assertFalse(source.contains(
                "value=\"#{shellView.activeInteraction.selectorSources[field.id].managementRoute}\""));
        assertTrue(source.contains("action=\"#{shellView.returnToSelectorOrigin}\""));
        assertTrue(source.contains("name=\"selectorContext\""));
        assertTrue(source.contains("name=\"selectorReturn\""));
        assertFalse(source.matches("(?s).*<f:param[^>]+inputValues.*"));

        String normalized = source.replaceAll("\\s+", " ");
        String selectorMarkup = normalized.substring(
                normalized.indexOf("openSelectorManagement"));
        assertFalse(selectorMarkup.contains("<f:ajax"));
        assertFalse(source.contains("requestedSelectorFieldId"));
        assertFalse(source.contains("requestedSelectorPageDirection"));
        assertFalse(normalized.contains(
                "name=\"selectorOption:#{field.id}\" immediate=\"true\""));
    }

    @Test
    void returnBannerHasResponsiveShellOwnedStyling() throws IOException {
        String view = resource("app/view.xhtml");
        String css = resource("resources/logixone/shell.css");
        String script = resource("resources/logixone/selector-return.js");

        assertTrue(view.contains("styleClass=\"selector-return-banner\""));
        assertTrue(view.contains("Opciones actualizadas"));
        assertTrue(css.contains(".selector-return-banner"));
        assertTrue(css.contains(".selector-return-form"));
        assertTrue(script.contains("new URLSearchParams()"));
        assertTrue(script.contains("input[name='selectorDraft']"));
        assertFalse(script.contains("localStorage"));
        assertFalse(script.contains("sessionStorage"));
    }

    private String resource(String relativePath) throws IOException {
        String classpath = "/META-INF/resources/" + relativePath;
        try (InputStream input = getClass().getResourceAsStream(classpath)) {
            assertNotNull(input, classpath);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int occurrences(String source, String value) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(value, offset)) >= 0) {
            count++;
            offset += value.length();
        }
        return count;
    }
}
