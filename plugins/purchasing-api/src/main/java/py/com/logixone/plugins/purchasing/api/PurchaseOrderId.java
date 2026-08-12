package py.com.logixone.plugins.purchasing.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of a purchase order. */
public record PurchaseOrderId(UUID value) implements Comparable<PurchaseOrderId> {
    public PurchaseOrderId { Objects.requireNonNull(value, "value"); }
    public static PurchaseOrderId parse(String value) {
        return new PurchaseOrderId(ContractValues.uuid(value, "purchaseOrderId"));
    }
    @Override public int compareTo(PurchaseOrderId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }
    @Override public String toString() { return value.toString(); }
}
