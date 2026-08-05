package py.com.logixone.kernel.domain.security.system;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity for a kernel-wide authorization role. */
public record SystemRoleId(UUID value) implements Comparable<SystemRoleId> {

    public SystemRoleId {
        Objects.requireNonNull(value, "value");
    }

    public static SystemRoleId parse(String value) {
        Objects.requireNonNull(value, "value");
        UUID parsed;
        try {
            parsed = UUID.fromString(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("System role id must be a canonical UUID", failure);
        }
        if (!parsed.toString().equals(value)) {
            throw new IllegalArgumentException(
                    "System role id must be a canonical lower-case UUID");
        }
        return new SystemRoleId(parsed);
    }

    @Override
    public int compareTo(SystemRoleId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
