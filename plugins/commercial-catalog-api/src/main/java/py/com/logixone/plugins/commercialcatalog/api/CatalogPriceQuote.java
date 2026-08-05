package py.com.logixone.plugins.commercialcatalog.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Immutable quote with the price-list decision inputs required for a snapshot. */
public record CatalogPriceQuote(
        PriceListId priceListId,
        PriceEntryId priceEntryId,
        CatalogItemId itemId,
        String currency,
        CatalogTaxMode taxMode,
        String unitCode,
        BigDecimal quantity,
        BigDecimal unitAmount,
        BigDecimal totalAmount,
        Instant validFrom,
        Optional<Instant> validUntil,
        long priceListVersion) {

    public CatalogPriceQuote {
        Objects.requireNonNull(priceListId, "priceListId");
        Objects.requireNonNull(priceEntryId, "priceEntryId");
        Objects.requireNonNull(itemId, "itemId");
        currency = ContractValues.currency(currency);
        Objects.requireNonNull(taxMode, "taxMode");
        unitCode = ContractValues.code(unitCode, "unitCode", 16);
        quantity = ContractValues.positive(quantity, "quantity");
        unitAmount = ContractValues.nonNegative(unitAmount, "unitAmount");
        totalAmount = ContractValues.nonNegative(totalAmount, "totalAmount");
        Objects.requireNonNull(validFrom, "validFrom");
        validUntil = Objects.requireNonNull(validUntil, "validUntil");
        validUntil.ifPresent(end -> {
            if (!end.isAfter(validFrom)) {
                throw new IllegalArgumentException("validUntil must be after validFrom");
            }
        });
        if (priceListVersion < 0) {
            throw new IllegalArgumentException("priceListVersion must not be negative");
        }
    }
}
