package py.com.logixone.kernel.domain.security;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.security.AppUserId;

/** Local application user linked to exactly one external OIDC identity. */
public record AppUser(
        AppUserId id,
        ExternalIdentity externalIdentity,
        Optional<String> displayName,
        UserStatus status,
        long version) {

    public AppUser {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(externalIdentity, "externalIdentity");
        displayName = validateDisplayName(displayName);
        Objects.requireNonNull(status, "status");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    private static Optional<String> validateDisplayName(Optional<String> value) {
        Objects.requireNonNull(value, "displayName");
        value.ifPresent(displayName -> {
            if (displayName.isBlank()
                    || displayName.length() > 160
                    || !displayName.equals(displayName.strip())
                    || displayName.codePoints().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("displayName must be a valid presentation value");
            }
        });
        return value;
    }
}
