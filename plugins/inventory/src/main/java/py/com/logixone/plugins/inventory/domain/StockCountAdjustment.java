package py.com.logixone.plugins.inventory.domain;

import java.math.BigDecimal;
import java.util.Objects;
import py.com.logixone.plugins.inventory.api.StockKey;

/** Explicit difference that must later become an ADJUSTMENT movement. */
public record StockCountAdjustment(
        StockKey key,
        BigDecimal theoreticalQuantity,
        BigDecimal countedQuantity,
        BigDecimal difference) {

    public StockCountAdjustment {
        Objects.requireNonNull(key, "key");
        theoreticalQuantity = InventoryValues.quantity(theoreticalQuantity, "theoreticalQuantity", false);
        countedQuantity = InventoryValues.quantity(countedQuantity, "countedQuantity", false);
        difference = InventoryValues.signedQuantity(difference, "difference");
        if (countedQuantity.subtract(theoreticalQuantity).compareTo(difference) != 0) {
            throw new IllegalArgumentException("difference must equal counted minus theoretical");
        }
    }
}
