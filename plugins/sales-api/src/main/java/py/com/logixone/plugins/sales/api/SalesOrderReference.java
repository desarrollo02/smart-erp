package py.com.logixone.plugins.sales.api;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record SalesOrderReference(SalesOrderId id, String number, String customerId,
        SalesOrderState state, String currencyCode, BigDecimal total,
        Optional<SalesQuoteId> sourceQuoteId, long version) {
    public SalesOrderReference {
        Objects.requireNonNull(id, "id"); number = ContractValues.code(number, "number", 64);
        customerId = ContractValues.uuid(customerId, "customerId").toString(); Objects.requireNonNull(state, "state");
        currencyCode = ContractValues.code(currencyCode, "currencyCode", 3); sourceQuoteId = Objects.requireNonNull(sourceQuoteId, "sourceQuoteId");
        if (!currencyCode.matches("[A-Z]{3}") || total == null || total.signum() < 0 || version < 0) throw new IllegalArgumentException("Invalid order reference");
    }
}
