package py.com.logixone.plugins.inventory.application;

import java.util.Objects;
import java.util.Optional;

public record InventoryOperationResult<T>(InventoryResultCode code, Optional<T> value) {
    public InventoryOperationResult {
        Objects.requireNonNull(code, "code");
        value = Objects.requireNonNull(value, "value");
        if ((code == InventoryResultCode.SUCCESS) != value.isPresent()) {
            throw new IllegalArgumentException("Only a successful result may contain a value");
        }
    }

    public static <T> InventoryOperationResult<T> success(T value) {
        return new InventoryOperationResult<>(
                InventoryResultCode.SUCCESS, Optional.of(Objects.requireNonNull(value, "value")));
    }

    public static <T> InventoryOperationResult<T> failure(InventoryResultCode code) {
        if (code == InventoryResultCode.SUCCESS) {
            throw new IllegalArgumentException("SUCCESS is not a failure code");
        }
        return new InventoryOperationResult<>(code, Optional.empty());
    }

    public boolean successful() {
        return code == InventoryResultCode.SUCCESS;
    }
}
