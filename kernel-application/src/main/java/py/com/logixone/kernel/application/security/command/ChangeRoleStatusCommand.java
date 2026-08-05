package py.com.logixone.kernel.application.security.command;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.security.RoleId;
import py.com.logixone.kernel.domain.security.RoleStatus;

public record ChangeRoleStatusCommand(
        RoleId roleId,
        CompanyId companyId,
        RoleStatus desiredStatus,
        long expectedVersion) {

    public ChangeRoleStatusCommand {
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(desiredStatus, "desiredStatus");
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
    }
}
