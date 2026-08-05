package py.com.logixone.kernel.domain.security;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable company-local role code; display labels remain separate and mutable. */
public record RoleCode(String value) implements Comparable<RoleCode> {

    private static final int MAX_LENGTH = 128;
    private static final Pattern VALID_VALUE =
            Pattern.compile("[a-z][a-z0-9_]*(?:[.:][a-z][a-z0-9_]*)*");

    public RoleCode {
        Objects.requireNonNull(value, "value");
        if (value.length() > MAX_LENGTH || !VALID_VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid role code: " + value);
        }
    }

    @Override
    public int compareTo(RoleCode other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
