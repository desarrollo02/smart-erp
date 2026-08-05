package py.com.logixone.web.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NativeSelectorMetadataResourceTest {

    private static final Map<String, List<String>> VIEW_USAGES = Map.of(
            "app/index.xhtml", List.of(
                    NativeSelectorSourceCatalog.APP_COMPANY_SWITCHER,
                    NativeSelectorSourceCatalog.APP_COMPANY_SELECTION),
            "admin/companies.xhtml", List.of(
                    NativeSelectorSourceCatalog.COMPANIES_CUSTOMIZATION),
            "admin/plugins.xhtml", List.of(
                    NativeSelectorSourceCatalog.PLUGINS_COMPANY,
                    NativeSelectorSourceCatalog.PLUGINS_CUSTOMIZATION),
            "admin/security.xhtml", List.of(
                    NativeSelectorSourceCatalog.SECURITY_COMPANY,
                    NativeSelectorSourceCatalog.SECURITY_MEMBERSHIP_USER,
                    NativeSelectorSourceCatalog.SECURITY_ASSIGNMENT_USER,
                    NativeSelectorSourceCatalog.SECURITY_ASSIGNMENT_ROLE,
                    NativeSelectorSourceCatalog.SECURITY_GRANT_ROLE,
                    NativeSelectorSourceCatalog.SECURITY_GRANT_PERMISSION),
            "admin/system-authority.xhtml", List.of(
                    NativeSelectorSourceCatalog.SYSTEM_ASSIGNMENT_USER,
                    NativeSelectorSourceCatalog.SYSTEM_ASSIGNMENT_ROLE,
                    NativeSelectorSourceCatalog.SYSTEM_GRANT_ROLE,
                    NativeSelectorSourceCatalog.SYSTEM_GRANT_PERMISSION),
            "admin/audit.xhtml", List.of(
                    NativeSelectorSourceCatalog.AUDIT_CATEGORY,
                    NativeSelectorSourceCatalog.AUDIT_OUTCOME,
                    NativeSelectorSourceCatalog.AUDIT_WINDOW));

    @Test
    void everyNativeSelectRendersExactlyOneGovernanceComponent() throws IOException {
        int selectCount = 0;
        int metadataCount = 0;

        for (var entry : VIEW_USAGES.entrySet()) {
            String source = resource(entry.getKey());
            int viewSelects = occurrences(source, "<h:selectOneMenu");
            int viewMetadata = occurrences(source, "<lx:selectorSource");
            assertEquals(entry.getValue().size(), viewSelects, entry.getKey());
            assertEquals(viewSelects, viewMetadata, entry.getKey());
            assertTrue(source.contains("xmlns:lx=\"jakarta.faces.composite/logixone\""),
                    entry.getKey());
            for (String usageId : entry.getValue()) {
                assertEquals(1, occurrences(source,
                        "nativeSelectorSources.sources['" + usageId + "']"), usageId);
            }
            selectCount += viewSelects;
            metadataCount += viewMetadata;
        }

        assertEquals(18, selectCount);
        assertEquals(18, metadataCount);
    }

    @Test
    void shellOwnedCompositeShowsOriginAndOnlyRendersAuthorizedManagement() throws IOException {
        String source = resource("logixone/selectorSource.xhtml");

        assertTrue(source.contains("cc.attrs.source.helpText"));
        assertTrue(source.contains("rendered=\"#{cc.attrs.source.managementAvailable}\""));
        assertTrue(source.contains(
                "action=\"#{nativeSelectorReturn.open(cc.attrs.source.usageId)}\""));
        assertTrue(source.contains("pt:data-native-selector-return=\"true\""));
        assertFalse(source.contains("onclick="));
        assertTrue(source.contains("cc.attrs.source.managementLabel"));
        assertFalse(source.contains("/faces#{cc.attrs.source.managementRoute}"));
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
