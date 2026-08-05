package py.com.logixone.plugins.inventory.api;

import java.math.BigDecimal;
import java.util.Objects;

/** Quantity projection in the catalog item's base unit. */
public record StockAvailability(
        StockKey key,
        String baseUnitCode,
        BigDecimal physicalQuantity,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity,
        long version) {

    public StockAvailability {
        Objects.requireNonNull(key, "key");
        baseUnitCode = ContractValues.code(baseUnitCode, "baseUnitCode", 16);
        physicalQuantity = ContractValues.nonNegativeQuantity(physicalQuantity, "physicalQuantity");
        reservedQuantity = ContractValues.nonNegativeQuantity(reservedQuantity, "reservedQuantity");
        availableQuantity = ContractValues.nonNegativeQuantity(availableQuantity, "availableQuantity");
        if (physicalQuantity.subtract(reservedQuantity).compareTo(availableQuantity) != 0) {
            throw new IllegalArgumentException("availableQuantity must equal physicalQuantity minus reservedQuantity");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
