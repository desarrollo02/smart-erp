package py.com.logixone.plugins.sales.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;

public record SalesLineSnapshot(UUID id, CatalogItemId catalogItemId, String catalogCode,
        String description, String unitCode, boolean stockManaged, BigDecimal quantity,
        BigDecimal unitPrice, String taxCode, Optional<String> priceListId,
        boolean manualPrice, Optional<String> priceExceptionReason, long catalogVersion) {
    public SalesLineSnapshot {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(catalogItemId, "catalogItemId");
        catalogCode = SalesValues.text(catalogCode, "catalog code", 64); description = SalesValues.text(description, "description", 240);
        unitCode = SalesValues.text(unitCode, "unit code", 16); quantity = SalesValues.quantity(quantity); unitPrice = SalesValues.amount(unitPrice);
        taxCode = SalesValues.text(taxCode, "tax code", 32); priceListId = Objects.requireNonNull(priceListId, "priceListId");
        priceExceptionReason = Objects.requireNonNull(priceExceptionReason, "priceExceptionReason").map(v -> SalesValues.text(v, "price exception reason", 240));
        if (manualPrice != priceExceptionReason.isPresent() || catalogVersion < 0) throw new IllegalArgumentException("Invalid price provenance");
    }
    public BigDecimal total() { return quantity.multiply(unitPrice); }
}
