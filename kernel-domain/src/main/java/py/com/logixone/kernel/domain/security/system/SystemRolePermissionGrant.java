package py.com.logixone.kernel.domain.security.system;

import java.util.Objects;
import py.com.logixone.kernel.api.security.SystemPermission;

/** Historical grant from a kernel-wide role to a system permission. */
public record SystemRolePermissionGrant(
        SystemRoleId roleId,
        SystemPermission permission) {

    public SystemRolePermissionGrant {
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(permission, "permission");
    }
}
