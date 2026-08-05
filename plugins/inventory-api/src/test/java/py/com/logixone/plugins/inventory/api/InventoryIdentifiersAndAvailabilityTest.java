package py.com.logixone.plugins.inventory.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryIdentifiersAndAvailabilityTest {
    @Test
    void parsesCanonicalIdentifiersAndRejectsNonCanonicalText() {
        String canonical = "00000000-0000-0000-0000-000000000001";

        assertEquals(canonical, WarehouseId.parse(canonical).toString());
        assertEquals(canonical, StockLocationId.parse(canonical).toString());
        assertEquals(canonical, InventoryItemId.parse(canonical).toString());
        assertThrows(IllegalArgumentException.class,
                () -> WarehouseId.parse("00000000-0000-0000-0000-00000000000A"));
    }

    @Test
    void availabilityRequiresExactReconciliationAndSixDecimalPrecision() {
        StockKey key = key();
        StockAvailability availability = new StockAvailability(
                key, " ea ", new BigDecimal("10.500000"), new BigDecimal("2.25"),
                new BigDecimal("8.25"), 3);

        assertEquals("EA", availability.baseUnitCode());
        assertEquals(0, new BigDecimal("8.25").compareTo(availability.availableQuantity()));
        assertThrows(IllegalArgumentException.class, () -> new StockAvailability(
                key, "EA", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, 0));
        assertThrows(IllegalArgumentException.class, () -> new StockAvailability(
                key, "EA", new BigDecimal("1.0000001"), BigDecimal.ZERO,
                new BigDecimal("1.0000001"), 0));
    }

    private static StockKey key() {
        return new StockKey(
                new InventoryItemId(new UUID(0, 1)),
                new WarehouseId(new UUID(0, 2)),
                new StockLocationId(new UUID(0, 3)),
                Optional.empty(), Optional.empty(), Optional.empty(), StockCondition.AVAILABLE);
    }
}
