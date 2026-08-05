package py.com.logixone.web.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.SelectorSourceKind;
import py.com.logixone.plugin.api.SelectorSourceOwnerKind;

class NativeSelectorSourceCatalogTest {

    @Test
    void publishesExactlyTheEighteenNativeSelectorsWithPlatformOwnership() {
        var sources = NativeSelectorSourceCatalog.all();

        assertEquals(18, sources.size());
        assertTrue(sources.values().stream()
                .allMatch(source -> source.owner().kind() == SelectorSourceOwnerKind.PLATFORM));
        assertTrue(sources.values().stream()
                .allMatch(source -> source.owner().id().equals("kernel")));
        assertEquals(11, sources.values().stream().filter(source -> source.manageable()).count());
        assertEquals(7, sources.values().stream().filter(source -> !source.manageable()).count());
    }

    @Test
    void governedReferencesExposeOnlyCanonicalAuthorizedAdministrationRoutes() {
        var sources = NativeSelectorSourceCatalog.all().values();
        Set<String> routes = sources.stream()
                .flatMap(source -> source.managementRoute().stream())
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "/admin/companies.xhtml",
                "/admin/security.xhtml",
                "/admin/system-authority.xhtml"), routes);
        assertTrue(sources.stream()
                .filter(source -> source.kind() == SelectorSourceKind.OPERATIONAL_REFERENCE
                        || source.kind() == SelectorSourceKind.BUSINESS_CATALOG)
                .allMatch(source -> source.managementPermission().isPresent()));
    }

    @Test
    void closedAndDeploymentSourcesNeverPretendToAllowRuntimeManagement() {
        var customizations = NativeSelectorSourceCatalog.source(
                NativeSelectorSourceCatalog.COMPANIES_CUSTOMIZATION);
        var permissions = NativeSelectorSourceCatalog.source(
                NativeSelectorSourceCatalog.SECURITY_GRANT_PERMISSION);
        var auditCategory = NativeSelectorSourceCatalog.source(
                NativeSelectorSourceCatalog.AUDIT_CATEGORY);

        assertEquals(SelectorSourceKind.DEPLOYMENT_COMPOSITION, customizations.kind());
        assertEquals(SelectorSourceKind.DEPLOYMENT_COMPOSITION, permissions.kind());
        assertEquals(SelectorSourceKind.CLOSED_STATE, auditCategory.kind());
        assertFalse(customizations.manageable());
        assertFalse(permissions.manageable());
        assertFalse(auditCategory.manageable());
        assertThrows(IllegalArgumentException.class,
                () -> NativeSelectorSourceCatalog.source("missing.selector"));
    }
}
