package py.com.logixone.plugins.inventory.api;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

final class ContractValues {
    static final int QUANTITY_SCALE = 6;
    static final int FACTOR_SCALE = 12;

    private ContractValues() {
    }

    static UUID canonicalUuid(String value, String name) {
        Objects.requireNonNull(value, name);
        UUID parsed;
        try {
            parsed = UUID.fromString(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(name + " must be a canonical UUID", failure);
        }
        if (!parsed.toString().equals(value)) {
            throw new IllegalArgumentException(name + " must be a canonical lower-case UUID");
        }
        return parsed;
    }

    static String text(String value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(name + " length must be between 1 and " + maxLength);
        }
        return normalized;
    }

    static String code(String value, String name, int maxLength) {
        return text(value, name, maxLength).toUpperCase(Locale.ROOT);
    }

    static String key(String value, String name, int maxLength) {
        return text(value, name, maxLength).toLowerCase(Locale.ROOT);
    }

    static BigDecimal nonNegativeQuantity(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.signum() < 0 || value.scale() > QUANTITY_SCALE) {
            throw new IllegalArgumentException(name + " must be non-negative with at most 6 decimal places");
        }
        return value.stripTrailingZeros();
    }

    static BigDecimal positiveQuantity(BigDecimal value, String name) {
        BigDecimal normalized = nonNegativeQuantity(value, name);
        if (normalized.signum() == 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return normalized;
    }

    static BigDecimal positiveFactor(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.signum() <= 0 || value.scale() > FACTOR_SCALE) {
            throw new IllegalArgumentException(name + " must be positive with at most 12 decimal places");
        }
        return value.stripTrailingZeros();
    }
}
