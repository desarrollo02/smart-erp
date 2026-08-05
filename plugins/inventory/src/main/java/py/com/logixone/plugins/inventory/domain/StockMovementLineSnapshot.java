package py.com.logixone.plugins.inventory.domain;

import java.util.Objects;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.inventory.api.StockMovementLine;

public record StockMovementLineSnapshot(
        int lineNumber,
        StockMovementLine line,
        CatalogItemId catalogItemId,
        String catalogCode,
        String catalogName) {
    public StockMovementLineSnapshot {
        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber must be positive");
        }
        Objects.requireNonNull(line, "line");
        Objects.requireNonNull(catalogItemId, "catalogItemId");
        catalogCode = InventoryValues.code(catalogCode, "catalogCode", 64);
        catalogName = InventoryValues.text(catalogName, "catalogName", 240);
    }
}
