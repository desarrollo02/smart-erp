package py.com.logixone.plugins.referencedata.api;

import java.util.Locale;
import java.util.Objects;

/** ISO 3166-1 alpha-2 identity used by consumers. */
public record CountryCode(String value) implements Comparable<CountryCode> {

    public CountryCode {
        value = Objects.requireNonNull(value, "value").strip().toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("CountryCode must be ISO alpha-2 syntax");
        }
    }

    @Override
    public int compareTo(CountryCode other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
