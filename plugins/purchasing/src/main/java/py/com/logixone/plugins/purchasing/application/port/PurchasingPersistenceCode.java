package py.com.logixone.plugins.purchasing.application.port;

public enum PurchasingPersistenceCode {
    DUPLICATE,
    REFERENCE_CONFLICT,
    VERSION_CONFLICT,
    IMMUTABLE_DOCUMENT,
    NOT_FOUND,
    STORAGE_FAILURE
}
