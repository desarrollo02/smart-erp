package py.com.logixone.kernel.application.company.command;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.company.CompanyStatus;

public record ChangeCompanyStatusCommand(
        CompanyId companyId,
        CompanyStatus desiredStatus,
        long expectedVersion) {

    public ChangeCompanyStatusCommand {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(desiredStatus, "desiredStatus");
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
    }
}
