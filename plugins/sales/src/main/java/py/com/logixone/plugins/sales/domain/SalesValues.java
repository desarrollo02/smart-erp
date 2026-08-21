package py.com.logixone.plugins.sales.domain;

import java.math.BigDecimal;

final class SalesValues {
    private SalesValues() { }
    static String text(String value, String name, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum) throw new IllegalArgumentException("Invalid " + name);
        return value;
    }
    static BigDecimal quantity(BigDecimal value) {
        if (value == null || value.signum() <= 0 || Math.max(value.scale(), 0) > 6) throw new IllegalArgumentException("Invalid quantity");
        return value.stripTrailingZeros();
    }
    static BigDecimal amount(BigDecimal value) {
        if (value == null || value.signum() < 0 || Math.max(value.scale(), 0) > 6) throw new IllegalArgumentException("Invalid amount");
        return value.stripTrailingZeros();
    }
}
