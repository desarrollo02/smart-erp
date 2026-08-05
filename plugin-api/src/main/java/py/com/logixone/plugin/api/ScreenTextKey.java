package py.com.logixone.plugin.api;

import java.util.Objects;
import java.util.regex.Pattern;

/** Framework-neutral resource key; expressions and implementation paths are deliberately excluded. */
public record ScreenTextKey(String value) {

    private static final int MAX_LENGTH = 128;
    private static final Pattern VALID_VALUE =
            Pattern.compile("[a-z][a-z0-9_]*(?:[.][a-z][a-z0-9_]*)*");

    public ScreenTextKey {
        Objects.requireNonNull(value, "value");
        if (value.length() > MAX_LENGTH || !VALID_VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid screen text key: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
