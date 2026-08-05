package py.com.logixone.kernel.application.security.system.command;

import java.util.Objects;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;

public record AssignSystemRoleCommand(AppUserId userId, SystemRoleId roleId) {

    public AssignSystemRoleCommand {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(roleId, "roleId");
    }
}
