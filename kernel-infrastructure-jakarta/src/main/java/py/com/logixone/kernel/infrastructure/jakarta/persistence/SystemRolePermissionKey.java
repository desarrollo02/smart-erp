package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;

@Embeddable
public class SystemRolePermissionKey implements Serializable {

    @Column(name = "system_role_id", nullable = false, updatable = false)
    private UUID systemRoleId;

    @Column(name = "permission_id", nullable = false, length = 128, updatable = false)
    private String permissionId;

    protected SystemRolePermissionKey() {
    }

    SystemRolePermissionKey(SystemRoleId roleId, SystemPermission permission) {
        systemRoleId = Objects.requireNonNull(roleId, "roleId").value();
        permissionId = Objects.requireNonNull(permission, "permission").value();
    }

    SystemRoleId roleId() {
        return new SystemRoleId(systemRoleId);
    }

    SystemPermission permission() {
        return new SystemPermission(permissionId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof SystemRolePermissionKey that
                && systemRoleId.equals(that.systemRoleId)
                && permissionId.equals(that.permissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(systemRoleId, permissionId);
    }
}
