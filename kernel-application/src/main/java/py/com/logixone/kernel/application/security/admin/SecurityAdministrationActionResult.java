package py.com.logixone.kernel.application.security.admin;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.application.security.SecurityOperationCode;
import py.com.logixone.kernel.application.security.SecurityOperationResult;
import py.com.logixone.kernel.application.security.SecurityOperationStatus;

public record SecurityAdministrationActionResult(
        SecurityOperationStatus status,
        Optional<SecurityOperationCode> failure) {

    public SecurityAdministrationActionResult {
        Objects.requireNonNull(status, "status");
        failure = Objects.requireNonNull(failure, "failure");
        if ((status == SecurityOperationStatus.REJECTED) != failure.isPresent()) {
            throw new IllegalArgumentException("only rejected results contain a failure");
        }
    }

    public static SecurityAdministrationActionResult from(SecurityOperationResult<?> result) {
        Objects.requireNonNull(result, "result");
        return new SecurityAdministrationActionResult(result.status(), result.failure());
    }

    public boolean changed() {
        return status == SecurityOperationStatus.CHANGED;
    }
}
