package py.com.logixone.kernel.application.company.admin;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.application.company.CompanyOperationCode;
import py.com.logixone.kernel.application.company.CompanyOperationResult;
import py.com.logixone.kernel.application.company.CompanyOperationStatus;

/** Presentation-safe command outcome without leaking entities through the web boundary. */
public record CompanyAdministrationActionResult(
        CompanyOperationStatus status,
        Optional<CompanyOperationCode> failure) {

    public CompanyAdministrationActionResult {
        Objects.requireNonNull(status, "status");
        failure = Objects.requireNonNull(failure, "failure");
        if ((status == CompanyOperationStatus.REJECTED) != failure.isPresent()) {
            throw new IllegalArgumentException("only rejected results contain a failure");
        }
    }

    public static CompanyAdministrationActionResult from(CompanyOperationResult<?> result) {
        Objects.requireNonNull(result, "result");
        return new CompanyAdministrationActionResult(result.status(), result.failure());
    }

    public boolean changed() {
        return status == CompanyOperationStatus.CHANGED;
    }
}
