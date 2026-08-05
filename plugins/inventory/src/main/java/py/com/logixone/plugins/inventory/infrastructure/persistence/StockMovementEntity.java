package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.plugins.inventory.api.StockMovementType;
import py.com.logixone.plugins.inventory.domain.StockMovementSnapshot;

@Entity
@Table(name = "stock_movement", schema = InventoryPersistenceNames.SCHEMA)
@IdClass(StockMovementEntity.Key.class)
public class StockMovementEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "stock_movement_id", nullable = false, updatable = false)
    private UUID stockMovementId;
    @Enumerated(EnumType.STRING) @Column(name = "movement_type", nullable = false, length = 24, updatable = false)
    private StockMovementType type;
    @Column(name = "reason_code", nullable = false, length = 64, updatable = false)
    private String reasonCode;
    @Column(name = "source_type", nullable = false, length = 64, updatable = false)
    private String sourceType;
    @Column(name = "source_id", nullable = false, length = 160, updatable = false)
    private String sourceId;
    @Column(name = "idempotency_key", nullable = false, length = 160, updatable = false)
    private String idempotencyKey;
    @Column(name = "posted_at", nullable = false, updatable = false)
    private Instant postedAt;
    @Column(name = "reversal_of_movement_id", updatable = false)
    private UUID reversalOfMovementId;

    protected StockMovementEntity() {
    }

    static StockMovementEntity from(StockMovementSnapshot snapshot) {
        StockMovementEntity entity = new StockMovementEntity();
        entity.companyId = snapshot.companyId().value();
        entity.stockMovementId = snapshot.id().value();
        entity.type = snapshot.request().type();
        entity.reasonCode = snapshot.request().reasonCode();
        entity.sourceType = snapshot.request().source().sourceType();
        entity.sourceId = snapshot.request().source().sourceId();
        entity.idempotencyKey = snapshot.request().idempotencyKey();
        entity.postedAt = snapshot.postedAt();
        entity.reversalOfMovementId = snapshot.request().reversalOf().map(value -> value.value()).orElse(null);
        return entity;
    }

    UUID companyId() { return companyId; }
    UUID stockMovementId() { return stockMovementId; }
    StockMovementType type() { return type; }
    String reasonCode() { return reasonCode; }
    String sourceType() { return sourceType; }
    String sourceId() { return sourceId; }
    String idempotencyKey() { return idempotencyKey; }
    Instant postedAt() { return postedAt; }
    UUID reversalOfMovementId() { return reversalOfMovementId; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID stockMovementId;
        public Key() { }
        Key(UUID companyId, UUID stockMovementId) { this.companyId = companyId; this.stockMovementId = stockMovementId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(stockMovementId, that.stockMovementId); }
        @Override public int hashCode() { return Objects.hash(companyId, stockMovementId); }
    }
}
