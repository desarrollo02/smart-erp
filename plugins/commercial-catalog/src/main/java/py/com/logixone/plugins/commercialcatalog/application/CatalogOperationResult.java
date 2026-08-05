package py.com.logixone.plugins.commercialcatalog.application;

import java.util.Objects;
import java.util.Optional;

public record CatalogOperationResult<T>(CatalogResultCode code, Optional<T> value) {
    public CatalogOperationResult {
        Objects.requireNonNull(code, "code");
        value = Objects.requireNonNull(value, "value");
        if ((code == CatalogResultCode.SUCCESS) != value.isPresent()) {
            throw new IllegalArgumentException("Only a successful result may contain a value");
        }
    }

    public static <T> CatalogOperationResult<T> success(T value) {
        return new CatalogOperationResult<>(
                CatalogResultCode.SUCCESS, Optional.of(Objects.requireNonNull(value, "value")));
    }

    public static <T> CatalogOperationResult<T> failure(CatalogResultCode code) {
        if (code == CatalogResultCode.SUCCESS) {
            throw new IllegalArgumentException("SUCCESS is not a failure code");
        }
        return new CatalogOperationResult<>(code, Optional.empty());
    }

    public boolean successful() {
        return code == CatalogResultCode.SUCCESS;
    }
}
