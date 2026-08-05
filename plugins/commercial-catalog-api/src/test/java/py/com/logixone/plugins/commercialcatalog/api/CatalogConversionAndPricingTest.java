package py.com.logixone.plugins.commercialcatalog.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogConversionAndPricingTest {

    @Test
    void conversionContractPreservesFactorAndVersions() {
        CatalogItemId itemId = new CatalogItemId(UUID.randomUUID());
        CatalogUnitConversionRequest request = new CatalogUnitConversionRequest(
                itemId, " caja ", "un", new BigDecimal("2"));
        CatalogUnitConversionResult result = new CatalogUnitConversionResult(
                itemId,
                request.sourceUnitCode(),
                request.targetUnitCode(),
                request.quantity(),
                new BigDecimal("12"),
                new BigDecimal("24"),
                7);

        assertEquals("CAJA", request.sourceUnitCode());
        assertEquals(new BigDecimal("12"), result.factor());
        assertEquals(7, result.itemVersion());
    }

    @Test
    void conversionRejectsNonPositiveQuantitiesAndFactors() {
        CatalogItemId itemId = new CatalogItemId(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> new CatalogUnitConversionRequest(
                itemId, "UN", "CJ", BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new CatalogUnitConversionResult(
                itemId, "UN", "CJ", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, 0));
    }

    @Test
    void quoteContractCapturesListPolicyAndValidity() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        CatalogPriceQuote quote = new CatalogPriceQuote(
                new PriceListId(UUID.randomUUID()),
                new PriceEntryId(UUID.randomUUID()),
                new CatalogItemId(UUID.randomUUID()),
                "pyg",
                CatalogTaxMode.TAX_INCLUDED,
                "un",
                new BigDecimal("3"),
                new BigDecimal("12500"),
                new BigDecimal("37500"),
                from,
                Optional.empty(),
                2);

        assertEquals("PYG", quote.currency());
        assertEquals("UN", quote.unitCode());
        assertEquals(CatalogTaxMode.TAX_INCLUDED, quote.taxMode());
    }

    @Test
    void quoteRejectsInvalidValidityOrAmounts() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> new CatalogPriceQuote(
                new PriceListId(UUID.randomUUID()),
                new PriceEntryId(UUID.randomUUID()),
                new CatalogItemId(UUID.randomUUID()),
                "PYG",
                CatalogTaxMode.NET,
                "UN",
                BigDecimal.ONE,
                new BigDecimal("-1"),
                BigDecimal.ZERO,
                from,
                Optional.of(from),
                0));
    }
}
