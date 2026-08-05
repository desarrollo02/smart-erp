package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.StockReservationRequest;
import py.com.logixone.plugins.inventory.api.StockReservationState;
import py.com.logixone.plugins.inventory.api.StockSourceReference;
import py.com.logixone.plugins.inventory.domain.StockReservationSnapshot;

@Entity
@Table(name = "stock_reservation", schema = InventoryPersistenceNames.SCHEMA)
@IdClass(StockReservationEntity.Key.class)
public class StockReservationEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "stock_reservation_id", nullable = false, updatable = false)
    private UUID stockReservationId;
    @Column(name = "inventory_item_id", nullable = false, updatable = false)
    private UUID inventoryItemId;
    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private UUID warehouseId;
    @Column(name = "stock_location_id", nullable = false, updatable = false)
    private UUID stockLocationId;
    @Column(name = "lot_code", length = 80, updatable = false)
    private String lotCode;
    @Column(name = "serial_number", length = 120, updatable = false)
    private String serialNumber;
    @Column(name = "expiry_date", updatable = false)
    private LocalDate expiryDate;
    @Enumerated(EnumType.STRING) @Column(name = "condition_code", nullable = false, length = 24, updatable = false)
    private StockCondition condition;
    @Column(name = "original_quantity", nullable = false, precision = 30, scale = 6, updatable = false)
    private BigDecimal originalQuantity;
    @Column(name = "consumed_quantity", nullable = false, precision = 30, scale = 6)
    private BigDecimal consumedQuantity;
    @Column(name = "released_quantity", nullable = false, precision = 30, scale = 6)
    private BigDecimal releasedQuantity;
    @Column(name = "source_type", nullable = false, length = 64, updatable = false)
    private String sourceType;
    @Column(name = "source_id", nullable = false, length = 160, updatable = false)
    private String sourceId;
    @Column(name = "idempotency_key", nullable = false, length = 160, updatable = false)
    private String idempotencyKey;
    @Enumerated(EnumType.STRING) @Column(name = "reservation_state", nullable = false, length = 32)
    private StockReservationState state;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
    @Version @Column(name = "entity_version", nullable = false)
    private long version;

    protected StockReservationEntity() {
    }

    static StockReservationEntity from(StockReservationSnapshot snapshot) {
        StockReservationEntity entity = new StockReservationEntity();
        entity.companyId = snapshot.companyId().value();
        entity.stockReservationId = snapshot.id().value();
        entity.inventoryItemId = snapshot.request().key().inventoryItemId().value();
        entity.warehouseId = snapshot.request().key().warehouseId().value();
        entity.stockLocationId = snapshot.request().key().locationId().value();
        entity.lotCode = snapshot.request().key().lotCode().orElse(null);
        entity.serialNumber = snapshot.request().key().serialNumber().orElse(null);
        entity.expiryDate = snapshot.request().key().expiryDate().orElse(null);
        entity.condition = snapshot.request().key().condition();
        entity.originalQuantity = snapshot.request().quantity();
        entity.sourceType = snapshot.request().source().sourceType();
        entity.sourceId = snapshot.request().source().sourceId();
        entity.idempotencyKey = snapshot.request().idempotencyKey();
        entity.createdAt = snapshot.createdAt();
        entity.expiresAt = snapshot.request().expiresAt();
        entity.version = snapshot.version();
        entity.apply(snapshot);
        return entity;
    }

    void apply(StockReservationSnapshot snapshot) {
        consumedQuantity = snapshot.consumedQuantity();
        releasedQuantity = snapshot.releasedQuantity();
        state = snapshot.state();
    }

    StockReservationSnapshot snapshot() {
        return new StockReservationSnapshot(
                new CompanyId(companyId), new StockReservationId(stockReservationId),
                new StockReservationRequest(
                        InventoryPersistenceValues.stockKey(
                                inventoryItemId, warehouseId, stockLocationId, lotCode,
                                serialNumber, expiryDate, condition),
                        originalQuantity, new StockSourceReference(sourceType, sourceId),
                        expiresAt, idempotencyKey),
                createdAt, consumedQuantity, releasedQuantity, state, version);
    }

    long version() { return version; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID stockReservationId;
        public Key() { }
        Key(UUID companyId, UUID stockReservationId) { this.companyId = companyId; this.stockReservationId = stockReservationId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(stockReservationId, that.stockReservationId); }
        @Override public int hashCode() { return Objects.hash(companyId, stockReservationId); }
    }
}
