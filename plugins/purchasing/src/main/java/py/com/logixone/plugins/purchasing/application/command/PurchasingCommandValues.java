package py.com.logixone.plugins.purchasing.application.command;

import java.util.Locale;
import java.util.Objects;

final class PurchasingCommandValues {
    private PurchasingCommandValues() {
    }

    static String key(String value, String field) {
        value = Objects.requireNonNull(value, field).strip().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || value.length() > 160
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid " + field);
        }
        return value;
    }
}
