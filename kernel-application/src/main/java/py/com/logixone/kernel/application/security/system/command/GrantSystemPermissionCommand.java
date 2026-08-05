package py.com.logixone.kernel.application.security.system.command;

import java.util.Objects;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;

public record GrantSystemPermissionCommand(
        SystemRoleId roleId,
        SystemPermission permission) {

    public GrantSystemPermissionCommand {
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(permission, "permission");
    }
}
