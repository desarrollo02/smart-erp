package py.com.logixone.plugins.inventory.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.StockReservationRequest;
import py.com.logixone.plugins.inventory.api.StockReservationState;
import py.com.logixone.plugins.inventory.api.StockSourceReference;

class StockReservationTest {
    @Test
    void supportsPartialConsumptionAndReleaseWithExplicitTerminalState() {
        Instant createdAt = Instant.parse("2026-07-31T12:00:00Z");
        StockReservation reservation = reservation(createdAt);

        reservation.consume(new BigDecimal("2"), 0);
        assertEquals(StockReservationState.PARTIALLY_CONSUMED, reservation.state());
        reservation.release(new BigDecimal("3"), 1);

        assertEquals(StockReservationState.RELEASED, reservation.state());
        assertEquals(0, BigDecimal.ZERO.compareTo(reservation.remainingQuantity()));
        assertEquals(0, new BigDecimal("2").compareTo(reservation.reference().consumedQuantity()));
        assertThrows(IllegalStateException.class, () -> reservation.release(BigDecimal.ONE, 2));
    }

    @Test
    void expiresOnlyAtOrAfterRequiredExpiryAndReleasesRemainder() {
        Instant createdAt = Instant.parse("2026-07-31T12:00:00Z");
        StockReservation reservation = reservation(createdAt);

        assertThrows(IllegalStateException.class, () -> reservation.expire(createdAt.plusSeconds(10), 0));
        reservation.expire(createdAt.plusSeconds(3600), 0);

        assertEquals(StockReservationState.EXPIRED, reservation.state());
        assertEquals(0, new BigDecimal("5").compareTo(reservation.releasedQuantity()));
    }

    private static StockReservation reservation(Instant createdAt) {
        StockReservationRequest request = new StockReservationRequest(
                InventoryDomainFixtures.key(10, 11), new BigDecimal("5"),
                new StockSourceReference("sales_order", "SO-1"), createdAt.plusSeconds(3600),
                "sales:so-1:line-1");
        return StockReservation.create(
                InventoryDomainFixtures.companyId(),
                new StockReservationId(InventoryDomainFixtures.uuid(30)), request, createdAt);
    }
}
