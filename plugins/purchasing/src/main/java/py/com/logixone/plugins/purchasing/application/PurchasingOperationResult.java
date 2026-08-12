package py.com.logixone.plugins.purchasing.application;

import java.util.Objects;
import java.util.Optional;

public record PurchasingOperationResult<T>(PurchasingResultCode code, Optional<T> value) {
    public PurchasingOperationResult {
        Objects.requireNonNull(code, "code");
        value = Objects.requireNonNull(value, "value");
        if ((code == PurchasingResultCode.SUCCESS) != value.isPresent()) {
            throw new IllegalArgumentException("Only SUCCESS may contain a value");
        }
    }

    public static <T> PurchasingOperationResult<T> success(T value) {
        return new PurchasingOperationResult<>(PurchasingResultCode.SUCCESS,
                Optional.of(Objects.requireNonNull(value, "value")));
    }

    public static <T> PurchasingOperationResult<T> failure(PurchasingResultCode code) {
        if (code == PurchasingResultCode.SUCCESS) {
            throw new IllegalArgumentException("SUCCESS is not a failure");
        }
        return new PurchasingOperationResult<>(code, Optional.empty());
    }

    public boolean successful() {
        return code == PurchasingResultCode.SUCCESS;
    }
}
