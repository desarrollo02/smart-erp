package py.com.logixone.plugin.api;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable plugin identity and unambiguous PostgreSQL schema suffix. */
public record PluginId(String value) implements Comparable<PluginId> {

    private static final int MAX_LENGTH = 59;
    private static final Pattern VALID_VALUE = Pattern.compile("[a-z][a-z0-9]*(?:_[a-z0-9]+)*");

    public PluginId {
        Objects.requireNonNull(value, "value");
        if (value.length() > MAX_LENGTH || !VALID_VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Plugin id must be lower-case snake_case and at most " + MAX_LENGTH + " characters: " + value);
        }
    }

    public String schemaName() {
        return "plg_" + value;
    }

    @Override
    public int compareTo(PluginId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
