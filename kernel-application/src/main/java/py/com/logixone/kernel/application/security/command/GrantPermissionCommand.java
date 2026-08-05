package py.com.logixone.kernel.application.security.command;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.security.RoleId;
import py.com.logixone.plugin.api.ContributionId;

public record GrantPermissionCommand(
        CompanyId companyId,
        RoleId roleId,
        ContributionId permissionId) {

    public GrantPermissionCommand {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(permissionId, "permissionId");
    }
}
