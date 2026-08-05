package py.com.logixone.plugins.businesspartners.application;

import java.util.List;
import py.com.logixone.plugin.api.ContributionId;

/** Public permission identifiers declared by the plugin descriptor. */
public final class BusinessPartnerPermissions {

    public static final ContributionId VIEW =
            new ContributionId("business_partners.view");
    public static final ContributionId MANAGE =
            new ContributionId("business_partners.manage");
    public static final ContributionId ROLES_MANAGE =
            new ContributionId("business_partners.roles.manage");
    public static final ContributionId LIFECYCLE_MANAGE =
            new ContributionId("business_partners.lifecycle.manage");

    private BusinessPartnerPermissions() {
    }

    public static List<ContributionId> all() {
        return List.of(VIEW, MANAGE, ROLES_MANAGE, LIFECYCLE_MANAGE);
    }
}
