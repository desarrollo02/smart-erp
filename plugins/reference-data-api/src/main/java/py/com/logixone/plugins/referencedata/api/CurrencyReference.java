package py.com.logixone.plugins.referencedata.api;

import java.util.Objects;
import java.util.OptionalInt;

/** Currency or fund value resolved from an immutable release and enterprise policy. */
public final class CurrencyReference {

    private final CurrencyCode code;
    private final String numericCode;
    private final OptionalInt minorUnit;
    private final String displayName;
    private final String releaseId;
    private final boolean enabled;

    /** Source-compatible constructor for currencies whose minor unit is defined. */
    public CurrencyReference(
            CurrencyCode code,
            String numericCode,
            int minorUnit,
            String displayName,
            String releaseId,
            boolean enabled) {
        this(code, numericCode, OptionalInt.of(minorUnit), displayName, releaseId, enabled);
    }

    /** Constructor that preserves an official not-applicable minor unit as absence. */
    public CurrencyReference(
            CurrencyCode code,
            String numericCode,
            OptionalInt minorUnit,
            String displayName,
            String releaseId,
            boolean enabled) {
        this.code = Objects.requireNonNull(code, "code");
        this.numericCode = Objects.requireNonNull(numericCode, "numericCode").strip();
        if (!this.numericCode.matches("[0-9]{3}")) {
            throw new IllegalArgumentException("Invalid numericCode");
        }
        this.minorUnit = Objects.requireNonNull(minorUnit, "minorUnit");
        minorUnit.ifPresent(value -> {
            if (value < 0 || value > 9) {
                throw new IllegalArgumentException("minorUnit must be between 0 and 9");
            }
        });
        this.displayName = text(displayName, "displayName", 160);
        this.releaseId = text(releaseId, "releaseId", 64);
        this.enabled = enabled;
    }

    public CurrencyCode code() {
        return code;
    }

    public String numericCode() {
        return numericCode;
    }

    /**
     * Existing access for currencies with a defined minor unit.
     * Use {@link #minorUnitIfDefined()} when the official value may be N.A.
     */
    public int minorUnit() {
        return minorUnit.orElseThrow(
                () -> new IllegalStateException("Minor unit is not applicable"));
    }

    public OptionalInt minorUnitIfDefined() {
        return minorUnit;
    }

    public String displayName() {
        return displayName;
    }

    public String releaseId() {
        return releaseId;
    }

    public boolean enabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof CurrencyReference that
                && enabled == that.enabled
                && code.equals(that.code)
                && numericCode.equals(that.numericCode)
                && minorUnit.equals(that.minorUnit)
                && displayName.equals(that.displayName)
                && releaseId.equals(that.releaseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, numericCode, minorUnit, displayName, releaseId, enabled);
    }

    @Override
    public String toString() {
        return "CurrencyReference[code=" + code
                + ", numericCode=" + numericCode
                + ", minorUnit=" + (minorUnit.isPresent() ? minorUnit.getAsInt() : "N.A.")
                + ", displayName=" + displayName
                + ", releaseId=" + releaseId
                + ", enabled=" + enabled + "]";
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
