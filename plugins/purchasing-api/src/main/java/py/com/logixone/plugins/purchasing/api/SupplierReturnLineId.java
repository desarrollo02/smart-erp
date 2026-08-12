package py.com.logixone.plugins.purchasing.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of a supplier return line. */
public record SupplierReturnLineId(UUID value) implements Comparable<SupplierReturnLineId> {
    public SupplierReturnLineId { Objects.requireNonNull(value, "value"); }
    public static SupplierReturnLineId parse(String value) {
        return new SupplierReturnLineId(ContractValues.uuid(value, "supplierReturnLineId"));
    }
    @Override public int compareTo(SupplierReturnLineId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }
    @Override public String toString() { return value.toString(); }
}
