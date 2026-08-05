package py.com.logixone.plugins.commercialcatalog.api;

import java.math.BigDecimal;
import java.util.Objects;

/** Deterministic conversion result whose factor can be snapshotted by a consumer. */
public record CatalogUnitConversionResult(
        CatalogItemId itemId,
        String sourceUnitCode,
        String targetUnitCode,
        BigDecimal sourceQuantity,
        BigDecimal factor,
        BigDecimal convertedQuantity,
        long itemVersion) {

    public CatalogUnitConversionResult {
        Objects.requireNonNull(itemId, "itemId");
        sourceUnitCode = ContractValues.code(sourceUnitCode, "sourceUnitCode", 16);
        targetUnitCode = ContractValues.code(targetUnitCode, "targetUnitCode", 16);
        sourceQuantity = ContractValues.positive(sourceQuantity, "sourceQuantity");
        factor = ContractValues.positive(factor, "factor");
        convertedQuantity = ContractValues.positive(convertedQuantity, "convertedQuantity");
        if (itemVersion < 0) {
            throw new IllegalArgumentException("itemVersion must not be negative");
        }
    }
}
