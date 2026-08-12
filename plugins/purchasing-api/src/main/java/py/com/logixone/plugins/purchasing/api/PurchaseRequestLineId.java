package py.com.logixone.plugins.purchasing.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of a purchase request line. */
public record PurchaseRequestLineId(UUID value) implements Comparable<PurchaseRequestLineId> {
    public PurchaseRequestLineId { Objects.requireNonNull(value, "value"); }
    public static PurchaseRequestLineId parse(String value) {
        return new PurchaseRequestLineId(ContractValues.uuid(value, "purchaseRequestLineId"));
    }
    @Override public int compareTo(PurchaseRequestLineId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }
    @Override public String toString() { return value.toString(); }
}
