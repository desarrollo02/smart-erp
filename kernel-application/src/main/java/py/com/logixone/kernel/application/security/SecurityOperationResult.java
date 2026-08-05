package py.com.logixone.kernel.application.security;

import java.util.Objects;
import java.util.Optional;

/** Typed result for expected administration and bootstrap outcomes. */
public record SecurityOperationResult<T>(
        SecurityOperationStatus status,
        Optional<T> value,
        Optional<SecurityOperationCode> failure) {

    public SecurityOperationResult {
        Objects.requireNonNull(status, "status");
        value = Objects.requireNonNull(value, "value");
        failure = Objects.requireNonNull(failure, "failure");
        if (status == SecurityOperationStatus.REJECTED) {
            if (value.isPresent() || failure.isEmpty()) {
                throw new IllegalArgumentException("a rejected result requires only a failure");
            }
        } else if (value.isEmpty() || failure.isPresent()) {
            throw new IllegalArgumentException("a successful result requires only a value");
        }
    }

    public static <T> SecurityOperationResult<T> changed(T value) {
        return new SecurityOperationResult<>(
                SecurityOperationStatus.CHANGED,
                Optional.of(Objects.requireNonNull(value, "value")),
                Optional.empty());
    }

    public static <T> SecurityOperationResult<T> unchanged(T value) {
        return new SecurityOperationResult<>(
                SecurityOperationStatus.UNCHANGED,
                Optional.of(Objects.requireNonNull(value, "value")),
                Optional.empty());
    }

    public static <T> SecurityOperationResult<T> rejected(SecurityOperationCode failure) {
        return new SecurityOperationResult<>(
                SecurityOperationStatus.REJECTED,
                Optional.empty(),
                Optional.of(Objects.requireNonNull(failure, "failure")));
    }

    public boolean changed() {
        return status == SecurityOperationStatus.CHANGED;
    }
}
