package py.com.logixone.plugins.purchasing.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of a purchase request. */
public record PurchaseRequestId(UUID value) implements Comparable<PurchaseRequestId> {
    public PurchaseRequestId { Objects.requireNonNull(value, "value"); }
    public static PurchaseRequestId parse(String value) {
        return new PurchaseRequestId(ContractValues.uuid(value, "purchaseRequestId"));
    }
    @Override public int compareTo(PurchaseRequestId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }
    @Override public String toString() { return value.toString(); }
}
