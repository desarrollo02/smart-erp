package py.com.logixone.plugins.purchasing.api;

import java.math.BigDecimal;
import java.util.Objects;

/** Minimal immutable projection of a purchase order. */
public record PurchaseOrderReference(
        PurchaseOrderId id,
        String number,
        String supplierId,
        PurchaseOrderState state,
        String currencyCode,
        BigDecimal orderedTotal,
        long version) {

    public PurchaseOrderReference {
        Objects.requireNonNull(id, "id");
        number = ContractValues.code(number, "number", 64);
        supplierId = ContractValues.uuid(supplierId, "supplierId").toString();
        Objects.requireNonNull(state, "state");
        currencyCode = ContractValues.code(currencyCode, "currencyCode", 3);
        if (!currencyCode.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Invalid currencyCode");
        }
        orderedTotal = ContractValues.amount(orderedTotal, "orderedTotal");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
