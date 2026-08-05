package py.com.logixone.kernel.domain.security.system;

import java.util.Objects;

/** Versioned kernel-wide role without company ownership. */
public record SystemRole(
        SystemRoleId id,
        SystemRoleCode code,
        String displayName,
        SystemRoleStatus status,
        long version) {

    public SystemRole {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(displayName, "displayName");
        if (displayName.isBlank()
                || displayName.length() > 160
                || !displayName.equals(displayName.strip())
                || displayName.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("displayName must be a valid presentation value");
        }
        Objects.requireNonNull(status, "status");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    public boolean isActive() {
        return status == SystemRoleStatus.ACTIVE;
    }
}
