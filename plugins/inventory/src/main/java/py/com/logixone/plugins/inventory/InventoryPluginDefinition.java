package py.com.logixone.plugins.inventory;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.MenuContribution;
import py.com.logixone.plugin.api.MigrationContribution;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;
import py.com.logixone.plugins.inventory.api.InventoryContractVersion;
import py.com.logixone.plugins.inventory.application.InventoryIdentity;
import py.com.logixone.plugins.inventory.application.InventoryPermissions;

/** Neutral discovery entry point for the inventory plugin. */
@ApplicationScoped
public class InventoryPluginDefinition implements PluginDefinition {
    public static final PluginId ID = InventoryIdentity.PLUGIN_ID;
    public static final PluginId COMMERCIAL_CATALOG_ID = new PluginId("commercial_catalog");

    private static final PluginDescriptor DESCRIPTOR = new PluginDescriptor(
            ID,
            PluginKind.FUNCTIONAL,
            SemanticVersion.parse(InventoryContractVersion.CURRENT),
            new VersionRange(SemanticVersion.parse("0.4.0"), SemanticVersion.parse("0.5.0")),
            "Inventory",
            List.of(new PluginDependency(
                    COMMERCIAL_CATALOG_ID,
                    new VersionRange(SemanticVersion.parse("1.0.0"), SemanticVersion.parse("2.0.0")),
                    DependencyKind.REQUIRED)),
            List.of(
                    new ContributionId("inventory.availability"),
                    new ContributionId("inventory.movements"),
                    new ContributionId("inventory.reservations")),
            InventoryPermissions.all(),
            List.of(
                    new MenuContribution(
                            new ContributionId("inventory.stock.menu"),
                            "inventory.menu.stock",
                            InventoryScreenContract.STOCK_ROUTE,
                            Optional.of(InventoryPermissions.VIEW)),
                    new MenuContribution(
                            new ContributionId("inventory.warehouses.menu"),
                            "inventory.menu.warehouses",
                            InventoryScreenContract.WAREHOUSES_ROUTE,
                            Optional.of(InventoryPermissions.VIEW)),
                    new MenuContribution(
                            new ContributionId("inventory.counts.menu"),
                            "inventory.menu.counts",
                            InventoryScreenContract.COUNTS_ROUTE,
                            Optional.of(InventoryPermissions.VIEW))),
            List.of(new MigrationContribution(
                    "plg_inventory",
                    "classpath:db/migration/inventory")),
            List.of(
                    InventoryScreenContract.stockDefinition(),
                    InventoryScreenContract.warehousesDefinition(),
                    InventoryScreenContract.countsDefinition()),
            List.of());

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }
}
