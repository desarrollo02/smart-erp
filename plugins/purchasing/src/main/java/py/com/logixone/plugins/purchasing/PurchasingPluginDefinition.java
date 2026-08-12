package py.com.logixone.plugins.purchasing;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.MigrationContribution;
import py.com.logixone.plugin.api.MenuContribution;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;
import py.com.logixone.plugins.purchasing.api.PurchasingContractVersion;
import py.com.logixone.plugins.purchasing.application.PurchasingIdentity;
import py.com.logixone.plugins.purchasing.application.PurchasingPermissions;

/** Neutral discovery entry point for the purchasing plugin. */
@ApplicationScoped
public class PurchasingPluginDefinition implements PluginDefinition {
    public static final PluginId ID = PurchasingIdentity.PLUGIN_ID;

    private static final PluginDescriptor DESCRIPTOR = new PluginDescriptor(
            ID,
            PluginKind.FUNCTIONAL,
            SemanticVersion.parse(PurchasingContractVersion.CURRENT),
            new VersionRange(SemanticVersion.parse("0.4.0"), SemanticVersion.parse("0.5.0")),
            "Purchasing",
            List.of(
                    required("business_partners", "1.1.0"),
                    required("commercial_catalog", "1.1.0"),
                    required("reference_data"),
                    required("inventory", "1.1.0")),
            List.of(
                    new ContributionId("purchasing.requests"),
                    new ContributionId("purchasing.orders"),
                    new ContributionId("purchasing.receipts"),
                    new ContributionId("purchasing.returns"),
                    new ContributionId("purchasing.imports")),
            PurchasingPermissions.all(),
            List.of(
                    menu("requests", PurchasingScreenContract.REQUESTS_ROUTE),
                    menu("orders", PurchasingScreenContract.ORDERS_ROUTE),
                    menu("receipts", PurchasingScreenContract.RECEIPTS_ROUTE),
                    menu("returns", PurchasingScreenContract.RETURNS_ROUTE),
                    menu("tracking", PurchasingScreenContract.TRACKING_ROUTE)),
            List.of(new MigrationContribution(
                    "plg_purchasing",
                    "classpath:db/migration/purchasing")),
            List.of(
                    PurchasingScreenContract.requestsDefinition(),
                    PurchasingScreenContract.ordersDefinition(),
                    PurchasingScreenContract.receiptsDefinition(),
                    PurchasingScreenContract.returnsDefinition(),
                    PurchasingScreenContract.trackingDefinition()),
            List.of());

    private static MenuContribution menu(String id, String route) {
        return new MenuContribution(
                new ContributionId("purchasing." + id + ".menu"),
                "purchasing.menu." + id, route,
                java.util.Optional.of(PurchasingPermissions.VIEW));
    }

    private static PluginDependency required(String pluginId) {
        return required(pluginId, "1.0.0");
    }

    private static PluginDependency required(String pluginId, String minimumVersion) {
        return new PluginDependency(
                new PluginId(pluginId), new VersionRange(
                        SemanticVersion.parse(minimumVersion),
                        SemanticVersion.parse("2.0.0")), DependencyKind.REQUIRED);
    }

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }
}
