package py.com.logixone.plugins.purchasing.application;

import java.util.List;
import py.com.logixone.plugin.api.ContributionId;

public final class PurchasingPermissions {
    public static final ContributionId VIEW = id("purchasing.view");
    public static final ContributionId REQUESTS_CREATE = id("purchasing.requests.create");
    public static final ContributionId REQUESTS_SUBMIT = id("purchasing.requests.submit");
    public static final ContributionId REQUESTS_APPROVE = id("purchasing.requests.approve");
    public static final ContributionId ORDERS_CREATE = id("purchasing.orders.create");
    public static final ContributionId ORDERS_ISSUE = id("purchasing.orders.issue");
    public static final ContributionId ORDERS_CLOSE = id("purchasing.orders.close");
    public static final ContributionId RECEIPTS_CREATE = id("purchasing.receipts.create");
    public static final ContributionId RECEIPTS_CONFIRM = id("purchasing.receipts.confirm");
    public static final ContributionId RETURNS_CREATE = id("purchasing.returns.create");
    public static final ContributionId RETURNS_CONFIRM = id("purchasing.returns.confirm");
    public static final ContributionId IMPORTS_EXECUTE = id("purchasing.imports.execute");

    private PurchasingPermissions() {
    }

    public static List<ContributionId> all() {
        return List.of(VIEW, REQUESTS_CREATE, REQUESTS_SUBMIT, REQUESTS_APPROVE,
                ORDERS_CREATE, ORDERS_ISSUE, ORDERS_CLOSE, RECEIPTS_CREATE,
                RECEIPTS_CONFIRM, RETURNS_CREATE, RETURNS_CONFIRM, IMPORTS_EXECUTE);
    }

    private static ContributionId id(String value) {
        return new ContributionId(value);
    }
}
