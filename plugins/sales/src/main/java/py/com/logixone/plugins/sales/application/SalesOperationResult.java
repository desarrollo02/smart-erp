package py.com.logixone.plugins.sales.application;

import java.util.Objects;
import java.util.Optional;

public record SalesOperationResult<T>(SalesResultCode code, Optional<T> value) {
    public SalesOperationResult {
        Objects.requireNonNull(code, "code");
        value = Objects.requireNonNull(value, "value");
        if ((code == SalesResultCode.SUCCESS) != value.isPresent()) {
            throw new IllegalArgumentException("Only SUCCESS may contain a value");
        }
    }
    public static <T> SalesOperationResult<T> success(T value) {
        return new SalesOperationResult<>(SalesResultCode.SUCCESS, Optional.of(value));
    }
    public static <T> SalesOperationResult<T> failure(SalesResultCode code) {
        return new SalesOperationResult<>(code, Optional.empty());
    }
    public boolean successful() { return code == SalesResultCode.SUCCESS; }
}
