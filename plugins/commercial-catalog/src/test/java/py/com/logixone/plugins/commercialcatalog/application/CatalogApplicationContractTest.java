package py.com.logixone.plugins.commercialcatalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;

class CatalogApplicationContractTest {

    @Test
    void publishesExactlyFourSeparatedPermissions() {
        assertEquals(4, CommercialCatalogPermissions.all().size());
        assertEquals(4, CommercialCatalogPermissions.all().stream().distinct().count());
        assertTrue(CommercialCatalogPermissions.all().stream()
                .allMatch(permission -> permission.value().startsWith("commercial_catalog.")));
    }

    @Test
    void authorizesOnlyTheExactPluginAndPermission() {
        CatalogOperationContext allowed = context(
                CommercialCatalogIdentity.PLUGIN_ID, CommercialCatalogPermissions.ITEMS_MANAGE);
        CatalogOperationContext wrongPlugin = context(
                new PluginId("inventory"), CommercialCatalogPermissions.ITEMS_MANAGE);
        CatalogOperationContext wrongPermission = context(
                CommercialCatalogIdentity.PLUGIN_ID, CommercialCatalogPermissions.VIEW);

        assertTrue(allowed.authorizes(CommercialCatalogPermissions.ITEMS_MANAGE));
        assertFalse(wrongPlugin.authorizes(CommercialCatalogPermissions.ITEMS_MANAGE));
        assertFalse(wrongPermission.authorizes(CommercialCatalogPermissions.ITEMS_MANAGE));
    }

    @Test
    void keepsSuccessAndFailureResultsStructurallyUnambiguous() {
        assertEquals("value", CatalogOperationResult.success("value").value().orElseThrow());
        assertTrue(CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED).value().isEmpty());
        assertThrows(IllegalArgumentException.class, () ->
                new CatalogOperationResult<>(CatalogResultCode.SUCCESS, java.util.Optional.empty()));
        assertThrows(IllegalArgumentException.class, () ->
                CatalogOperationResult.failure(CatalogResultCode.SUCCESS));
    }

    private static CatalogOperationContext context(PluginId pluginId, ContributionId permission) {
        return new CatalogOperationContext(
                new AuthenticatedCompanyContext(
                        new AuthenticatedActor(new AppUserId(new UUID(0, 1))),
                        new CompanyId(new UUID(0, 2))),
                pluginId,
                permission,
                "request:catalog-test");
    }
}
