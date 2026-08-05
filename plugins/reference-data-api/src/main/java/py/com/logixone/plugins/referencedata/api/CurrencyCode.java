package py.com.logixone.plugins.referencedata.api;

import java.util.Locale;
import java.util.Objects;

/** ISO 4217 alphabetic identity used by consumers. */
public record CurrencyCode(String value) implements Comparable<CurrencyCode> {

    public CurrencyCode {
        value = Objects.requireNonNull(value, "value").strip().toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("CurrencyCode must be ISO 4217 alphabetic syntax");
        }
    }

    @Override
    public int compareTo(CurrencyCode other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
