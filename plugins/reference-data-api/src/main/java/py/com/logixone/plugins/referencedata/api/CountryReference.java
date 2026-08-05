package py.com.logixone.plugins.referencedata.api;

import java.util.Objects;

/** Country value resolved from an immutable release and an enterprise policy. */
public record CountryReference(
        CountryCode code,
        String alpha3Code,
        String numericCode,
        String displayName,
        String releaseId,
        boolean enabled) {

    public CountryReference {
        Objects.requireNonNull(code, "code");
        alpha3Code = code(alpha3Code, "alpha3Code", "[A-Z]{3}");
        numericCode = code(numericCode, "numericCode", "[0-9]{3}");
        displayName = text(displayName, "displayName", 160);
        releaseId = text(releaseId, "releaseId", 64);
    }

    private static String code(String value, String field, String pattern) {
        value = Objects.requireNonNull(value, field).strip().toUpperCase(java.util.Locale.ROOT);
        if (!value.matches(pattern)) {
            throw new IllegalArgumentException("Invalid " + field);
        }
        return value;
    }

    private static String text(String value, String field, int maximumLength) {
        value = Objects.requireNonNull(value, field).strip();
        if (value.isEmpty() || value.length() > maximumLength
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid " + field);
        }
        return value;
    }
}
