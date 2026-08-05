package py.com.logixone.web.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CanonicalNavigationResourceTest {

    private static final List<String> ADMIN_VIEWS = List.of(
            "index.xhtml",
            "companies.xhtml",
            "plugins.xhtml",
            "security.xhtml",
            "system-authority.xhtml",
            "audit.xhtml");

    private static final List<String> APP_VIEWS = List.of("index.xhtml", "view.xhtml");

    @Test
    void adminViewsUseCanonicalFacesPathsInsteadOfRelativeNavigationOutcomes() throws IOException {
        for (String view : ADMIN_VIEWS) {
            String source = resource("admin/" + view);
            assertFalse(source.contains("<h:link"), view);
            assertFalse(source.contains("requestContextPath}/admin/"), view);
        }
        assertTrue(resource("admin/index.xhtml")
                .contains("requestContextPath}/faces#{section.outcome}"));
    }

    @Test
    void applicationViewsUseCanonicalFacesPaths() throws IOException {
        for (String view : APP_VIEWS) {
            String source = resource("app/" + view);
            assertFalse(source.contains("<h:link"), view);
            assertFalse(source.contains("requestContextPath}/app/"), view);
            assertTrue(source.contains("requestContextPath}/faces/app/"), view);
        }
        String pluginView = resource("app/view.xhtml");
        assertTrue(pluginView.contains("activeInteraction.table.rowsEmpty"));
        assertFalse(pluginView.contains("activeInteraction.table.empty}"));
        String routeParameter =
                "<input type=\"hidden\" name=\"route\" value=\"#{shellView.requestedRoute}\"/>";
        assertTrue(pluginView.indexOf(routeParameter) >= 0);
        assertTrue(pluginView.indexOf(routeParameter) != pluginView.lastIndexOf(routeParameter));
        assertTrue(pluginView.contains(
                "<input type=\"hidden\" name=\"resource\" value=\"#{shellView.selectedResourceId}\"/>"));
        assertTrue(pluginView.contains(
                "<input type=\"hidden\" name=\"version\" value=\"#{shellView.selectedResourceVersion}\"/>"));
    }

    @Test
    void nestedViewsUseContextAbsoluteStaticStylesheetPaths() throws IOException {
        for (String view : ADMIN_VIEWS) {
            String source = resource("admin/" + view);
            assertFalse(source.contains("<h:outputStylesheet"), view);
            assertTrue(source.contains(
                    "requestContextPath}/faces/jakarta.faces.resource/shell.css?ln=logixone"), view);
            assertTrue(source.contains(
                    "requestContextPath}/faces/jakarta.faces.resource/admin.css?ln=logixone"), view);
        }
        for (String view : APP_VIEWS) {
            String source = resource("app/" + view);
            assertFalse(source.contains("<h:outputStylesheet"), view);
            assertTrue(source.contains(
                    "requestContextPath}/faces/jakarta.faces.resource/shell.css?ln=logixone"), view);
        }
    }

    @Test
    void pluginMutationFormsCarryTheSelectedCompanyAcrossPostback() throws IOException {
        String source = resource("admin/plugins.xhtml");
        String nativeCompany =
                "<input type=\"hidden\" name=\"company\" value=\"#{pluginAdminView.companyId}\"/>";

        assertTrue(source.contains(nativeCompany));
        assertTrue(source.contains(
                "<input type=\"hidden\" name=\"plugin\" value=\"#{plugin.pluginId}\"/>"));
        assertTrue(source.contains(
                "<input type=\"hidden\" name=\"decisionVersion\" value=\"#{plugin.decisionVersion}\"/>"));
        assertTrue(source.contains("action=\"#{pluginAdminView.enableSelected}\""));
        assertTrue(source.contains("action=\"#{pluginAdminView.disableSelected}\""));
        assertTrue(source.contains("showSummary=\"true\" showDetail=\"true\""));
        assertTrue(source.contains(
                "styleClass=\"record-actions single-action\" includeViewParams=\"true\""));
        assertTrue(source.contains(
                "id=\"replace-customization\" styleClass=\"admin-inline-form\" includeViewParams=\"true\""));
        assertFalse(source.contains("<f:viewAction"));
    }

    @Test
    void securityMutationFormsExposeAnUnprefixedCompanyParameterForEarlyLoading() throws IOException {
        String source = resource("admin/security.xhtml");
        String companyParameter =
                "<input type=\"hidden\" name=\"company\" value=\"#{businessSecurityAdminView.companyId}\"/>";

        assertTrue(source.indexOf(companyParameter) >= 0);
        assertTrue(source.indexOf(companyParameter) != source.lastIndexOf(companyParameter));
        assertFalse(source.contains("<h:inputHidden value=\"#{businessSecurityAdminView.companyId}\"/>"));
    }

    private String resource(String relativePath) throws IOException {
        String classpath = "/META-INF/resources/" + relativePath;
        try (InputStream input = getClass().getResourceAsStream(classpath)) {
            assertNotNull(input, classpath);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
