package py.com.logixone.kernel.domain.security;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;

/** Role assignment scoped explicitly to one user membership and one company. */
public record MembershipRoleAssignment(
        AppUserId userId,
        CompanyId companyId,
        RoleId roleId) {

    public MembershipRoleAssignment {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(roleId, "roleId");
    }
}
