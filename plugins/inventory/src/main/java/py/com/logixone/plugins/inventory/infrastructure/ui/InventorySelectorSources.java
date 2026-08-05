package py.com.logixone.plugins.inventory.infrastructure.ui;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.SelectorEmptyOptionPolicy;
import py.com.logixone.plugin.api.SelectorInactiveValuePolicy;
import py.com.logixone.plugin.api.SelectorLoadingStrategy;
import py.com.logixone.plugin.api.SelectorManagementCapability;
import py.com.logixone.plugin.api.SelectorSourceDefinition;
import py.com.logixone.plugin.api.SelectorSourceId;
import py.com.logixone.plugin.api.SelectorSourceKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugins.inventory.InventoryScreenContract;
import py.com.logixone.plugins.inventory.application.InventoryIdentity;
import py.com.logixone.plugins.inventory.application.InventoryPermissions;

/** Complete selector governance for the current inventory screens. */
final class InventorySelectorSources {

    private static final PluginId COMMERCIAL_CATALOG = new PluginId("commercial_catalog");
    private static final ContributionId CATALOG_ITEMS_MANAGE =
            new ContributionId("commercial_catalog.items.manage");
    private static final Set<SelectorManagementCapability> FULL_CATALOG = Set.of(
            SelectorManagementCapability.VIEW,
            SelectorManagementCapability.CREATE,
            SelectorManagementCapability.EDIT,
            SelectorManagementCapability.INACTIVATE);

    static final Map<ScreenElementId, SelectorSourceDefinition> WAREHOUSES = Map.of(
            InventoryScreenContract.WAREHOUSE_SEARCH_STATE,
            closed("inventory.warehouse_states", SelectorEmptyOptionPolicy.MEANS_ALL),
            InventoryScreenContract.LOCATION_NEW_TYPE,
            closed("inventory.location_types", SelectorEmptyOptionPolicy.NOT_ALLOWED),
            InventoryScreenContract.LOCATION_TO_RENAME,
            locations(SelectorEmptyOptionPolicy.ALLOWED),
            InventoryScreenContract.LOCATION_TO_INACTIVATE,
            locations(SelectorEmptyOptionPolicy.ALLOWED));

