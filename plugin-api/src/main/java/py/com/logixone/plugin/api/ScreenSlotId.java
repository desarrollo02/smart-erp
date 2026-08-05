package py.com.logixone.plugin.api;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable local identity of an extension slot inside a public screen contract. */
public record ScreenSlotId(String value) implements Comparable<ScreenSlotId> {

    private static final int MAX_LENGTH = 64;
    private static final Pattern VALID_VALUE = Pattern.compile("[a-z][a-z0-9]*(?:_[a-z0-9]+)*");

    public ScreenSlotId {
        Objects.requireNonNull(value, "value");
        if (value.length() > MAX_LENGTH || !VALID_VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid screen slot id: " + value);
        }
    }

    @Override
    public int compareTo(ScreenSlotId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
