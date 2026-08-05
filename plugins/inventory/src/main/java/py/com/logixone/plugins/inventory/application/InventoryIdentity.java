package py.com.logixone.plugins.inventory.application;

import py.com.logixone.plugin.api.PluginId;

/** Stable neutral identity shared by the descriptor and application boundary. */
public final class InventoryIdentity {
    public static final PluginId PLUGIN_ID = new PluginId("inventory");

    private InventoryIdentity() {
    }
}
