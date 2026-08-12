package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestLineId;
import py.com.logixone.plugins.purchasing.domain.CurrencySnapshot;
import py.com.logixone.plugins.purchasing.domain.PurchaseRequest;

@Entity
@Table(name = "purchase_request_line", schema = PurchasingPersistenceNames.SCHEMA)
@IdClass(PurchaseRequestLineEntity.Key.class)
public class PurchaseRequestLineEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "purchase_request_id", nullable = false, updatable = false)
    private UUID purchaseRequestId;
    @Id @Column(name = "purchase_request_line_id", nullable = false, updatable = false)
    private UUID purchaseRequestLineId;
    @Column(name = "line_position", nullable = false, updatable = false)
    private int position;
    @Embedded
    private PurchasedItemEmbeddable item;
    @Column(name = "requested_quantity", nullable = false, precision = 30, scale = 6)
    private BigDecimal quantity;
    @Column(name = "expected_unit_price", precision = 30, scale = 6)
    private BigDecimal expectedUnitPrice;
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "code", column = @Column(name = "expected_currency_code", length = 3)),
        @AttributeOverride(name = "minorUnit", column = @Column(name = "expected_currency_minor_unit")),
        @AttributeOverride(name = "displayName", column = @Column(name = "expected_currency_name", length = 160)),
        @AttributeOverride(name = "releaseId", column = @Column(name = "expected_currency_release_id", length = 64))
    })
    private CurrencyEmbeddable expectedCurrency;

    protected PurchaseRequestLineEntity() {
    }

    static PurchaseRequestLineEntity from(
            UUID companyId, UUID requestId, int position, PurchaseRequest.Line line) {
        PurchaseRequestLineEntity entity = new PurchaseRequestLineEntity();
        entity.companyId = companyId;
        entity.purchaseRequestId = requestId;
        entity.purchaseRequestLineId = line.id().value();
        entity.position = position;
        entity.item = PurchasedItemEmbeddable.from(line.item());
        entity.quantity = line.quantity();
        line.expectedPrice().ifPresent(price -> {
            entity.expectedUnitPrice = price.amount();
            entity.expectedCurrency = CurrencyEmbeddable.from(price.currency());
        });
        return entity;
    }

    PurchaseRequest.Line snapshot() {
        Optional<PurchaseRequest.ExpectedPrice> expectedPrice = expectedUnitPrice == null
                ? Optional.empty()
                : Optional.of(new PurchaseRequest.ExpectedPrice(
                        expectedUnitPrice, expectedCurrency.snapshot()));
        return new PurchaseRequest.Line(
                new PurchaseRequestLineId(purchaseRequestLineId), item.snapshot(), quantity, expectedPrice);
    }

    UUID id() { return purchaseRequestLineId; }
    int position() { return position; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID purchaseRequestId;
        public UUID purchaseRequestLineId;
        public Key() { }
        Key(UUID companyId, UUID purchaseRequestId, UUID purchaseRequestLineId) { this.companyId = companyId; this.purchaseRequestId = purchaseRequestId; this.purchaseRequestLineId = purchaseRequestLineId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(purchaseRequestId, that.purchaseRequestId) && Objects.equals(purchaseRequestLineId, that.purchaseRequestLineId); }
        @Override public int hashCode() { return Objects.hash(companyId, purchaseRequestId, purchaseRequestLineId); }
    }
}
