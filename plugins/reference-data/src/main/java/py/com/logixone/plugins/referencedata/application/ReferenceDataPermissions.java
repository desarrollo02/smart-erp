package py.com.logixone.plugins.referencedata.application;

import java.util.List;
import py.com.logixone.plugin.api.ContributionId;

/** Permissions owned by reference_data; publication updates remain deployment controlled. */
public final class ReferenceDataPermissions {

    public static final ContributionId VIEW = new ContributionId("reference_data.view");
    public static final ContributionId POLICY_MANAGE =
            new ContributionId("reference_data.policy.manage");

    private ReferenceDataPermissions() {
    }

    public static List<ContributionId> all() {
        return List.of(VIEW, POLICY_MANAGE);
    }
}
