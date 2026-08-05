package py.com.logixone.plugins.inventory.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugins.inventory.api.StockKey;

public record StockCountLineSnapshot(
        int lineNumber,
        StockKey key,
        BigDecimal theoreticalQuantity,
        Optional<BigDecimal> countedQuantity) {
    public StockCountLineSnapshot {
        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber must be positive");
        }
        Objects.requireNonNull(key, "key");
        theoreticalQuantity = InventoryValues.quantity(theoreticalQuantity, "theoreticalQuantity", false);
        countedQuantity = Objects.requireNonNull(countedQuantity, "countedQuantity")
                .map(value -> InventoryValues.quantity(value, "countedQuantity", false));
    }
}
