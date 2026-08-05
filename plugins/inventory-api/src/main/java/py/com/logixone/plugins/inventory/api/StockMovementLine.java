package py.com.logixone.plugins.inventory.api;

import java.util.Objects;

public record StockMovementLine(
        StockKey key,
        StockMovementDirection direction,
        MovementQuantity quantity) {

    public StockMovementLine {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(quantity, "quantity");
    }
}
