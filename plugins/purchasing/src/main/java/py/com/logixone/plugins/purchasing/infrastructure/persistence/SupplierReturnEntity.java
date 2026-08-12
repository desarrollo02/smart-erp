package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.persistence.Column;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnLineId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnState;
import py.com.logixone.plugins.purchasing.domain.SupplierReturn;

@Entity
@Table(name = "supplier_return", schema = PurchasingPersistenceNames.SCHEMA)
@IdClass(SupplierReturnEntity.Key.class)
public class SupplierReturnEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "supplier_return_id", nullable = false, updatable = false)
    private UUID supplierReturnId;
    @Column(name = "return_number", nullable = false, length = 64, updatable = false)
    private String number;
    @Column(name = "purchase_order_id", nullable = false, updatable = false)
    private UUID purchaseOrderId;
    @Column(name = "return_reason", nullable = false, length = 240, updatable = false)
    private String reason;
    @Enumerated(EnumType.STRING) @Column(name = "return_state", nullable = false, length = 24)
    private SupplierReturnState state;
    @Column(name = "confirmed_by")
    private UUID confirmedBy;
    @Column(name = "confirmed_at")
    private Instant confirmedAt;
    @Version @Column(name = "entity_version", nullable = false)
    private long version;

    protected SupplierReturnEntity() {
    }

    static SupplierReturnEntity from(SupplierReturn.Snapshot snapshot) {
        SupplierReturnEntity entity = new SupplierReturnEntity();
        entity.companyId = snapshot.companyId().value();
        entity.supplierReturnId = snapshot.id().value();
        entity.number = snapshot.number();
        entity.purchaseOrderId = snapshot.orderId().value();
        entity.reason = snapshot.reason();
        entity.version = snapshot.version();
        entity.apply(snapshot);
        return entity;
    }

    void apply(SupplierReturn.Snapshot snapshot) {
        state = snapshot.state();
        confirmedBy = snapshot.confirmedBy().map(AppUserId::value).orElse(null);
        confirmedAt = snapshot.confirmedAt().orElse(null);
    }

    SupplierReturn.Snapshot snapshot(
            List<SupplierReturn.Line> lines,
            Map<SupplierReturnLineId, StockMovementId> movements) {
        return new SupplierReturn.Snapshot(
                new CompanyId(companyId), new SupplierReturnId(supplierReturnId), number,
                new PurchaseOrderId(purchaseOrderId), reason, lines, state,
                Optional.ofNullable(confirmedBy).map(AppUserId::new),
                Optional.ofNullable(confirmedAt), movements, version);
    }

    UUID companyId() { return companyId; }
    UUID id() { return supplierReturnId; }
    long version() { return version; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID supplierReturnId;
        public Key() { }
        Key(UUID companyId, UUID supplierReturnId) { this.companyId = companyId; this.supplierReturnId = supplierReturnId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(supplierReturnId, that.supplierReturnId); }
        @Override public int hashCode() { return Objects.hash(companyId, supplierReturnId); }
    }
}
