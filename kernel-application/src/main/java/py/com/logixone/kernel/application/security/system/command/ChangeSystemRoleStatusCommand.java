package py.com.logixone.kernel.application.security.system.command;

import java.util.Objects;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;
import py.com.logixone.kernel.domain.security.system.SystemRoleStatus;

public record ChangeSystemRoleStatusCommand(
        SystemRoleId roleId,
        SystemRoleStatus desiredStatus,
        long expectedVersion) {

    public ChangeSystemRoleStatusCommand {
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(desiredStatus, "desiredStatus");
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
    }
}
