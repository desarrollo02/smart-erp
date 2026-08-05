package py.com.logixone.plugins.commercialcatalog.domain;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/** Canonical typed value; no Cartesian variant generation is implied. */
public record VariantAttributeValue(VariantValueType type, String value) {
    public VariantAttributeValue {
        Objects.requireNonNull(type, "type");
        value = DomainValues.text(value, "variant attribute value", 100);
        value = switch (type) {
            case TEXT -> value;
            case NUMBER -> new BigDecimal(value).stripTrailingZeros().toPlainString();
            case BOOLEAN -> normalizeBoolean(value);
        };
    }

    private static String normalizeBoolean(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!normalized.equals("true") && !normalized.equals("false")) {
            throw new IllegalArgumentException("Boolean variant value must be true or false");
        }
        return normalized;
    }
}
