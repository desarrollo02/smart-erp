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
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptLineId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptState;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.domain.GoodsReceipt;

@Entity
@Table(name = "goods_receipt", schema = PurchasingPersistenceNames.SCHEMA)
@IdClass(GoodsReceiptEntity.Key.class)
public class GoodsReceiptEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "goods_receipt_id", nullable = false, updatable = false)
    private UUID goodsReceiptId;
    @Column(name = "receipt_number", nullable = false, length = 64, updatable = false)
    private String number;
    @Column(name = "purchase_order_id", nullable = false, updatable = false)
    private UUID purchaseOrderId;
    @Enumerated(EnumType.STRING) @Column(name = "receipt_state", nullable = false, length = 24)
    private GoodsReceiptState state;
    @Column(name = "confirmed_by")
    private UUID confirmedBy;
    @Column(name = "confirmed_at")
    private Instant confirmedAt;
    @Version @Column(name = "entity_version", nullable = false)
    private long version;

    protected GoodsReceiptEntity() {
    }

    static GoodsReceiptEntity from(GoodsReceipt.Snapshot snapshot) {
        GoodsReceiptEntity entity = new GoodsReceiptEntity();
        entity.companyId = snapshot.companyId().value();
        entity.goodsReceiptId = snapshot.id().value();
        entity.number = snapshot.number();
        entity.purchaseOrderId = snapshot.orderId().value();
        entity.version = snapshot.version();
        entity.apply(snapshot);
        return entity;
    }

    void apply(GoodsReceipt.Snapshot snapshot) {
        state = snapshot.state();
        confirmedBy = snapshot.confirmedBy().map(AppUserId::value).orElse(null);
        confirmedAt = snapshot.confirmedAt().orElse(null);
    }

    GoodsReceipt.Snapshot snapshot(List<GoodsReceipt.Line> lines, Map<GoodsReceiptLineId, StockMovementId> movements) {
        return new GoodsReceipt.Snapshot(
                new CompanyId(companyId), new GoodsReceiptId(goodsReceiptId), number,
                new PurchaseOrderId(purchaseOrderId), lines, state,
                Optional.ofNullable(confirmedBy).map(AppUserId::new),
                Optional.ofNullable(confirmedAt), movements, version);
    }

    UUID companyId() { return companyId; }
    UUID id() { return goodsReceiptId; }
    long version() { return version; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID goodsReceiptId;
        public Key() { }
        Key(UUID companyId, UUID goodsReceiptId) { this.companyId = companyId; this.goodsReceiptId = goodsReceiptId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(goodsReceiptId, that.goodsReceiptId); }
        @Override public int hashCode() { return Objects.hash(companyId, goodsReceiptId); }
    }
}
