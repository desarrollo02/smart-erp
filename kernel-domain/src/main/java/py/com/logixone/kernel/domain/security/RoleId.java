package py.com.logixone.kernel.domain.security;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity for a company-owned authorization role. */
public record RoleId(UUID value) implements Comparable<RoleId> {

    public RoleId {
        Objects.requireNonNull(value, "value");
    }

    public static RoleId parse(String value) {
        Objects.requireNonNull(value, "value");
        UUID parsed;
        try {
            parsed = UUID.fromString(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Role id must be a canonical UUID", failure);
        }
        if (!parsed.toString().equals(value)) {
            throw new IllegalArgumentException("Role id must be a canonical lower-case UUID");
        }
        return new RoleId(parsed);
    }

    @Override
    public int compareTo(RoleId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
