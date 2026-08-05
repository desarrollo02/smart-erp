package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.security.RoleId;
import py.com.logixone.plugin.api.ContributionId;

@Embeddable
public class RolePermissionKey implements Serializable {

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID roleId;

    @Column(name = "permission_id", nullable = false, length = 128, updatable = false)
    private String permissionId;

    protected RolePermissionKey() {
    }

    RolePermissionKey(CompanyId companyId, RoleId roleId, ContributionId permissionId) {
        this.companyId = Objects.requireNonNull(companyId, "companyId").value();
        this.roleId = Objects.requireNonNull(roleId, "roleId").value();
        this.permissionId = Objects.requireNonNull(permissionId, "permissionId").value();
    }

    CompanyId companyId() {
        return new CompanyId(companyId);
    }

    RoleId roleId() {
        return new RoleId(roleId);
    }

    ContributionId permissionId() {
        return new ContributionId(permissionId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof RolePermissionKey that
                && companyId.equals(that.companyId)
                && roleId.equals(that.roleId)
                && permissionId.equals(that.permissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, roleId, permissionId);
    }
}
