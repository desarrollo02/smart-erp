package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.StockReservationState;
import py.com.logixone.plugins.inventory.domain.ReservationOperation;
import py.com.logixone.plugins.inventory.domain.ReservationOperationType;

@Entity
@Table(name = "stock_reservation_operation", schema = InventoryPersistenceNames.SCHEMA)
@IdClass(ReservationOperationEntity.Key.class)
public class ReservationOperationEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "idempotency_key", nullable = false, length = 160, updatable = false)
    private String idempotencyKey;
    @Column(name = "stock_reservation_id", nullable = false, updatable = false)
    private UUID stockReservationId;
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 24, updatable = false)
    private ReservationOperationType operationType;
    @Column(name = "operation_quantity", nullable = false, precision = 30, scale = 6, updatable = false)
    private BigDecimal operationQuantity;
    @Column(name = "resulting_consumed_quantity", nullable = false, precision = 30, scale = 6, updatable = false)
    private BigDecimal resultingConsumedQuantity;
    @Column(name = "resulting_released_quantity", nullable = false, precision = 30, scale = 6, updatable = false)
    private BigDecimal resultingReleasedQuantity;
    @Column(name = "resulting_remaining_quantity", nullable = false, precision = 30, scale = 6, updatable = false)
    private BigDecimal resultingRemainingQuantity;
    @Enumerated(EnumType.STRING)
    @Column(name = "resulting_state", nullable = false, length = 32, updatable = false)
    private StockReservationState resultingState;
    @Column(name = "resulting_version", nullable = false, updatable = false)
    private long resultingVersion;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected ReservationOperationEntity() {
    }

    static ReservationOperationEntity from(ReservationOperation operation) {
        ReservationOperationEntity entity = new ReservationOperationEntity();
        entity.companyId = operation.companyId().value();
        entity.idempotencyKey = operation.idempotencyKey();
        entity.stockReservationId = operation.reservationId().value();
        entity.operationType = operation.type();
        entity.operationQuantity = operation.quantity();
        entity.resultingConsumedQuantity = operation.resultingConsumedQuantity();
        entity.resultingReleasedQuantity = operation.resultingReleasedQuantity();
        entity.resultingRemainingQuantity = operation.resultingRemainingQuantity();
        entity.resultingState = operation.resultingState();
        entity.resultingVersion = operation.resultingVersion();
        entity.occurredAt = operation.occurredAt();
        return entity;
    }

    ReservationOperation snapshot() {
        return new ReservationOperation(
                new CompanyId(companyId), idempotencyKey,
                new StockReservationId(stockReservationId), operationType, operationQuantity,
                resultingConsumedQuantity, resultingReleasedQuantity,
                resultingRemainingQuantity, resultingState, resultingVersion, occurredAt);
    }

    public static final class Key implements Serializable {
        public UUID companyId;
        public String idempotencyKey;
        public Key() { }
        Key(UUID companyId, String idempotencyKey) {
            this.companyId = companyId;
            this.idempotencyKey = idempotencyKey;
        }
        @Override public boolean equals(Object other) {
            return this == other || other instanceof Key that
                    && Objects.equals(companyId, that.companyId)
                    && Objects.equals(idempotencyKey, that.idempotencyKey);
        }
        @Override public int hashCode() { return Objects.hash(companyId, idempotencyKey); }
    }
}
