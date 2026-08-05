package py.com.logixone.web.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NativeSelectorReturnResourceTest {

    @Test
    void everyNativeOriginCapturesOnlyExplicitDraftFieldsInsideItsPostForm() throws IOException {
        Map<String, Integer> expectedDraftForms = Map.of(
                "app/index.xhtml", 2,
                "admin/plugins.xhtml", 1,
                "admin/security.xhtml", 4,
                "admin/system-authority.xhtml", 2);

        for (var expected : expectedDraftForms.entrySet()) {
            String source = resource(expected.getKey());
            assertEquals(expected.getValue(), occurrences(source,
                    "name=\"selectorDraft\""), expected.getKey());
            assertTrue(source.contains("data-selector-draft"), expected.getKey());
            assertTrue(source.contains("selector-return.js?ln=logixone"), expected.getKey());
            assertTrue(source.contains("<lx:selectorReturnStatus/>"), expected.getKey());
        }

        String security = resource("admin/security.xhtml");
        assertTrue(security.contains("data-selector-draft=\"company_id\""));
        assertTrue(security.contains("pt:data-selector-draft=\"membership_user_id\""));
        assertTrue(security.contains("pt:data-selector-draft=\"assignment_user_id\""));
        assertTrue(security.contains("pt:data-selector-draft=\"assignment_role_id\""));
        assertTrue(security.contains("pt:data-selector-draft=\"grant_role_id\""));
        assertTrue(security.contains("pt:data-selector-draft=\"grant_permission_id\""));
    }

    @Test
    void everyNativeTargetPreservesOnlyTheOpaqueContextAcrossItsPostbacks() throws IOException {
        Map<String, Integer> expectedForms = Map.of(
                "admin/companies.xhtml", 2,
                "admin/security.xhtml", 11,
                "admin/system-authority.xhtml", 6);

        for (var expected : expectedForms.entrySet()) {
            String path = expected.getKey();
            String source = resource(path);
            assertTrue(source.contains(
                    "<f:viewParam name=\"selectorContext\" value=\"#{nativeSelectorReturn.selectorContextId}\"/>"),
                    path);
            assertTrue(source.contains("<lx:selectorReturnStatus/>"), path);
            assertEquals(expected.getValue(), occurrences(source, "<h:form"), path);
            assertEquals(expected.getValue(), occurrences(source,
                    "<input type=\"hidden\" name=\"selectorContext\""
            ), path);
            source.lines().filter(line -> line.contains("<h:form"))
                    .forEach(line -> assertTrue(
                            line.contains("includeViewParams=\"true\""), path + ": " + line));
            assertFalse(source.contains("selectorReturn.inputs"), path);
        }
    }

    @Test
    void sharedResourcesUseSeparateCaptureMarkersAndAReauthorizedReturnAction()
            throws IOException {
        String script = resource("resources/logixone/selector-return.js");
        String status = resource("logixone/selectorReturnStatus.xhtml");

        assertTrue(script.contains("[data-screen-input]"));
        assertTrue(script.contains("[data-selector-draft]"));
        assertTrue(script.contains("[data-native-selector-return='true']"));
        assertTrue(script.contains("}, true);"));
        assertFalse(script.contains("localStorage"));
        assertFalse(script.contains("sessionStorage"));
        assertTrue(status.contains("action=\"#{nativeSelectorReturn.returnToOrigin}\""));
        assertTrue(status.contains(
                "<input type=\"hidden\" name=\"selectorContext\" value=\"#{nativeSelectorReturn.selectorContextId}\"/>"));
        assertTrue(status.contains("nativeSelectorReturn.restored"));
    }

    @Test
    void systemAssignmentStacksItsManagedSelectorsAtMediumWidth() throws IOException {
        String view = resource("admin/system-authority.xhtml");
        String styles = resource("resources/logixone/admin.css");

        assertTrue(view.contains(
                "id=\"system-assignment-form\" styleClass=\"admin-form-grid system-assignment-grid\""));
        assertTrue(styles.contains("@media (min-width: 600px) and (max-width: 839px)"));
        assertTrue(styles.replace("\r\n", "\n").contains(
                ".system-assignment-grid,\n    .admin-form-grid-wide {\n"
                        + "        grid-template-columns: minmax(0, 1fr);\n    }"));
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
