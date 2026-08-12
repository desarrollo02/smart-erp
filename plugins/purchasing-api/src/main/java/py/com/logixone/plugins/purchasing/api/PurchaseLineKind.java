package py.com.logixone.plugins.purchasing.api;

/** Determines whether receiving a line changes physical stock. */
public enum PurchaseLineKind {
    STOCK,
    NON_STOCK,
    SERVICE
}
