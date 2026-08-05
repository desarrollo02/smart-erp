package py.com.logixone.kernel.application.security.admin;

import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;
import py.com.logixone.kernel.domain.security.system.SystemRoleStatus;

public record SystemRoleAdministrationView(
        SystemRoleId roleId,
        String code,
        String displayName,
        SystemRoleStatus status,
        long version,
        List<AppUserId> assignedUserIds,
        List<SystemPermission> grantedPermissions) {

    public SystemRoleAdministrationView {
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(status, "status");
        assignedUserIds = List.copyOf(Objects.requireNonNull(assignedUserIds, "assignedUserIds"));
        grantedPermissions = List.copyOf(Objects.requireNonNull(grantedPermissions, "grantedPermissions"));
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
