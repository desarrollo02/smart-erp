package py.com.logixone.plugins.inventory.domain;

import java.math.BigDecimal;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockKey;

public record InventoryBalanceSnapshot(
        CompanyId companyId,
        StockKey key,
        String baseUnitCode,
        BigDecimal physicalQuantity,
        BigDecimal reservedQuantity,
        long version) {
    public InventoryBalanceSnapshot {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(key, "key");
        baseUnitCode = InventoryValues.code(baseUnitCode, "baseUnitCode", 16);
        physicalQuantity = InventoryValues.quantity(physicalQuantity, "physicalQuantity", false);
        reservedQuantity = InventoryValues.quantity(reservedQuantity, "reservedQuantity", false);
        if (reservedQuantity.compareTo(physicalQuantity) > 0) {
            throw new IllegalArgumentException("reservedQuantity must not exceed physicalQuantity");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
