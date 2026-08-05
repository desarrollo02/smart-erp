package py.com.logixone.kernel.application.security.system.command;

import java.util.Objects;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;

public record UnassignSystemRoleCommand(AppUserId userId, SystemRoleId roleId) {

    public UnassignSystemRoleCommand {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(roleId, "roleId");
    }
}
