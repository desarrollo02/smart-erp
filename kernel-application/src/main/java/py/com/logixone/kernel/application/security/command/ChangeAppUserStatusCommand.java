package py.com.logixone.kernel.application.security.command;

import java.util.Objects;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.domain.security.UserStatus;

public record ChangeAppUserStatusCommand(
        AppUserId userId,
        UserStatus desiredStatus,
        long expectedVersion) {

    public ChangeAppUserStatusCommand {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(desiredStatus, "desiredStatus");
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
    }
}
