package py.com.logixone.plugins.commercialcatalog.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Explicit price-list quote request; sales remains responsible for list selection. */
public record CatalogPriceQuoteRequest(
        PriceListId priceListId,
        CatalogItemId itemId,
        String unitCode,
        BigDecimal quantity,
        Instant effectiveAt) {

    public CatalogPriceQuoteRequest {
        Objects.requireNonNull(priceListId, "priceListId");
        Objects.requireNonNull(itemId, "itemId");
        unitCode = ContractValues.code(unitCode, "unitCode", 16);
        quantity = ContractValues.positive(quantity, "quantity");
        Objects.requireNonNull(effectiveAt, "effectiveAt");
    }
}
