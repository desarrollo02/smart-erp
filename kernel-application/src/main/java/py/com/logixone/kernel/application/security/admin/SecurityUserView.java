package py.com.logixone.kernel.application.security.admin;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.UserStatus;

/** Administrative projection that intentionally omits the configured issuer. */
public record SecurityUserView(
        AppUserId userId,
        String subject,
        Optional<String> displayName,
        UserStatus status,
        long version) {

    public SecurityUserView {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(subject, "subject");
        displayName = Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(status, "status");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    public static SecurityUserView from(AppUser user) {
        Objects.requireNonNull(user, "user");
        return new SecurityUserView(
                user.id(), user.externalIdentity().subject(), user.displayName(), user.status(), user.version());
    }
}
