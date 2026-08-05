package py.com.logixone.plugins.inventory.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugins.inventory.api.StockCountId;

class StockCountTest {
    @Test
    void followsLifecycleLocksOnlyItsScopeAndProducesAdjustmentDifference() {
        var countedKey = InventoryDomainFixtures.key(10, 11);
        StockCount count = StockCount.draft(
                InventoryDomainFixtures.companyId(),
                new StockCountId(InventoryDomainFixtures.uuid(40)),
                new StockCountScope(countedKey.warehouseId(), Optional.of(countedKey.locationId())));

        count.addLine(countedKey, new BigDecimal("10"), 0);
        count.start(1);
        assertTrue(count.blocks(countedKey));
        assertFalse(count.blocks(InventoryDomainFixtures.key(10, 12)));
        count.record(countedKey, new BigDecimal("8"), 2);
        count.sendToReview(3);
        var adjustments = count.post(4);

        assertEquals(StockCountState.POSTED, count.state());
        assertEquals(1, adjustments.size());
        assertEquals(0, new BigDecimal("-2").compareTo(adjustments.getFirst().difference()));
        assertFalse(count.blocks(countedKey));
        assertThrows(IllegalStateException.class, () -> count.cancel(5));
    }

    @Test
    void cannotReviewIncompleteCountOrAddOutOfScopeLine() {
        var key = InventoryDomainFixtures.key(10, 11);
        StockCount count = StockCount.draft(
                InventoryDomainFixtures.companyId(),
                new StockCountId(InventoryDomainFixtures.uuid(41)),
                new StockCountScope(key.warehouseId(), Optional.of(key.locationId())));

        assertThrows(IllegalArgumentException.class,
                () -> count.addLine(InventoryDomainFixtures.key(10, 12), BigDecimal.ONE, 0));
        count.addLine(key, BigDecimal.ONE, 0);
        count.start(1);
        assertThrows(IllegalStateException.class, () -> count.sendToReview(2));
    }
}
