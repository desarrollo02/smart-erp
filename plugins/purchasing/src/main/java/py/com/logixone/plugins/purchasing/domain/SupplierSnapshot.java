package py.com.logixone.plugins.purchasing.domain;

import java.util.Objects;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;

/** Historical supplier data retained by an order. */
public record SupplierSnapshot(
        BusinessPartnerId id,
        String code,
        String displayName,
        long sourceVersion) {

    public SupplierSnapshot {
        Objects.requireNonNull(id, "id");
        code = PurchasingValues.code(code, "supplierCode", 64);
        displayName = PurchasingValues.text(displayName, "supplierDisplayName", 200);
        if (sourceVersion < 0) {
            throw new IllegalArgumentException("sourceVersion must not be negative");
        }
    }
}
