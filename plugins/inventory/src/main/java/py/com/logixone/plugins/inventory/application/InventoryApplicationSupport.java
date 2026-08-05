package py.com.logixone.plugins.inventory.application;

import java.util.Objects;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceCode;

final class InventoryApplicationSupport {
    private InventoryApplicationSupport() {
    }

    static boolean authorized(InventoryOperationContext context, ContributionId permission) {
        return Objects.requireNonNull(context, "context").authorizes(permission);
    }

    static InventoryResultCode map(InventoryPersistenceCode code) {
        return switch (Objects.requireNonNull(code, "code")) {
            case NOT_FOUND -> InventoryResultCode.NOT_FOUND;
            case VERSION_CONFLICT -> InventoryResultCode.VERSION_CONFLICT;
            case DUPLICATE -> InventoryResultCode.DUPLICATE;
            case REFERENCE_CONFLICT -> InventoryResultCode.REFERENCE_CONFLICT;
            case SCOPE_LOCKED -> InventoryResultCode.SCOPE_LOCKED;
            case STORAGE_FAILURE -> InventoryResultCode.STORAGE_FAILURE;
        };
    }
}
