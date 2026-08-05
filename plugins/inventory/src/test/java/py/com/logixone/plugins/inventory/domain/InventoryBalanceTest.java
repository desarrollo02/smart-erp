package py.com.logixone.plugins.inventory.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InventoryBalanceTest {
    @Test
    void keepsPhysicalReservedAndAvailableNonNegative() {
        InventoryBalance balance = InventoryBalance.empty(
                InventoryDomainFixtures.companyId(), InventoryDomainFixtures.key(10, 11), "EA");

        balance.receive(new BigDecimal("10"), 0);
        balance.reserve(new BigDecimal("4"), 1);
        assertEquals(0, new BigDecimal("6").compareTo(balance.availableQuantity()));

        assertThrows(IllegalStateException.class, () -> balance.issue(new BigDecimal("7"), 2));
        assertEquals(2, balance.version());
        balance.consumeReserved(new BigDecimal("3"), 2);
        assertEquals(0, new BigDecimal("7").compareTo(balance.physicalQuantity()));
        assertEquals(0, BigDecimal.ONE.compareTo(balance.reservedQuantity()));
        assertEquals(0, new BigDecimal("6").compareTo(balance.availability().availableQuantity()));
    }

    @Test
    void rejectsStaleVersionAndExcessPrecision() {
        InventoryBalance balance = InventoryBalance.empty(
                InventoryDomainFixtures.companyId(), InventoryDomainFixtures.key(10, 11), "EA");
        balance.receive(BigDecimal.ONE, 0);

        assertThrows(ConcurrentInventoryChangeException.class,
                () -> balance.reserve(BigDecimal.ONE, 0));
        assertThrows(IllegalArgumentException.class,
                () -> balance.receive(new BigDecimal("0.0000001"), 1));
    }

    @Test
    void appliesOneAggregatedPhysicalDeltaWithOneVersionAdvance() {
        InventoryBalance balance = InventoryBalance.empty(
                InventoryDomainFixtures.companyId(), InventoryDomainFixtures.key(10, 11), "EA");
        balance.receive(new BigDecimal("10"), 0);
        balance.reserve(new BigDecimal("3"), 1);

        balance.adjustPhysical(new BigDecimal("-4"), 2);

        assertEquals(0, new BigDecimal("6").compareTo(balance.physicalQuantity()));
        assertEquals(0, new BigDecimal("3").compareTo(balance.availableQuantity()));
        assertEquals(3, balance.version());
        assertThrows(IllegalStateException.class,
                () -> balance.adjustPhysical(new BigDecimal("-4"), 3));
    }
}
