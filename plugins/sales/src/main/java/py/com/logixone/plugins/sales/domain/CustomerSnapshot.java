package py.com.logixone.plugins.sales.domain;

import java.util.Objects;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;

public record CustomerSnapshot(BusinessPartnerId id, String code, String displayName, String taxId, long sourceVersion) {
    public CustomerSnapshot {
        Objects.requireNonNull(id, "id"); code = SalesValues.text(code, "customer code", 64);
        displayName = SalesValues.text(displayName, "customer name", 200); taxId = SalesValues.text(taxId, "tax id", 64);
        if (sourceVersion < 0) throw new IllegalArgumentException("Invalid sourceVersion");
    }
}
