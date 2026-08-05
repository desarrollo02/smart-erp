package py.com.logixone.plugins.inventory.application;

/** Stable results exposed by the internal application boundary. */
public enum InventoryResultCode {
    SUCCESS,
    ACCESS_DENIED,
    NOT_FOUND,
    VERSION_CONFLICT,
    DUPLICATE,
    REFERENCE_CONFLICT,
    SCOPE_LOCKED,
    INSUFFICIENT_STOCK,
    IDEMPOTENCY_CONFLICT,
    INVALID_OPERATION,
    STORAGE_FAILURE
}
