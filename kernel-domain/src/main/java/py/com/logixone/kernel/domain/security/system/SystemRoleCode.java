package py.com.logixone.kernel.domain.security.system;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable kernel-wide role code; display labels remain separate and mutable. */
public record SystemRoleCode(String value) implements Comparable<SystemRoleCode> {

    private static final int MAX_LENGTH = 128;
    private static final Pattern VALID_VALUE =
            Pattern.compile("[a-z][a-z0-9_]*(?:[.:][a-z][a-z0-9_]*)*");

    public SystemRoleCode {
        Objects.requireNonNull(value, "value");
        if (value.length() > MAX_LENGTH || !VALID_VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid system role code: " + value);
        }
    }

    @Override
    public int compareTo(SystemRoleCode other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
