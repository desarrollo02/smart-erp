package py.com.logixone.kernel.api.security;

import java.util.Objects;
import java.util.UUID;

/** Opaque and immutable application-user identity exposed by neutral kernel contracts. */
public record AppUserId(UUID value) implements Comparable<AppUserId> {

    public AppUserId {
        Objects.requireNonNull(value, "value");
    }

    public static AppUserId parse(String value) {
        Objects.requireNonNull(value, "value");
        UUID parsed;
        try {
            parsed = UUID.fromString(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Application user id must be a canonical UUID", failure);
        }
        if (!parsed.toString().equals(value)) {
            throw new IllegalArgumentException(
                    "Application user id must be a canonical lower-case UUID");
        }
        return new AppUserId(parsed);
    }

    @Override
    public int compareTo(AppUserId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
