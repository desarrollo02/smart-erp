package py.com.logixone.plugins.inventory.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugins.inventory.api.StockMovementDirection;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockMovementLine;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;
import py.com.logixone.plugins.inventory.api.StockMovementType;
import py.com.logixone.plugins.inventory.api.StockSourceReference;

class StockMovementTest {
    @Test
    void acceptsAnAtomicTransferWithEqualOppositeBaseQuantities() {
        StockMovementLine debit = new StockMovementLine(
                InventoryDomainFixtures.key(10, 11), StockMovementDirection.DECREASE,
                InventoryDomainFixtures.quantity("3"));
        StockMovementLine credit = new StockMovementLine(
                InventoryDomainFixtures.key(10, 12), StockMovementDirection.INCREASE,
                InventoryDomainFixtures.quantity("3"));
        StockMovementRequest request = request(StockMovementType.TRANSFER, List.of(debit, credit));

        StockMovement movement = StockMovement.post(
                InventoryDomainFixtures.companyId(),
                new StockMovementId(InventoryDomainFixtures.uuid(20)), request,
                Instant.parse("2026-07-31T12:00:00Z"));

        assertEquals(StockMovementType.TRANSFER, movement.reference().type());
        assertEquals(2, movement.lines().size());
    }

    @Test
    void rejectsBrokenTransferAndWrongReceiptDirection() {
        StockMovementLine debit = new StockMovementLine(
                InventoryDomainFixtures.key(10, 11), StockMovementDirection.DECREASE,
                InventoryDomainFixtures.quantity("3"));
        StockMovementLine unequalCredit = new StockMovementLine(
                InventoryDomainFixtures.key(10, 12), StockMovementDirection.INCREASE,
                InventoryDomainFixtures.quantity("2"));

        assertThrows(IllegalArgumentException.class, () -> StockMovement.post(
                InventoryDomainFixtures.companyId(),
                new StockMovementId(InventoryDomainFixtures.uuid(20)),
                request(StockMovementType.TRANSFER, List.of(debit, unequalCredit)), Instant.EPOCH));
        assertThrows(IllegalArgumentException.class, () -> StockMovement.post(
                InventoryDomainFixtures.companyId(),
                new StockMovementId(InventoryDomainFixtures.uuid(21)),
                request(StockMovementType.RECEIPT, List.of(debit)), Instant.EPOCH));
    }

    private static StockMovementRequest request(
            StockMovementType type, List<StockMovementLine> lines) {
        return new StockMovementRequest(
                type, "manual", new StockSourceReference("manual", "DEMO"),
                type.name().toLowerCase() + "-1", lines, Optional.empty());
    }
}
