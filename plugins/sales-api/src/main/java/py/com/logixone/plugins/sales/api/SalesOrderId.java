package py.com.logixone.plugins.sales.api;

import java.util.Objects;
import java.util.UUID;

public record SalesOrderId(UUID value) {
    public SalesOrderId { Objects.requireNonNull(value, "value"); }
    public static SalesOrderId parse(String value) { return new SalesOrderId(ContractValues.uuid(value, "salesOrderId")); }
    @Override public String toString() { return value.toString(); }
}
