package py.com.logixone.plugins.commercialcatalog.api;

import java.math.BigDecimal;
import java.util.Objects;

/** Request for an item-specific quantity conversion. */
public record CatalogUnitConversionRequest(
        CatalogItemId itemId,
        String sourceUnitCode,
        String targetUnitCode,
        BigDecimal quantity) {

    public CatalogUnitConversionRequest {
        Objects.requireNonNull(itemId, "itemId");
        sourceUnitCode = ContractValues.code(sourceUnitCode, "sourceUnitCode", 16);
        targetUnitCode = ContractValues.code(targetUnitCode, "targetUnitCode", 16);
        quantity = ContractValues.positive(quantity, "quantity");
    }
}
