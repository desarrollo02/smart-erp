package py.com.logixone.kernel.domain.security;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugin.api.ContributionId;

/** Historical grant from a company role to a public permission contribution id. */
public record RolePermissionGrant(
        CompanyId companyId,
        RoleId roleId,
        ContributionId permissionId) {

    public RolePermissionGrant {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(permissionId, "permissionId");
    }
}
