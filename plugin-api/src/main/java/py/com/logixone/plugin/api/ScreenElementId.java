package py.com.logixone.plugin.api;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable local identity of an element inside a public screen contract. */
public record ScreenElementId(String value) implements Comparable<ScreenElementId> {

    private static final int MAX_LENGTH = 64;
    private static final Pattern VALID_VALUE = Pattern.compile("[a-z][a-z0-9]*(?:_[a-z0-9]+)*");

    public ScreenElementId {
        Objects.requireNonNull(value, "value");
        if (value.length() > MAX_LENGTH || !VALID_VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid screen element id: " + value);
        }
    }

    @Override
    public int compareTo(ScreenElementId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
