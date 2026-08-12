package py.com.logixone.plugins.purchasing.application;

public enum PurchasingResultCode {
    SUCCESS,
    ACCESS_DENIED,
    NOT_FOUND,
    DUPLICATE,
    VERSION_CONFLICT,
    REFERENCE_CONFLICT,
    IMMUTABLE_DOCUMENT,
    INVALID_OPERATION,
    IDEMPOTENCY_CONFLICT,
    INVENTORY_FAILURE,
    STORAGE_FAILURE
}
