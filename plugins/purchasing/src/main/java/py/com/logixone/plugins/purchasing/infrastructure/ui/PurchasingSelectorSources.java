package py.com.logixone.plugins.purchasing.infrastructure.ui;

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
import py.com.logixone.plugins.purchasing.PurchasingScreenContract;
import py.com.logixone.plugins.purchasing.application.PurchasingIdentity;
import py.com.logixone.plugins.purchasing.application.PurchasingPermissions;

/** Governed selector sources for every purchasing screen. */
final class PurchasingSelectorSources {
    private static final Set<SelectorManagementCapability> VIEW =
            Set.of(SelectorManagementCapability.VIEW);
    private static final Set<SelectorManagementCapability> MANAGE = Set.of(
            SelectorManagementCapability.VIEW, SelectorManagementCapability.CREATE,
            SelectorManagementCapability.EDIT, SelectorManagementCapability.INACTIVATE);

    static final Map<ScreenElementId, SelectorSourceDefinition> REQUESTS = Map.ofEntries(
            entry(PurchasingScreenContract.REQUEST_SEARCH_STATE, closed("request_states", true)),
            entry(PurchasingScreenContract.REQUEST_KIND, closed("line_kinds", false)),
            entry(PurchasingScreenContract.REQUEST_ITEM, catalogItems(true)),
            entry(PurchasingScreenContract.REQUEST_CURRENCY, currencies(true)),
            entry(PurchasingScreenContract.REQUEST_ADD_KIND, closed("line_kinds", false)),
            entry(PurchasingScreenContract.REQUEST_ADD_ITEM, catalogItems(true)),
            entry(PurchasingScreenContract.REQUEST_ADD_CURRENCY, currencies(true)));

    static final Map<ScreenElementId, SelectorSourceDefinition> ORDERS = Map.ofEntries(
            entry(PurchasingScreenContract.ORDER_SEARCH_STATE, closed("order_states", true)),
            entry(PurchasingScreenContract.ORDER_SUPPLIER, suppliers(false)),
            entry(PurchasingScreenContract.ORDER_CURRENCY, currencies(false)),
            entry(PurchasingScreenContract.ORDER_KIND, closed("line_kinds", false)),
            entry(PurchasingScreenContract.ORDER_ITEM, catalogItems(true)),
            entry(PurchasingScreenContract.ORDER_REQUEST, purchasingReference(
                    "approved_requests", PurchasingScreenContract.REQUESTS_ROUTE, true, true)),
            entry(PurchasingScreenContract.ORDER_REQUEST_LINE, purchasingReference(
                    "approved_request_lines", PurchasingScreenContract.REQUESTS_ROUTE, true, false)),
            entry(PurchasingScreenContract.ORDER_ADD_KIND, closed("line_kinds", false)),
            entry(PurchasingScreenContract.ORDER_ADD_ITEM, catalogItems(true)));

    static final Map<ScreenElementId, SelectorSourceDefinition> RECEIPTS = Map.ofEntries(
            entry(PurchasingScreenContract.RECEIPT_SEARCH_STATE, closed("receipt_states", true)),
            entry(PurchasingScreenContract.RECEIPT_ORDER, purchasingReference(
                    "receivable_orders", PurchasingScreenContract.ORDERS_ROUTE, false, true)),
            entry(PurchasingScreenContract.RECEIPT_ORDER_LINE, purchasingReference(
                    "receivable_order_lines", PurchasingScreenContract.ORDERS_ROUTE, false, false)),
            entry(PurchasingScreenContract.RECEIPT_WAREHOUSE, warehouses(true)),
            entry(PurchasingScreenContract.RECEIPT_LOCATION, locations(true)),
            entry(PurchasingScreenContract.RECEIPT_CONDITION, closed("stock_conditions", true)));

    static final Map<ScreenElementId, SelectorSourceDefinition> RETURNS = Map.ofEntries(
            entry(PurchasingScreenContract.RETURN_SEARCH_STATE, closed("return_states", true)),
            entry(PurchasingScreenContract.RETURN_ORDER, purchasingReference(
                    "returnable_orders", PurchasingScreenContract.ORDERS_ROUTE, false, true)),
            entry(PurchasingScreenContract.RETURN_RECEIPT, purchasingReference(
                    "confirmed_receipts", PurchasingScreenContract.RECEIPTS_ROUTE, false, true)),
            entry(PurchasingScreenContract.RETURN_RECEIPT_LINE, purchasingReference(
                    "confirmed_receipt_lines", PurchasingScreenContract.RECEIPTS_ROUTE, false, false)));

