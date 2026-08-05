package py.com.logixone.plugins.commercialcatalog.application;

import java.util.List;
import py.com.logixone.plugin.api.ContributionId;

/** Public permission identifiers declared by the functional plugin. */
public final class CommercialCatalogPermissions {
    public static final ContributionId VIEW = new ContributionId("commercial_catalog.view");
    public static final ContributionId ITEMS_MANAGE =
            new ContributionId("commercial_catalog.items.manage");
    public static final ContributionId PRICES_MANAGE =
            new ContributionId("commercial_catalog.prices.manage");
    public static final ContributionId DEFINITIONS_MANAGE =
            new ContributionId("commercial_catalog.definitions.manage");

    private CommercialCatalogPermissions() {
    }

    public static List<ContributionId> all() {
        return List.of(VIEW, ITEMS_MANAGE, PRICES_MANAGE, DEFINITIONS_MANAGE);
    }
}
