package py.com.logixone.plugin.api;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable qualified identity of the option source used by one or more selectors. */
public record SelectorSourceId(String value) implements Comparable<SelectorSourceId> {

    private static final int MAX_LENGTH = 128;
    private static final Pattern VALID_VALUE =
            Pattern.compile("[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)*");

    public SelectorSourceId {
        Objects.requireNonNull(value, "value");
        if (value.length() > MAX_LENGTH || !VALID_VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid selector source id: " + value);
        }
    }

    @Override
    public int compareTo(SelectorSourceId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
