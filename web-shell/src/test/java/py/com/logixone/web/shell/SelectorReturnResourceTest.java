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

        assertEquals(3, occurrences(source,
                "action=\"#{shellView.openSelectorManagement(field.id)}\""));
        assertEquals(3, occurrences(source, "name=\"selectorDraft\""));
        assertEquals(6, occurrences(source, "pt:data-screen-input=\"#{field.id}\""));
        assertEquals(3, occurrences(source, "immediate=\"true\""));
        assertEquals(3, occurrences(source,
                "onclick=\"LogixoneSelectorReturn.capture(this);\""));
        assertFalse(source.contains(
                "value=\"#{shellView.activeInteraction.selectorSources[field.id].managementRoute}\""));
        assertTrue(source.contains("action=\"#{shellView.returnToSelectorOrigin}\""));
        assertTrue(source.contains("name=\"selectorContext\""));
        assertTrue(source.contains("name=\"selectorReturn\""));
        assertFalse(source.matches("(?s).*<f:param[^>]+inputValues.*"));
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