    static final Map<ScreenElementId, SelectorSourceDefinition> STOCK = Map.ofEntries(
            entry(InventoryScreenContract.STOCK_SEARCH_STATE,
                    closed("inventory.stock_item_states", SelectorEmptyOptionPolicy.MEANS_ALL)),
            entry(InventoryScreenContract.STOCK_NEW_CATALOG_ITEM,
                    managed(
                            "commercial_catalog.items",
                            COMMERCIAL_CATALOG,
                            "/catalog",
                            CATALOG_ITEMS_MANAGE,
                            FULL_CATALOG,
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.STOCK_NEW_TRACKING,
                    closed("inventory.tracking_modes", SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.STOCK_NEW_EXPIRY,
                    closed("inventory.expiry_policies", SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.AVAILABILITY_WAREHOUSE,
                    warehouses(SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.AVAILABILITY_LOCATION,
                    locations(SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.AVAILABILITY_CONDITION,
                    closed("inventory.stock_conditions", SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.MOVEMENT_TYPE,
                    closed("inventory.movement_types", SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.MOVEMENT_WAREHOUSE,
                    warehouses(SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.MOVEMENT_LOCATION,
                    locations(SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.MOVEMENT_TARGET_WAREHOUSE,
                    warehouses(SelectorEmptyOptionPolicy.ALLOWED)),
            entry(InventoryScreenContract.MOVEMENT_TARGET_LOCATION,
                    locations(SelectorEmptyOptionPolicy.ALLOWED)),
            entry(InventoryScreenContract.MOVEMENT_CONDITION,
                    closed("inventory.stock_conditions", SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.RESERVATION_WAREHOUSE,
                    warehouses(SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.RESERVATION_LOCATION,
                    locations(SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.RESERVATION_CONDITION,
                    closed("inventory.stock_conditions", SelectorEmptyOptionPolicy.NOT_ALLOWED)));

    static final Map<ScreenElementId, SelectorSourceDefinition> COUNTS = Map.ofEntries(
            entry(InventoryScreenContract.COUNT_SEARCH_STATE,
                    closed("inventory.count_states", SelectorEmptyOptionPolicy.MEANS_ALL)),
            entry(InventoryScreenContract.COUNT_NEW_WAREHOUSE,
                    warehouses(SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.COUNT_NEW_LOCATION,
                    locations(SelectorEmptyOptionPolicy.ALLOWED)),
            entry(InventoryScreenContract.COUNT_LINE_ITEM,
                    managed(
                            "inventory.stock_items",
                            InventoryIdentity.PLUGIN_ID,
                            InventoryScreenContract.STOCK_ROUTE,
                            InventoryPermissions.ITEMS_MANAGE,
                            FULL_CATALOG,
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.COUNT_LINE_LOCATION,
                    locations(SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.COUNT_LINE_CONDITION,
                    closed("inventory.stock_conditions", SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(InventoryScreenContract.COUNT_CAPTURE_LINE,
                    managed(
                            "inventory.count_lines",
                            InventoryIdentity.PLUGIN_ID,
                            InventoryScreenContract.COUNTS_ROUTE,
                            InventoryPermissions.COUNTS_MANAGE,
                            Set.of(
                                    SelectorManagementCapability.VIEW,
                                    SelectorManagementCapability.CREATE,
                                    SelectorManagementCapability.EDIT),
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)));

    private InventorySelectorSources() {
    }

    private static Map.Entry<ScreenElementId, SelectorSourceDefinition> entry(
            ScreenElementId field, SelectorSourceDefinition source) {
        return Map.entry(field, source);
    }

    private static SelectorSourceDefinition warehouses(SelectorEmptyOptionPolicy emptyPolicy) {
        return managed(
                "inventory.warehouses",
                InventoryIdentity.PLUGIN_ID,
                InventoryScreenContract.WAREHOUSES_ROUTE,
                InventoryPermissions.STORAGE_MANAGE,
                FULL_CATALOG,
                emptyPolicy);
    }

    private static SelectorSourceDefinition locations(SelectorEmptyOptionPolicy emptyPolicy) {
        return managed(
                "inventory.locations",
                InventoryIdentity.PLUGIN_ID,
                InventoryScreenContract.WAREHOUSES_ROUTE,
                InventoryPermissions.STORAGE_MANAGE,
                FULL_CATALOG,
                emptyPolicy);
    }

    private static SelectorSourceDefinition closed(
            String id, SelectorEmptyOptionPolicy emptyPolicy) {
        return new SelectorSourceDefinition(
                new SelectorSourceId(id),
                InventoryIdentity.PLUGIN_ID,
                SelectorSourceKind.CLOSED_STATE,
                SemanticVersion.parse("1.0.0"),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                emptyPolicy,
                SelectorInactiveValuePolicy.NOT_APPLICABLE,
                SelectorLoadingStrategy.INLINE);
    }

    private static SelectorSourceDefinition managed(
            String id,
            PluginId owner,
            String route,
            ContributionId permission,
            Set<SelectorManagementCapability> capabilities,
            SelectorEmptyOptionPolicy emptyPolicy) {
        return new SelectorSourceDefinition(
                new SelectorSourceId(id),
                owner,
                SelectorSourceKind.OPERATIONAL_REFERENCE,
                SemanticVersion.parse("1.0.0"),
                Optional.of(route),
                Optional.of(permission),
                capabilities,
                emptyPolicy,
                SelectorInactiveValuePolicy.EXCLUDE_FOR_NEW_KEEP_SELECTED,
                SelectorLoadingStrategy.INLINE);
    }
}
