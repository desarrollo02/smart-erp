package py.com.logixone.plugins.commercialcatalog;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.MigrationContribution;
import py.com.logixone.plugin.api.MenuContribution;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogIdentity;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogPermissions;

/** Neutral discovery entry point for the commercial catalog plugin. */
@ApplicationScoped
public class CommercialCatalogPluginDefinition implements PluginDefinition {

    public static final PluginId ID = CommercialCatalogIdentity.PLUGIN_ID;
    public static final PluginId REFERENCE_DATA_ID = new PluginId("reference_data");

    private static final PluginDescriptor DESCRIPTOR = new PluginDescriptor(
            ID,
            PluginKind.FUNCTIONAL,
            SemanticVersion.parse("1.0.0"),
            new VersionRange(
                    SemanticVersion.parse("0.4.0"),
                    SemanticVersion.parse("0.5.0")),
            "Commercial catalog",
            List.of(new PluginDependency(
                    REFERENCE_DATA_ID,
                    new VersionRange(
                            SemanticVersion.parse("1.0.0"),
                            SemanticVersion.parse("2.0.0")),
                    DependencyKind.REQUIRED)),
            List.of(
                    new ContributionId("commercial_catalog.items"),
                    new ContributionId("commercial_catalog.price_lists"),
                    new ContributionId("commercial_catalog.definitions"),
                    new ContributionId("commercial_catalog.variant_families"),
                    new ContributionId("commercial_catalog.tax_profiles")),
            CommercialCatalogPermissions.all(),
            List.of(
                    new MenuContribution(
                            new ContributionId("commercial_catalog.items.menu"),
                            "commercial_catalog.menu.items",
                            CommercialCatalogScreenContract.ITEMS_ROUTE,
                            Optional.of(CommercialCatalogPermissions.VIEW)),
                    new MenuContribution(
                            new ContributionId("commercial_catalog.price_lists.menu"),
                            "commercial_catalog.menu.price_lists",
                            CommercialCatalogScreenContract.PRICE_LISTS_ROUTE,
                            Optional.of(CommercialCatalogPermissions.VIEW)),
                    new MenuContribution(
                            new ContributionId("commercial_catalog.definitions.menu"),
                            "commercial_catalog.menu.definitions",
                            CommercialCatalogScreenContract.DEFINITIONS_ROUTE,
                            Optional.of(CommercialCatalogPermissions.DEFINITIONS_MANAGE)),
                    new MenuContribution(
                            new ContributionId("commercial_catalog.variant_families.menu"),
                            "commercial_catalog.menu.variant_families",
                            CommercialCatalogScreenContract.VARIANT_FAMILIES_ROUTE,
                            Optional.of(CommercialCatalogPermissions.DEFINITIONS_MANAGE)),
                    new MenuContribution(
                            new ContributionId("commercial_catalog.tax_profiles.menu"),
                            "commercial_catalog.menu.tax_profiles",
                            CommercialCatalogScreenContract.TAX_PROFILES_ROUTE,
                            Optional.of(CommercialCatalogPermissions.DEFINITIONS_MANAGE))),
            List.of(new MigrationContribution(
                    "plg_commercial_catalog",
                    "classpath:db/migration/commercial_catalog")),
            List.of(
                    CommercialCatalogScreenContract.itemsDefinition(),
                    CommercialCatalogScreenContract.priceListsDefinition(),
                    CommercialCatalogScreenContract.definitionsDefinition(),
                    CommercialCatalogScreenContract.variantFamiliesDefinition(),
                    CommercialCatalogScreenContract.taxProfilesDefinition()),
            List.of());

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }
}
