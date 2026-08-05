package py.com.logixone.kernel.application.security.command;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.domain.security.RoleId;

public record UnassignRoleCommand(AppUserId userId, CompanyId companyId, RoleId roleId) {

    public UnassignRoleCommand {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(roleId, "roleId");
    }
}
