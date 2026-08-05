package py.com.logixone.plugins.referencedata.api;

import java.util.Objects;

/** Currency or fund value resolved from an immutable release and enterprise policy. */
public record CurrencyReference(
        CurrencyCode code,
        String numericCode,
        int minorUnit,
        String displayName,
        String releaseId,
        boolean enabled) {

    public CurrencyReference {
        Objects.requireNonNull(code, "code");
        numericCode = Objects.requireNonNull(numericCode, "numericCode").strip();
        if (!numericCode.matches("[0-9]{3}")) {
            throw new IllegalArgumentException("Invalid numericCode");
        }
        if (minorUnit < 0 || minorUnit > 9) {
            throw new IllegalArgumentException("minorUnit must be between 0 and 9");
        }
        displayName = text(displayName, "displayName", 160);
        releaseId = text(releaseId, "releaseId", 64);
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
