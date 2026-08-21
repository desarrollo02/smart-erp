package py.com.logixone.plugins.sales.api;

import java.util.Objects;
import java.util.UUID;

public record SalesQuoteId(UUID value) {
    public SalesQuoteId { Objects.requireNonNull(value, "value"); }
    public static SalesQuoteId parse(String value) { return new SalesQuoteId(ContractValues.uuid(value, "salesQuoteId")); }
    @Override public String toString() { return value.toString(); }
}
