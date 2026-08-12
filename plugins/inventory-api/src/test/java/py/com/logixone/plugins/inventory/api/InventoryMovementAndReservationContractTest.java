package py.com.logixone.plugins.inventory.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryMovementAndReservationContractTest {
    @Test
    void movementPreservesConversionSnapshotAndRequiresReversalLinkOnlyForReversal() {
        MovementQuantity quantity = new MovementQuantity(
                "box", new BigDecimal("2"), "ea", new BigDecimal("12.000000000000"),
                new BigDecimal("24"), 7);
        StockMovementLine line = new StockMovementLine(key(), StockMovementDirection.INCREASE, quantity);
        StockMovementRequest request = new StockMovementRequest(
                StockMovementType.RECEIPT, "manual", new StockSourceReference("manual", "DEMO-1"),
                " Client-Request-1 ", List.of(line), Optional.empty());

        assertEquals("BOX", request.lines().getFirst().quantity().presentedUnitCode());
        assertEquals("client-request-1", request.idempotencyKey());
        assertThrows(IllegalArgumentException.class, () -> new StockMovementRequest(
                StockMovementType.REVERSAL, "correction", request.source(), "retry-2",
                List.of(line), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new MovementQuantity(
                "EA", BigDecimal.ONE, "EA", new BigDecimal("1.0000000000001"), BigDecimal.ONE, 0));
    }

    @Test
    void catalogMovementKeepsInventoryIdentityPrivateAndResolvesOneStockLine() {
        MovementQuantity quantity = new MovementQuantity(
                "BOX", BigDecimal.ONE, "EA", new BigDecimal("12"),
                new BigDecimal("12"), 4);
        CatalogStockMovementRequest request = new CatalogStockMovementRequest(
                StockMovementType.RECEIPT, "PURCHASE_RECEIPT",
                new StockSourceReference("PURCHASING_RECEIPT", "RC-1"),
                "receipt-line-1", new UUID(0, 20), new WarehouseId(new UUID(0, 2)),
                new StockLocationId(new UUID(0, 3)), Optional.of("LOT-A"),
                Optional.empty(), Optional.of(LocalDate.of(2027, 1, 1)),
                StockCondition.AVAILABLE, quantity);

        StockMovementRequest resolved = request.resolve(new InventoryItemId(new UUID(0, 1)));

        assertEquals(new UUID(0, 1), resolved.lines().getFirst().key().inventoryItemId().value());
        assertEquals(StockMovementDirection.INCREASE, resolved.lines().getFirst().direction());
        assertThrows(IllegalArgumentException.class, () -> new CatalogStockMovementRequest(
                StockMovementType.RECEIPT, "PURCHASE_RECEIPT", request.source(),
                "receipt-line-2", request.catalogItemId(), request.warehouseId(),
                request.locationId(), Optional.empty(), Optional.empty(), Optional.empty(),
                StockCondition.AVAILABLE,
                new MovementQuantity("EA", BigDecimal.ONE, "EA", BigDecimal.ONE,
                        new BigDecimal("2"), 4)));
    }

    @Test
    void reservationRequiresSourceExpiryAndReconciledLifecycleQuantities() {
        Instant expiry = Instant.parse("2026-08-01T12:00:00Z");
        StockReservationRequest request = new StockReservationRequest(
                key(), new BigDecimal("5.5"), new StockSourceReference("sales_order", "SO-10"),
                expiry, "sales:so-10:line-1");
        StockReservationReference reference = new StockReservationReference(
                new StockReservationId(new UUID(0, 5)), request.key(), request.quantity(),
                new BigDecimal("2"), BigDecimal.ONE, new BigDecimal("2.5"),
                StockReservationState.PARTIALLY_CONSUMED, request.source(), expiry, 2);

        assertEquals(0, new BigDecimal("2.5").compareTo(reference.remainingQuantity()));
        assertThrows(IllegalArgumentException.class, () -> new StockReservationReference(
                reference.id(), reference.key(), new BigDecimal("5.5"), new BigDecimal("2"),
                BigDecimal.ONE, new BigDecimal("3"), reference.state(), reference.source(), expiry, 2));
    }

    private static StockKey key() {
        return new StockKey(
                new InventoryItemId(new UUID(0, 1)), new WarehouseId(new UUID(0, 2)),
                new StockLocationId(new UUID(0, 3)), Optional.of("LOT-A"), Optional.empty(),
                Optional.empty(), StockCondition.AVAILABLE);
    }
}
