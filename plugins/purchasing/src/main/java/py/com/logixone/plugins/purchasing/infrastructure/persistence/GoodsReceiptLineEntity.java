package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderLineId;
import py.com.logixone.plugins.purchasing.domain.GoodsReceipt;

@Entity
@Table(name = "goods_receipt_line", schema = PurchasingPersistenceNames.SCHEMA)
@IdClass(GoodsReceiptLineEntity.Key.class)
public class GoodsReceiptLineEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "goods_receipt_id", nullable = false, updatable = false)
    private UUID goodsReceiptId;
    @Id @Column(name = "goods_receipt_line_id", nullable = false, updatable = false)
    private UUID goodsReceiptLineId;
    @Column(name = "line_position", nullable = false, updatable = false)
    private int position;
    @Column(name = "purchase_order_id", nullable = false, updatable = false)
    private UUID purchaseOrderId;
    @Column(name = "purchase_order_line_id", nullable = false, updatable = false)
    private UUID purchaseOrderLineId;
    @Enumerated(EnumType.STRING) @Column(name = "line_kind", nullable = false, length = 24, updatable = false)
    private PurchaseLineKind kind;
    @Column(name = "received_quantity", nullable = false, precision = 30, scale = 6, updatable = false)
    private BigDecimal quantity;
    @Column(name = "warehouse_id", updatable = false)
    private UUID warehouseId;
    @Column(name = "stock_location_id", updatable = false)
    private UUID locationId;
    @Column(name = "lot_code", length = 80, updatable = false)
    private String lotCode;
    @Column(name = "serial_number", length = 120, updatable = false)
    private String serialNumber;
    @Column(name = "expiry_date", updatable = false)
    private LocalDate expiryDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "stock_condition", length = 24, updatable = false)
    private StockCondition condition;
    @Column(name = "stock_movement_id")
    private UUID stockMovementId;

    protected GoodsReceiptLineEntity() {
    }

    static GoodsReceiptLineEntity from(
            UUID companyId, UUID receiptId, UUID orderId, int position, GoodsReceipt.Line line,
            Optional<StockMovementId> movementId) {
        GoodsReceiptLineEntity entity = new GoodsReceiptLineEntity();
        entity.companyId = companyId;
        entity.goodsReceiptId = receiptId;
        entity.goodsReceiptLineId = line.id().value();
        entity.position = position;
        entity.purchaseOrderId = orderId;
        entity.purchaseOrderLineId = line.orderLineId().value();
        entity.kind = line.kind();
        entity.quantity = line.quantity();
        entity.warehouseId = line.warehouseId().map(WarehouseId::value).orElse(null);
        entity.locationId = line.locationId().map(StockLocationId::value).orElse(null);
        entity.lotCode = line.lotCode().orElse(null);
        entity.serialNumber = line.serialNumber().orElse(null);
        entity.expiryDate = line.expiryDate().orElse(null);
        entity.condition = line.condition().orElse(null);
        entity.stockMovementId = movementId.map(StockMovementId::value).orElse(null);
        return entity;
    }

    void applyMovement(Optional<StockMovementId> movementId) {
        stockMovementId = movementId.map(StockMovementId::value).orElse(null);
    }

    GoodsReceipt.Line snapshot() {
        return new GoodsReceipt.Line(
                new GoodsReceiptLineId(goodsReceiptLineId),
                new PurchaseOrderLineId(purchaseOrderLineId), kind, quantity,
                Optional.ofNullable(warehouseId).map(WarehouseId::new),
                Optional.ofNullable(locationId).map(StockLocationId::new),
                Optional.ofNullable(lotCode), Optional.ofNullable(serialNumber),
                Optional.ofNullable(expiryDate), Optional.ofNullable(condition));
    }

    GoodsReceiptLineId id() { return new GoodsReceiptLineId(goodsReceiptLineId); }
    int position() { return position; }
    Optional<StockMovementId> movementId() {
        return Optional.ofNullable(stockMovementId).map(StockMovementId::new);
    }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID goodsReceiptId;
        public UUID goodsReceiptLineId;
        public Key() { }
        Key(UUID companyId, UUID goodsReceiptId, UUID goodsReceiptLineId) { this.companyId = companyId; this.goodsReceiptId = goodsReceiptId; this.goodsReceiptLineId = goodsReceiptLineId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(goodsReceiptId, that.goodsReceiptId) && Objects.equals(goodsReceiptLineId, that.goodsReceiptLineId); }
        @Override public int hashCode() { return Objects.hash(companyId, goodsReceiptId, goodsReceiptLineId); }
    }
}
