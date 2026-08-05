package py.com.logixone.plugins.inventory.domain;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.StockReservationReference;
import py.com.logixone.plugins.inventory.api.StockReservationState;

/** Immutable idempotency receipt for a reservation mutation. */
public record ReservationOperation(
        CompanyId companyId,
        String idempotencyKey,
        StockReservationId reservationId,
        ReservationOperationType type,
        BigDecimal quantity,
        BigDecimal resultingConsumedQuantity,
        BigDecimal resultingReleasedQuantity,
        BigDecimal resultingRemainingQuantity,
        StockReservationState resultingState,
        long resultingVersion,
        Instant occurredAt) {

    public ReservationOperation {
        Objects.requireNonNull(companyId, "companyId");
        idempotencyKey = canonicalIdempotencyKey(idempotencyKey);
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(type, "type");
        quantity = InventoryValues.quantity(quantity, "quantity", true);
        resultingConsumedQuantity = InventoryValues.quantity(
                resultingConsumedQuantity, "resultingConsumedQuantity", false);
        resultingReleasedQuantity = InventoryValues.quantity(
                resultingReleasedQuantity, "resultingReleasedQuantity", false);
        resultingRemainingQuantity = InventoryValues.quantity(
                resultingRemainingQuantity, "resultingRemainingQuantity", false);
        Objects.requireNonNull(resultingState, "resultingState");
        if (resultingVersion < 0) {
            throw new IllegalArgumentException("resultingVersion must not be negative");
        }
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public static ReservationOperation capture(
            StockReservation reservation,
            ReservationOperationType type,
            BigDecimal quantity,
            String idempotencyKey,
            Instant occurredAt) {
        Objects.requireNonNull(reservation, "reservation");
        return new ReservationOperation(
                reservation.companyId(), idempotencyKey, reservation.id(), type, quantity,
                reservation.consumedQuantity(), reservation.releasedQuantity(),
                reservation.remainingQuantity(), reservation.state(), reservation.version(), occurredAt);
    }

    public boolean matches(
            StockReservationId expectedReservationId,
            ReservationOperationType expectedType,
            BigDecimal expectedQuantity) {
        return reservationId.equals(expectedReservationId)
                && type == expectedType
                && quantity.compareTo(expectedQuantity) == 0;
    }

    public StockReservationReference result(StockReservation reservation) {
        Objects.requireNonNull(reservation, "reservation");
        var request = reservation.snapshot().request();
        return new StockReservationReference(
                reservationId, request.key(), request.quantity(), resultingConsumedQuantity,
                resultingReleasedQuantity, resultingRemainingQuantity, resultingState,
                request.source(), request.expiresAt(), resultingVersion);
    }

    public static String canonicalIdempotencyKey(String value) {
        Objects.requireNonNull(value, "idempotencyKey");
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 160) {
            throw new IllegalArgumentException("idempotencyKey length must be between 1 and 160");
        }
        return normalized;
    }
}
