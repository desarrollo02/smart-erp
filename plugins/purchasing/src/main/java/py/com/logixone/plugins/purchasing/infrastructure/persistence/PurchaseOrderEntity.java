package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;

@Entity
@Table(name = "purchase_order", schema = PurchasingPersistenceNames.SCHEMA)
@IdClass(PurchaseOrderEntity.Key.class)
public class PurchaseOrderEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "purchase_order_id", nullable = false, updatable = false)
    private UUID purchaseOrderId;
    @Column(name = "order_number", nullable = false, length = 64, updatable = false)
    private String number;
    @Embedded
    private SupplierEmbeddable supplier;
    @Embedded
    private CurrencyEmbeddable currency;
    @Column(name = "direct_order_justification", length = 240, updatable = false)
    private String directOrderJustification;
    @Enumerated(EnumType.STRING) @Column(name = "order_state", nullable = false, length = 24)
    private PurchaseOrderState state;
    @Column(name = "issued_by")
    private UUID issuedBy;
    @Column(name = "issued_at")
    private Instant issuedAt;
    @Column(name = "terminal_reason", length = 240)
    private String terminalReason;
    @Version @Column(name = "entity_version", nullable = false)
    private long version;

    protected PurchaseOrderEntity() {
    }

    static PurchaseOrderEntity from(PurchaseOrder.Snapshot snapshot) {
        PurchaseOrderEntity entity = new PurchaseOrderEntity();
        entity.companyId = snapshot.companyId().value();
        entity.purchaseOrderId = snapshot.id().value();
        entity.number = snapshot.number();
        entity.supplier = SupplierEmbeddable.from(snapshot.supplier());
        entity.currency = CurrencyEmbeddable.from(snapshot.currency());
        entity.directOrderJustification = snapshot.directOrderJustification().orElse(null);
        entity.version = snapshot.version();
        entity.apply(snapshot);
        return entity;
    }

    void apply(PurchaseOrder.Snapshot snapshot) {
        state = snapshot.state();
        issuedBy = snapshot.issuedBy().map(AppUserId::value).orElse(null);
        issuedAt = snapshot.issuedAt().orElse(null);
        terminalReason = snapshot.terminalReason().orElse(null);
    }

    PurchaseOrder.Snapshot snapshot(List<PurchaseOrder.LineSnapshot> lines) {
        return new PurchaseOrder.Snapshot(
                new CompanyId(companyId), new PurchaseOrderId(purchaseOrderId), number,
                supplier.snapshot(), currency.snapshot(), lines,
                Optional.ofNullable(directOrderJustification), state,
                Optional.ofNullable(issuedBy).map(AppUserId::new),
                Optional.ofNullable(issuedAt), Optional.ofNullable(terminalReason), version);
    }

    UUID companyId() { return companyId; }
    UUID id() { return purchaseOrderId; }
    long version() { return version; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID purchaseOrderId;
        public Key() { }
        Key(UUID companyId, UUID purchaseOrderId) { this.companyId = companyId; this.purchaseOrderId = purchaseOrderId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(purchaseOrderId, that.purchaseOrderId); }
        @Override public int hashCode() { return Objects.hash(companyId, purchaseOrderId); }
    }
}
