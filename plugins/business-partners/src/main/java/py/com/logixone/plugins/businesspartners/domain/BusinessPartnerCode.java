package py.com.logixone.plugins.businesspartners.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/** Normalized human-facing code; never used as the technical identity. */
public record BusinessPartnerCode(String value) implements Comparable<BusinessPartnerCode> {

    public static final int MAX_LENGTH = 64;

    public BusinessPartnerCode {
        Objects.requireNonNull(value, "value");
        value = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .toUpperCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Business partner code must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Business partner code must not exceed " + MAX_LENGTH + " characters");
        }
        if (value.codePoints().anyMatch(codePoint ->
                Character.isWhitespace(codePoint) || Character.isISOControl(codePoint))) {
            throw new IllegalArgumentException(
                    "Business partner code must not contain whitespace or control characters");
        }
    }

    @Override
    public int compareTo(BusinessPartnerCode other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
