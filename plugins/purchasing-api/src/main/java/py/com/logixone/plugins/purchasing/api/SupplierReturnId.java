package py.com.logixone.plugins.purchasing.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of a supplier return. */
public record SupplierReturnId(UUID value) implements Comparable<SupplierReturnId> {
    public SupplierReturnId { Objects.requireNonNull(value, "value"); }
    public static SupplierReturnId parse(String value) {
        return new SupplierReturnId(ContractValues.uuid(value, "supplierReturnId"));
    }
    @Override public int compareTo(SupplierReturnId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }
    @Override public String toString() { return value.toString(); }
}
