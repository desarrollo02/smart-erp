package py.com.logixone.plugins.inventory.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;

class InventoryApplicationContractTest {

    @Test
    void publishesExactlySevenSeparatedPermissions() {
        assertEquals(7, InventoryPermissions.all().size());
        assertEquals(7, InventoryPermissions.all().stream().distinct().count());
        assertTrue(InventoryPermissions.all().stream()
                .allMatch(permission -> permission.value().startsWith("inventory.")));
    }

    @Test
    void authorizesOnlyTheExactPluginAndPermission() {
        InventoryOperationContext allowed = context(
                InventoryIdentity.PLUGIN_ID, InventoryPermissions.MOVEMENTS_POST);
        InventoryOperationContext wrongPlugin = context(
                new PluginId("commercial_catalog"), InventoryPermissions.MOVEMENTS_POST);
        InventoryOperationContext wrongPermission = context(
                InventoryIdentity.PLUGIN_ID, InventoryPermissions.VIEW);

        assertTrue(allowed.authorizes(InventoryPermissions.MOVEMENTS_POST));
        assertFalse(wrongPlugin.authorizes(InventoryPermissions.MOVEMENTS_POST));
        assertFalse(wrongPermission.authorizes(InventoryPermissions.MOVEMENTS_POST));
    }

    @Test
    void keepsSuccessAndFailureResultsUnambiguous() {
        assertEquals("ok", InventoryOperationResult.success("ok").value().orElseThrow());
        assertTrue(InventoryOperationResult.failure(InventoryResultCode.ACCESS_DENIED).value().isEmpty());
        assertThrows(IllegalArgumentException.class, () ->
                new InventoryOperationResult<>(InventoryResultCode.SUCCESS, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () ->
                InventoryOperationResult.failure(InventoryResultCode.SUCCESS));
    }

    private static InventoryOperationContext context(
            PluginId pluginId, ContributionId permissionId) {
        return new InventoryOperationContext(
                new AuthenticatedCompanyContext(
                        new AuthenticatedActor(new AppUserId(new UUID(0, 1))),
                        new CompanyId(new UUID(0, 2))),
                pluginId,
                permissionId,
                "request:inventory-contract");
    }
}
