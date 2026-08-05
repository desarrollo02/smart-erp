package py.com.logixone.plugins.inventory.api;

import java.math.BigDecimal;

/** Reproducible unit conversion snapshot carried by a movement line. */
public record MovementQuantity(
        String presentedUnitCode,
        BigDecimal presentedQuantity,
        String baseUnitCode,
        BigDecimal conversionFactor,
        BigDecimal baseQuantity,
        long catalogItemVersion) {

    public MovementQuantity {
        presentedUnitCode = ContractValues.code(presentedUnitCode, "presentedUnitCode", 16);
        presentedQuantity = ContractValues.positiveQuantity(presentedQuantity, "presentedQuantity");
        baseUnitCode = ContractValues.code(baseUnitCode, "baseUnitCode", 16);
        conversionFactor = ContractValues.positiveFactor(conversionFactor, "conversionFactor");
        baseQuantity = ContractValues.positiveQuantity(baseQuantity, "baseQuantity");
        if (catalogItemVersion < 0) {
            throw new IllegalArgumentException("catalogItemVersion must not be negative");
        }
    }
}
