package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestLineId;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;

@Entity
@Table(name = "purchase_order_allocation", schema = PurchasingPersistenceNames.SCHEMA)
@IdClass(PurchaseOrderAllocationEntity.Key.class)
public class PurchaseOrderAllocationEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "purchase_order_id", nullable = false, updatable = false)
    private UUID purchaseOrderId;
    @Id @Column(name = "purchase_order_line_id", nullable = false, updatable = false)
    private UUID purchaseOrderLineId;
    @Id @Column(name = "purchase_request_id", nullable = false, updatable = false)
    private UUID purchaseRequestId;
    @Id @Column(name = "purchase_request_line_id", nullable = false, updatable = false)
    private UUID purchaseRequestLineId;
    @Column(name = "allocation_position", nullable = false, updatable = false)
    private int position;
    @Column(name = "allocated_quantity", nullable = false, precision = 30, scale = 6, updatable = false)
    private BigDecimal quantity;

    protected PurchaseOrderAllocationEntity() {
    }

    static PurchaseOrderAllocationEntity from(
            UUID companyId, UUID orderId, UUID orderLineId, int position,
            PurchaseOrder.Allocation allocation) {
        PurchaseOrderAllocationEntity entity = new PurchaseOrderAllocationEntity();
        entity.companyId = companyId;
        entity.purchaseOrderId = orderId;
        entity.purchaseOrderLineId = orderLineId;
        entity.purchaseRequestId = allocation.requestId().value();
        entity.purchaseRequestLineId = allocation.requestLineId().value();
        entity.position = position;
        entity.quantity = allocation.quantity();
        return entity;
    }

    PurchaseOrder.Allocation snapshot() {
        return new PurchaseOrder.Allocation(
                new PurchaseRequestId(purchaseRequestId),
                new PurchaseRequestLineId(purchaseRequestLineId), quantity);
    }

    int position() { return position; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID purchaseOrderId;
        public UUID purchaseOrderLineId;
        public UUID purchaseRequestId;
        public UUID purchaseRequestLineId;
        public Key() { }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(purchaseOrderId, that.purchaseOrderId) && Objects.equals(purchaseOrderLineId, that.purchaseOrderLineId) && Objects.equals(purchaseRequestId, that.purchaseRequestId) && Objects.equals(purchaseRequestLineId, that.purchaseRequestLineId); }
        @Override public int hashCode() { return Objects.hash(companyId, purchaseOrderId, purchaseOrderLineId, purchaseRequestId, purchaseRequestLineId); }
    }
}
