package py.com.logixone.plugins.purchasing.api;

/** Commercial lifecycle of an order, separate from receipt fulfillment. */
public enum PurchaseOrderState {
    DRAFT,
    ISSUED,
    CLOSED,
    CANCELLED
}
