package py.com.logixone.plugins.purchasing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

final class PurchasingValues {
    private PurchasingValues() {
    }

    static String text(String value, String field, int maximumLength) {
        value = Objects.requireNonNull(value, field).strip();
        if (value.isEmpty() || value.length() > maximumLength
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid " + field);
        }
        return value;
    }

    static String code(String value, String field, int maximumLength) {
        value = text(value, field, maximumLength).toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z0-9][A-Z0-9._/-]*")) {
            throw new IllegalArgumentException("Invalid " + field);
        }
        return value;
    }

    static BigDecimal quantity(BigDecimal value, String field) {
        value = Objects.requireNonNull(value, field).stripTrailingZeros();
        if (value.signum() <= 0 || Math.max(value.scale(), 0) > 6) {
            throw new IllegalArgumentException(field + " must be positive with at most 6 decimals");
        }
        return value;
    }

    static BigDecimal amount(BigDecimal value, String field) {
        value = Objects.requireNonNull(value, field).stripTrailingZeros();
        if (value.signum() < 0 || Math.max(value.scale(), 0) > 6) {
            throw new IllegalArgumentException(field + " must be non-negative with at most 6 decimals");
        }
        return value;
    }

    static BigDecimal total(BigDecimal quantity, BigDecimal unitPrice, int minorUnit) {
        return quantity.multiply(unitPrice).setScale(minorUnit, RoundingMode.HALF_UP);
    }
}
