package py.com.logixone.plugins.purchasing.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of a purchase order line. */
public record PurchaseOrderLineId(UUID value) implements Comparable<PurchaseOrderLineId> {
    public PurchaseOrderLineId { Objects.requireNonNull(value, "value"); }
    public static PurchaseOrderLineId parse(String value) {
        return new PurchaseOrderLineId(ContractValues.uuid(value, "purchaseOrderLineId"));
    }
    @Override public int compareTo(PurchaseOrderLineId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }
    @Override public String toString() { return value.toString(); }
}
