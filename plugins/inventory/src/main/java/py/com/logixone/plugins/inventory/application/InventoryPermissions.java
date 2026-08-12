package py.com.logixone.plugins.inventory.application;

import java.util.List;
import py.com.logixone.plugin.api.ContributionId;

/** Public permission identifiers declared by inventory. */
public final class InventoryPermissions {
    public static final ContributionId VIEW = new ContributionId("inventory.view");
    public static final ContributionId STORAGE_MANAGE =
            new ContributionId("inventory.storage.manage");
    public static final ContributionId ITEMS_MANAGE =
            new ContributionId("inventory.items.manage");
    public static final ContributionId MOVEMENTS_POST =
            new ContributionId("inventory.movements.post");
    public static final ContributionId PURCHASE_MOVEMENTS_POST =
            new ContributionId("inventory.movements.purchase.post");
    public static final ContributionId RESERVATIONS_MANAGE =
            new ContributionId("inventory.reservations.manage");
    public static final ContributionId COUNTS_MANAGE =
            new ContributionId("inventory.counts.manage");
    public static final ContributionId ADJUSTMENTS_POST =
            new ContributionId("inventory.adjustments.post");

    private InventoryPermissions() {
    }

    public static List<ContributionId> all() {
        return List.of(
                VIEW,
                STORAGE_MANAGE,
                ITEMS_MANAGE,
                MOVEMENTS_POST,
                PURCHASE_MOVEMENTS_POST,
                RESERVATIONS_MANAGE,
                COUNTS_MANAGE,
                ADJUSTMENTS_POST);
    }
}
