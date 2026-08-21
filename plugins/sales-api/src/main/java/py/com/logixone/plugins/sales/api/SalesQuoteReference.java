package py.com.logixone.plugins.sales.api;

import java.math.BigDecimal;
import java.util.Objects;

public record SalesQuoteReference(SalesQuoteId id, String number, String customerId,
        SalesQuoteState state, String currencyCode, BigDecimal total, long version) {
    public SalesQuoteReference {
        Objects.requireNonNull(id, "id"); number = ContractValues.code(number, "number", 64);
        customerId = ContractValues.uuid(customerId, "customerId").toString(); Objects.requireNonNull(state, "state");
        currencyCode = ContractValues.code(currencyCode, "currencyCode", 3);
        if (!currencyCode.matches("[A-Z]{3}") || total == null || total.signum() < 0 || version < 0) throw new IllegalArgumentException("Invalid quote reference");
    }
}