    static final Map<ScreenElementId, SelectorSourceDefinition> TRACKING = Map.of(
            PurchasingScreenContract.TRACKING_SEARCH_STATE, closed("order_states", true));

    private PurchasingSelectorSources() {
    }

    private static Map.Entry<ScreenElementId, SelectorSourceDefinition> entry(
            ScreenElementId field, SelectorSourceDefinition source) {
        return Map.entry(field, source);
    }

    private static SelectorSourceDefinition closed(String id, boolean meansAll) {
        return new SelectorSourceDefinition(
                new SelectorSourceId("purchasing." + id), PurchasingIdentity.PLUGIN_ID,
                SelectorSourceKind.CLOSED_STATE, SemanticVersion.parse("1.0.0"),
                Optional.empty(), Optional.empty(), Set.of(),
                meansAll ? SelectorEmptyOptionPolicy.MEANS_ALL
                        : SelectorEmptyOptionPolicy.NOT_ALLOWED,
                SelectorInactiveValuePolicy.NOT_APPLICABLE, SelectorLoadingStrategy.INLINE);
    }

    private static SelectorSourceDefinition catalogItems(boolean emptyAllowed) {
        return managed("commercial_catalog.purchase_items", new PluginId("commercial_catalog"),
                SelectorSourceKind.BUSINESS_CATALOG, "/catalog",
                new ContributionId("commercial_catalog.items.manage"), MANAGE,
                emptyAllowed, SelectorLoadingStrategy.SEARCH_ON_DEMAND);
    }

    private static SelectorSourceDefinition suppliers(boolean emptyAllowed) {
        return managed("business_partners.suppliers", new PluginId("business_partners"),
                SelectorSourceKind.BUSINESS_CATALOG, "/business-partners",
                new ContributionId("business_partners.manage"), MANAGE,
                emptyAllowed, SelectorLoadingStrategy.SEARCH_ON_DEMAND);
    }

    private static SelectorSourceDefinition currencies(boolean emptyAllowed) {
        return managed("reference_data.currencies", new PluginId("reference_data"),
                SelectorSourceKind.NORMATIVE_CATALOG, "/reference-data",
                new ContributionId("reference_data.view"), VIEW,
                emptyAllowed, SelectorLoadingStrategy.SEARCH_ON_DEMAND);
    }

    private static SelectorSourceDefinition warehouses(boolean emptyAllowed) {
        return managed("inventory.warehouses", new PluginId("inventory"),
                SelectorSourceKind.OPERATIONAL_REFERENCE, "/inventory/warehouses",
                new ContributionId("inventory.storage.manage"), MANAGE,
                emptyAllowed, SelectorLoadingStrategy.SEARCH_ON_DEMAND);
    }

    private static SelectorSourceDefinition locations(boolean emptyAllowed) {
        return managed("inventory.locations", new PluginId("inventory"),
                SelectorSourceKind.OPERATIONAL_REFERENCE, "/inventory/warehouses",
                new ContributionId("inventory.storage.manage"), MANAGE,
                emptyAllowed, SelectorLoadingStrategy.INLINE);
    }

    private static SelectorSourceDefinition purchasingReference(
            String id, String route, boolean emptyAllowed, boolean search) {
        return managed("purchasing." + id, PurchasingIdentity.PLUGIN_ID,
                SelectorSourceKind.OPERATIONAL_REFERENCE, route,
                PurchasingPermissions.VIEW, VIEW, emptyAllowed,
                search ? SelectorLoadingStrategy.SEARCH_ON_DEMAND
                        : SelectorLoadingStrategy.INLINE);
    }

    private static SelectorSourceDefinition managed(
            String id, PluginId owner, SelectorSourceKind kind, String route,
            ContributionId permission, Set<SelectorManagementCapability> capabilities,
            boolean emptyAllowed, SelectorLoadingStrategy loading) {
        return new SelectorSourceDefinition(
                new SelectorSourceId(id), owner, kind, SemanticVersion.parse("1.0.0"),
                Optional.of(route), Optional.of(permission), capabilities,
                emptyAllowed ? SelectorEmptyOptionPolicy.ALLOWED
                        : SelectorEmptyOptionPolicy.NOT_ALLOWED,
                SelectorInactiveValuePolicy.EXCLUDE_FOR_NEW_KEEP_SELECTED, loading);
    }
}
