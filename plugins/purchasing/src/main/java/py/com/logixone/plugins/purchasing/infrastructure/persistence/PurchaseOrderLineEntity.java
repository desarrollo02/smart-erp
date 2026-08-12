package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderLineId;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;

@Entity
@Table(name = "purchase_order_line", schema = PurchasingPersistenceNames.SCHEMA)
@IdClass(PurchaseOrderLineEntity.Key.class)
public class PurchaseOrderLineEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "purchase_order_id", nullable = false, updatable = false)
    private UUID purchaseOrderId;
    @Id @Column(name = "purchase_order_line_id", nullable = false, updatable = false)
    private UUID purchaseOrderLineId;
    @Column(name = "line_position", nullable = false, updatable = false)
    private int position;
    @Embedded
    private PurchasedItemEmbeddable item;
    @Column(name = "ordered_quantity", nullable = false, precision = 30, scale = 6, updatable = false)
    private BigDecimal orderedQuantity;
    @Column(name = "unit_price", nullable = false, precision = 30, scale = 6, updatable = false)
    private BigDecimal unitPrice;
    @Column(name = "received_quantity", nullable = false, precision = 30, scale = 6)
    private BigDecimal receivedQuantity;
    @Column(name = "returned_quantity", nullable = false, precision = 30, scale = 6)
    private BigDecimal returnedQuantity;
    @Column(name = "short_closed_quantity", nullable = false, precision = 30, scale = 6)
    private BigDecimal shortClosedQuantity;

    protected PurchaseOrderLineEntity() {
    }

    static PurchaseOrderLineEntity from(
            UUID companyId, UUID orderId, int position, PurchaseOrder.LineSnapshot line) {
        PurchaseOrderLineEntity entity = new PurchaseOrderLineEntity();
        entity.companyId = companyId;
        entity.purchaseOrderId = orderId;
        entity.purchaseOrderLineId = line.id().value();
        entity.position = position;
        entity.item = PurchasedItemEmbeddable.from(line.item());
        entity.orderedQuantity = line.orderedQuantity();
        entity.unitPrice = line.unitPrice();
        entity.apply(line);
        return entity;
    }

    void apply(PurchaseOrder.LineSnapshot line) {
        receivedQuantity = line.receivedQuantity();
        returnedQuantity = line.returnedQuantity();
        shortClosedQuantity = line.shortClosedQuantity();
    }

    PurchaseOrder.LineSnapshot snapshot(List<PurchaseOrder.Allocation> allocations) {
        BigDecimal pending = orderedQuantity.subtract(receivedQuantity)
                .add(returnedQuantity).subtract(shortClosedQuantity);
        return new PurchaseOrder.LineSnapshot(
                new PurchaseOrderLineId(purchaseOrderLineId), item.snapshot(), orderedQuantity,
                unitPrice, allocations, receivedQuantity, returnedQuantity,
                shortClosedQuantity, pending);
    }

    UUID id() { return purchaseOrderLineId; }
    int position() { return position; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID purchaseOrderId;
        public UUID purchaseOrderLineId;
        public Key() { }
        Key(UUID companyId, UUID purchaseOrderId, UUID purchaseOrderLineId) { this.companyId = companyId; this.purchaseOrderId = purchaseOrderId; this.purchaseOrderLineId = purchaseOrderLineId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(purchaseOrderId, that.purchaseOrderId) && Objects.equals(purchaseOrderLineId, that.purchaseOrderLineId); }
        @Override public int hashCode() { return Objects.hash(companyId, purchaseOrderId, purchaseOrderLineId); }
    }
}
