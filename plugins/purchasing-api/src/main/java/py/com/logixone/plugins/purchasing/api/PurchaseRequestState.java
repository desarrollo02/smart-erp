package py.com.logixone.plugins.purchasing.api;

/** Independent lifecycle of a purchase request. */
public enum PurchaseRequestState {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    CANCELLED
}
