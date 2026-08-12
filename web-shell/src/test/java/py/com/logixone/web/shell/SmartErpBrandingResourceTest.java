package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class SmartErpBrandingResourceTest {

    private static final List<String> BRANDED_PAGES = List.of(
            "app/index.xhtml",
            "app/view.xhtml",
            "admin/index.xhtml",
            "admin/companies.xhtml",
            "admin/plugins.xhtml",
            "admin/security.xhtml",
            "admin/system-authority.xhtml",
            "admin/audit.xhtml");

    @Test
    void everyShellPageUsesTheSmartErpBrand() throws IOException {
        for (String page : BRANDED_PAGES) {
            String source = resource(page);

            assertTrue(source.contains("<title>Smart ERP ·"), page);
            assertTrue(source.contains("<strong>Smart ERP</strong>"), page);
            assertTrue(source.contains(
                    "class=\"brand-mark\" aria-hidden=\"true\">S</span>"), page);
            assertFalse(source.contains("<title>Logixone"), page);
            assertFalse(source.contains("<strong>Logixone</strong>"), page);
            assertFalse(source.contains(">Logixone ·"), page);
        }
    }

    @Test
    void technicalCompatibilityIdentifiersRemainStable() throws IOException {
        String source = resource("app/view.xhtml");

        assertTrue(source.contains("ln=logixone"));
        assertTrue(source.contains("LogixoneSelectorReturn.capture(this)"));
        assertFalse(source.contains("ln=smart-erp"));
        assertFalse(source.contains("SmartErpSelectorReturn"));
    }

    @Test
    void workspaceExposesTheServerValidatedCompanyContext() throws IOException {
        String source = resource("app/index.xhtml");

        assertTrue(source.contains(
                "class=\"identity-copy\" data-company-id=\"#{shellView.selectedCompanyId}\""));
    }

    @Test
    void workspaceKeepsSelectorHelpReadableOnTheDarkTopbar() throws IOException {
        String source = resource("resources/logixone/shell.css");

        assertTrue(source.contains(".topbar .native-selector-source small { color: #c7d8d4; }"));
        assertTrue(source.contains(".topbar .selector-management-link { color: #8ff3e8; }"));
    }

    private String resource(String relativePath) throws IOException {
        String classpath = "/META-INF/resources/" + relativePath;
        try (InputStream input = getClass().getResourceAsStream(classpath)) {
            assertNotNull(input, classpath);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
