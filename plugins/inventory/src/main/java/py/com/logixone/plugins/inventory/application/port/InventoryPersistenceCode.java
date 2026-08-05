package py.com.logixone.plugins.inventory.application.port;

public enum InventoryPersistenceCode {
    NOT_FOUND,
    VERSION_CONFLICT,
    DUPLICATE,
    REFERENCE_CONFLICT,
    SCOPE_LOCKED,
    STORAGE_FAILURE
}
