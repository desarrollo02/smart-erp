package py.com.logixone.kernel.application.security.admin;

import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.domain.security.RoleId;
import py.com.logixone.kernel.domain.security.RoleStatus;
import py.com.logixone.plugin.api.ContributionId;

public record CompanyRoleAdministrationView(
        RoleId roleId,
        String code,
        String displayName,
        RoleStatus status,
        long version,
        List<ContributionId> grantedPermissions) {

    public CompanyRoleAdministrationView {
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(status, "status");
        grantedPermissions = List.copyOf(Objects.requireNonNull(grantedPermissions, "grantedPermissions"));
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
