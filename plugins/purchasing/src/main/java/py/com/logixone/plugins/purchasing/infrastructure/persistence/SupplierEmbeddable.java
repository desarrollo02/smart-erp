package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.purchasing.domain.SupplierSnapshot;

@Embeddable
public class SupplierEmbeddable {
    @Column(name = "supplier_id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "supplier_code_snapshot", nullable = false, length = 64)
    private String code;
    @Column(name = "supplier_name_snapshot", nullable = false, length = 200)
    private String displayName;
    @Column(name = "supplier_source_version", nullable = false)
    private long sourceVersion;

    protected SupplierEmbeddable() {
    }

    static SupplierEmbeddable from(SupplierSnapshot snapshot) {
        SupplierEmbeddable value = new SupplierEmbeddable();
        value.id = snapshot.id().value();
        value.code = snapshot.code();
        value.displayName = snapshot.displayName();
        value.sourceVersion = snapshot.sourceVersion();
        return value;
    }

    SupplierSnapshot snapshot() {
        return new SupplierSnapshot(new BusinessPartnerId(id), code, displayName, sourceVersion);
    }
}
