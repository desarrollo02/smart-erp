package py.com.logixone.plugins.purchasing.api;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

final class ContractValues {
    private ContractValues() {
    }

    static UUID uuid(String value, String field) {
        value = Objects.requireNonNull(value, field);
        UUID parsed;
        try {
            parsed = UUID.fromString(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(field + " must be a canonical UUID", failure);
        }
        if (!parsed.toString().equals(value)) {
            throw new IllegalArgumentException(field + " must be a canonical lower-case UUID");
        }
        return parsed;
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
        if (value.signum() < 0 || Math.max(value.scale(), 0) > 9) {
            throw new IllegalArgumentException(field + " must be non-negative with at most 9 decimals");
        }
        return value;
    }
}
