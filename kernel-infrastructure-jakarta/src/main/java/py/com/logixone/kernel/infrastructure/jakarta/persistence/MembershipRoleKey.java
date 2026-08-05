package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.domain.security.RoleId;

@Embeddable
public class MembershipRoleKey implements Serializable {

    @Column(name = "app_user_id", nullable = false, updatable = false)
    private UUID appUserId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID roleId;

    protected MembershipRoleKey() {
    }

    MembershipRoleKey(AppUserId userId, CompanyId companyId, RoleId roleId) {
        appUserId = Objects.requireNonNull(userId, "userId").value();
        this.companyId = Objects.requireNonNull(companyId, "companyId").value();
        this.roleId = Objects.requireNonNull(roleId, "roleId").value();
    }

    AppUserId userId() {
        return new AppUserId(appUserId);
    }

    CompanyId companyId() {
        return new CompanyId(companyId);
    }

    RoleId roleId() {
        return new RoleId(roleId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof MembershipRoleKey that
                && appUserId.equals(that.appUserId)
                && companyId.equals(that.companyId)
                && roleId.equals(that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appUserId, companyId, roleId);
    }
}
