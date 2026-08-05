package py.com.logixone.kernel.application.company;

import java.util.Objects;
import java.util.Optional;

/** Explicit neutral result for administrative company operations. */
public record CompanyOperationResult<T>(
        CompanyOperationStatus status,
        Optional<T> value,
        Optional<CompanyOperationCode> failure) {

    public CompanyOperationResult {
        Objects.requireNonNull(status, "status");
        value = Objects.requireNonNull(value, "value");
        failure = Objects.requireNonNull(failure, "failure");
        if (status == CompanyOperationStatus.REJECTED) {
            if (failure.isEmpty() || value.isPresent()) {
                throw new IllegalArgumentException("a rejected result requires only a failure");
            }
        } else if (value.isEmpty() || failure.isPresent()) {
            throw new IllegalArgumentException("a successful result requires only a value");
        }
    }

    public static <T> CompanyOperationResult<T> changed(T value) {
        return new CompanyOperationResult<>(
                CompanyOperationStatus.CHANGED,
                Optional.of(Objects.requireNonNull(value, "value")),
                Optional.empty());
    }

    public static <T> CompanyOperationResult<T> unchanged(T value) {
        return new CompanyOperationResult<>(
                CompanyOperationStatus.UNCHANGED,
                Optional.of(Objects.requireNonNull(value, "value")),
                Optional.empty());
    }

    public static <T> CompanyOperationResult<T> rejected(CompanyOperationCode failure) {
        return new CompanyOperationResult<>(
                CompanyOperationStatus.REJECTED,
                Optional.empty(),
                Optional.of(Objects.requireNonNull(failure, "failure")));
    }

    public boolean changed() {
        return status == CompanyOperationStatus.CHANGED;
    }
}
