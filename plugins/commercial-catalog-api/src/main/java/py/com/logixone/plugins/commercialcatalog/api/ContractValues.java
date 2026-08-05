package py.com.logixone.plugins.commercialcatalog.api;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

final class ContractValues {

    private ContractValues() {
    }

    static String text(String value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    name + " length must be between 1 and " + maxLength);
        }
        return normalized;
    }

    static String code(String value, String name, int maxLength) {
        return text(value, name, maxLength).toUpperCase(Locale.ROOT);
    }

    static String currency(String value) {
        String normalized = code(value, "currency", 3);
        try {
            Currency.getInstance(normalized);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("currency must be an ISO 4217 code", failure);
        }
        return normalized;
    }

    static BigDecimal positive(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    static BigDecimal nonNegative(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }
}
