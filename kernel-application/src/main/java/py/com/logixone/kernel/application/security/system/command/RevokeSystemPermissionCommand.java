package py.com.logixone.kernel.application.security.system.command;

import java.util.Objects;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;

public record RevokeSystemPermissionCommand(
        SystemRoleId roleId,
        SystemPermission permission) {

    public RevokeSystemPermissionCommand {
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(permission, "permission");
    }
}
