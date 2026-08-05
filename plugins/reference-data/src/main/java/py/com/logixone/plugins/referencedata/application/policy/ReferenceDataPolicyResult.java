package py.com.logixone.plugins.referencedata.application.policy;

import java.util.Objects;
import java.util.Optional;

/** Closed result returned to inbound adapters without leaking persistence failures. */
public record ReferenceDataPolicyResult<T>(Code code, Optional<T> value) {

    public enum Code {
        SUCCESS,
        ACCESS_DENIED,
        NOT_FOUND,
        VERSION_CONFLICT
    }

    public ReferenceDataPolicyResult {
        Objects.requireNonNull(code, "code");
        value = Objects.requireNonNull(value, "value");
        if ((code == Code.SUCCESS) != value.isPresent()) {
            throw new IllegalArgumentException("Successful results require a value");
        }
    }

    public static <T> ReferenceDataPolicyResult<T> success(T value) {
        return new ReferenceDataPolicyResult<>(Code.SUCCESS, Optional.of(value));
    }

    public static <T> ReferenceDataPolicyResult<T> failure(Code code) {
        if (code == Code.SUCCESS) {
            throw new IllegalArgumentException("Use success for successful results");
        }
        return new ReferenceDataPolicyResult<>(code, Optional.empty());
    }

    public boolean successful() {
        return code == Code.SUCCESS;
    }
}
