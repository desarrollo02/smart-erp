package py.com.logixone.plugins.inventory.domain;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

final class InventoryValues {
    static final int QUANTITY_SCALE = 6;
    static final int FACTOR_SCALE = 12;

    private InventoryValues() {
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

    static BigDecimal quantity(BigDecimal value, String name, boolean positive) {
        Objects.requireNonNull(value, name);
        if ((positive ? value.signum() <= 0 : value.signum() < 0)
                || value.scale() > QUANTITY_SCALE) {
            throw new IllegalArgumentException(name + " has invalid sign or more than 6 decimal places");
        }
        return value.stripTrailingZeros();
    }

    static BigDecimal signedQuantity(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.scale() > QUANTITY_SCALE) {
            throw new IllegalArgumentException(name + " has more than 6 decimal places");
        }
        return value.stripTrailingZeros();
    }
}
