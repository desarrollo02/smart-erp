package py.com.logixone.plugins.inventory.application.port;

import java.util.Objects;

public final class InventoryPersistenceException extends RuntimeException {
    private final InventoryPersistenceCode code;

    public InventoryPersistenceException(InventoryPersistenceCode code) {
        this(code, null);
    }

    public InventoryPersistenceException(InventoryPersistenceCode code, Throwable cause) {
        super(Objects.requireNonNull(code, "code").name(), cause);
        this.code = code;
    }

    public InventoryPersistenceCode code() {
        return code;
    }
}
