package py.com.logixone.plugins.commercialcatalog.domain;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.Currency;

final class DomainValues {

    private DomainValues() {
    }

    static String text(String value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(name + " length must be between 1 and " + maxLength);
        }
        return normalized;
    }

    static String optionalText(String value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(name + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }

    static String code(String value, String name, int maxLength) {
        return text(value, name, maxLength).toUpperCase(Locale.ROOT);
    }

    static BigDecimal positive(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value.stripTrailingZeros();
    }

    static BigDecimal nonNegative(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value.stripTrailingZeros();
    }

    static String currency(String value) {
        String code = code(value, "currency", 3);
        try {
            Currency.getInstance(code);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("currency must be an ISO 4217 code", failure);
        }
        return code;
    }
}
