package py.com.logixone.plugins.businesspartners.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record BusinessPartnerOperationResult<T>(
        BusinessPartnerResultCode code,
        Optional<T> value,
        List<BusinessPartnerWarning> warnings) {

    public BusinessPartnerOperationResult {
        Objects.requireNonNull(code, "code");
        value = Objects.requireNonNull(value, "value");
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
        if ((code == BusinessPartnerResultCode.SUCCESS) != value.isPresent()) {
            throw new IllegalArgumentException("Only a successful result may contain a value");
        }
        if (code != BusinessPartnerResultCode.SUCCESS && !warnings.isEmpty()) {
            throw new IllegalArgumentException("A failed result cannot contain warnings");
        }
    }

    public static <T> BusinessPartnerOperationResult<T> success(
            T value, List<BusinessPartnerWarning> warnings) {
        return new BusinessPartnerOperationResult<>(
                BusinessPartnerResultCode.SUCCESS,
                Optional.of(Objects.requireNonNull(value, "value")),
                warnings);
    }

    public static <T> BusinessPartnerOperationResult<T> failure(
            BusinessPartnerResultCode code) {
        if (code == BusinessPartnerResultCode.SUCCESS) {
            throw new IllegalArgumentException("SUCCESS is not a failure code");
        }
        return new BusinessPartnerOperationResult<>(code, Optional.empty(), List.of());
    }

    public boolean successful() {
        return code == BusinessPartnerResultCode.SUCCESS;
    }
}
