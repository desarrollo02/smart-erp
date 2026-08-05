package py.com.logixone.plugins.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.StockReservationReference;
import py.com.logixone.plugins.inventory.api.StockReservationRequest;
import py.com.logixone.plugins.inventory.api.StockReservationState;

/** Reservation aggregate with explicit consumed, released and remaining quantities. */
public final class StockReservation {
    private final CompanyId companyId;
    private final StockReservationId id;
    private final StockReservationRequest request;
    private final Instant createdAt;
    private BigDecimal consumedQuantity = BigDecimal.ZERO;
    private BigDecimal releasedQuantity = BigDecimal.ZERO;
    private StockReservationState state = StockReservationState.ACTIVE;
    private long version;

    private StockReservation(
            CompanyId companyId,
            StockReservationId id,
            StockReservationRequest request,
            Instant createdAt) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.id = Objects.requireNonNull(id, "id");
        this.request = Objects.requireNonNull(request, "request");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        if (!request.expiresAt().isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after reservation creation");
        }
    }

    public static StockReservation create(
            CompanyId companyId,
            StockReservationId id,
            StockReservationRequest request,
            Instant createdAt) {
        return new StockReservation(companyId, id, request, createdAt);
    }

    public static StockReservation restore(StockReservationSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        StockReservation reservation = new StockReservation(
                snapshot.companyId(), snapshot.id(), snapshot.request(), snapshot.createdAt());
        reservation.consumedQuantity = snapshot.consumedQuantity();
        reservation.releasedQuantity = snapshot.releasedQuantity();
        reservation.state = snapshot.state();
        reservation.version = snapshot.version();
        return reservation;
    }

    public void consume(BigDecimal quantity, long expectedVersion) {
        verifyMutable(expectedVersion);
        BigDecimal normalized = requireAtMostRemaining(quantity);
        consumedQuantity = consumedQuantity.add(normalized);
        state = remainingQuantity().signum() == 0
                ? StockReservationState.CONSUMED
                : StockReservationState.PARTIALLY_CONSUMED;
        version++;
    }

    public void release(BigDecimal quantity, long expectedVersion) {
        verifyMutable(expectedVersion);
        BigDecimal normalized = requireAtMostRemaining(quantity);
        releasedQuantity = releasedQuantity.add(normalized);
        if (remainingQuantity().signum() == 0) {
            state = StockReservationState.RELEASED;
        } else if (consumedQuantity.signum() > 0) {
            state = StockReservationState.PARTIALLY_CONSUMED;
        }
        version++;
    }

    public void expire(Instant now, long expectedVersion) {
        verifyMutable(expectedVersion);
        Objects.requireNonNull(now, "now");
        if (now.isBefore(request.expiresAt())) {
            throw new IllegalStateException("Reservation has not reached its expiry time");
        }
        releasedQuantity = releasedQuantity.add(remainingQuantity());
        state = StockReservationState.EXPIRED;
        version++;
    }

    private BigDecimal requireAtMostRemaining(BigDecimal quantity) {
        BigDecimal normalized = InventoryValues.quantity(quantity, "quantity", true);
        if (normalized.compareTo(remainingQuantity()) > 0) {
            throw new IllegalArgumentException("Quantity exceeds reservation remainder");
        }
        return normalized;
    }

    private void verifyMutable(long expectedVersion) {
        if (expectedVersion != version) {
            throw new ConcurrentInventoryChangeException(expectedVersion, version);
        }
        if (state == StockReservationState.CONSUMED
                || state == StockReservationState.RELEASED
                || state == StockReservationState.EXPIRED) {
            throw new IllegalStateException("Terminal reservation cannot be changed");
        }
    }

    public BigDecimal remainingQuantity() {
        return request.quantity().subtract(consumedQuantity).subtract(releasedQuantity);
    }

    public StockReservationReference reference() {
        return new StockReservationReference(
                id, request.key(), request.quantity(), consumedQuantity, releasedQuantity,
                remainingQuantity(), state, request.source(), request.expiresAt(), version);
    }

    public CompanyId companyId() { return companyId; }
    public StockReservationId id() { return id; }
    public StockReservationState state() { return state; }
    public BigDecimal originalQuantity() { return request.quantity(); }
    public BigDecimal consumedQuantity() { return consumedQuantity; }
    public BigDecimal releasedQuantity() { return releasedQuantity; }
    public long version() { return version; }
    public StockReservationSnapshot snapshot() {
        return new StockReservationSnapshot(
                companyId, id, request, createdAt, consumedQuantity, releasedQuantity, state, version);
    }
}
