package py.com.logixone.plugins.purchasing.api;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/** Typed line accepted by the controlled open-document import contract. */
public record PurchaseImportLine(
        String sourceLineKey,
        PurchaseLineKind kind,
        Optional<String> catalogItemId,
        String description,
        String unitCode,
        BigDecimal quantity,
        Optional<BigDecimal> expectedUnitPrice) {

    public PurchaseImportLine {
        sourceLineKey = ContractValues.text(sourceLineKey, "sourceLineKey", 160);
        Objects.requireNonNull(kind, "kind");
        catalogItemId = Objects.requireNonNull(catalogItemId, "catalogItemId")
                .map(value -> ContractValues.uuid(value, "catalogItemId").toString());
        description = ContractValues.text(description, "description", 240);
        unitCode = ContractValues.code(unitCode, "unitCode", 16);
        quantity = ContractValues.quantity(quantity, "quantity");
        expectedUnitPrice = Objects.requireNonNull(expectedUnitPrice, "expectedUnitPrice")
                .map(value -> ContractValues.amount(value, "expectedUnitPrice"));
        if (expectedUnitPrice.stream().anyMatch(value -> Math.max(value.scale(), 0) > 6)) {
            throw new IllegalArgumentException(
                    "expectedUnitPrice supports at most 6 decimal places");
        }
        if (kind == PurchaseLineKind.STOCK && catalogItemId.isEmpty()) {
            throw new IllegalArgumentException("STOCK lines require catalogItemId");
        }
    }
}
