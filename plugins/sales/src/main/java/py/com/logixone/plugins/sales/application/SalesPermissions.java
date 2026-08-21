package py.com.logixone.plugins.sales.application;

import java.util.List;
import py.com.logixone.plugin.api.ContributionId;

public final class SalesPermissions {
    public static final ContributionId VIEW = id("sales.view");
    public static final ContributionId TERMS_MANAGE = id("sales.terms.manage");
    public static final ContributionId QUOTES_CREATE = id("sales.quotes.create");
    public static final ContributionId QUOTES_ISSUE = id("sales.quotes.issue");
    public static final ContributionId QUOTES_ACCEPT = id("sales.quotes.accept");
    public static final ContributionId QUOTES_CANCEL = id("sales.quotes.cancel");
    public static final ContributionId PRICE_OVERRIDE = id("sales.prices.override");
    public static final ContributionId ORDERS_CREATE = id("sales.orders.create");
    public static final ContributionId ORDERS_CONFIRM = id("sales.orders.confirm");
    public static final ContributionId ORDERS_CANCEL = id("sales.orders.cancel");
    public static final ContributionId ORDERS_CLOSE = id("sales.orders.close");

    private SalesPermissions() { }

    public static List<ContributionId> all() {
        return List.of(VIEW, TERMS_MANAGE, QUOTES_CREATE, QUOTES_ISSUE,
                QUOTES_ACCEPT, QUOTES_CANCEL, PRICE_OVERRIDE, ORDERS_CREATE,
                ORDERS_CONFIRM, ORDERS_CANCEL, ORDERS_CLOSE);
    }

    private static ContributionId id(String value) { return new ContributionId(value); }
}
