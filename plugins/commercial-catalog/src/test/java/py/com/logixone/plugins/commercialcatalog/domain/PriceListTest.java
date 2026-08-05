package py.com.logixone.plugins.commercialcatalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPriceQuoteRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogTaxMode;
import py.com.logixone.plugins.commercialcatalog.api.PriceEntryId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;

class PriceListTest {

    private static final Instant JANUARY = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FEBRUARY = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant MARCH = Instant.parse("2026-03-01T00:00:00Z");

    @Test
    void fixesCurrencyTaxModeScaleAndRoundingAtCreation() {
        PriceList list = list();

        assertEquals("PYG", list.currency());
        assertEquals(CatalogTaxMode.TAX_INCLUDED, list.taxMode());
        assertEquals(2, list.scale());
        assertEquals(RoundingMode.HALF_UP, list.roundingMode());
        assertThrows(IllegalArgumentException.class, () -> PriceList.create(
                company(), listId(), new PriceListCode("BAD"), new PriceListName("Bad"),
                "ZZZ", CatalogTaxMode.NET, 2, RoundingMode.HALF_UP));
        assertThrows(IllegalArgumentException.class, () -> PriceList.create(
                company(), listId(), new PriceListCode("BAD"), new PriceListName("Bad"),
                "USD", CatalogTaxMode.NET, 7, RoundingMode.HALF_UP));
    }

    @Test
    void quotesTheHighestApplicableQuantityTierWithDeterministicRounding() {
        PriceList list = list();
        list.addEntry(entry(10, "1", "10.005", JANUARY, Optional.empty()), 0);
        list.addEntry(entry(11, "10", "8.445", JANUARY, Optional.empty()), 1);

        var quote = list.quote(new CatalogPriceQuoteRequest(
                list.id(), itemId(), "EA", new BigDecimal("12"), FEBRUARY)).orElseThrow();

        assertEquals(new PriceEntryId(uuid(11)), quote.priceEntryId());
        assertEquals(0, new BigDecimal("8.45").compareTo(quote.unitAmount()));
        assertEquals(0, new BigDecimal("101.40").compareTo(quote.totalAmount()));
        assertEquals("PYG", quote.currency());
        assertEquals(CatalogTaxMode.TAX_INCLUDED, quote.taxMode());
        assertEquals(2, quote.priceListVersion());
    }

    @Test
    void rejectsOverlappingValidityForTheSameScopeButAllowsAnAdjacentPeriodOrTier() {
        PriceList list = list();
        list.addEntry(entry(10, "1", "10", JANUARY, Optional.of(FEBRUARY)), 0);

        assertThrows(IllegalArgumentException.class,
                () -> list.addEntry(entry(11, "1", "11", JANUARY.plusSeconds(10), Optional.of(MARCH)), 1));

        list.addEntry(entry(12, "1", "12", FEBRUARY, Optional.of(MARCH)), 1);
        list.addEntry(entry(13, "10", "9", JANUARY, Optional.of(MARCH)), 2);
        assertEquals(3, list.entries().size());
    }

    @Test
    void preservesEntriesWhenInactivatedAndDetectsStaleChanges() {
        PriceList list = list();
        list.addEntry(entry(10, "1", "10", JANUARY, Optional.empty()), 0);
        list.inactivate(1);

        assertTrue(list.quote(new CatalogPriceQuoteRequest(
                list.id(), itemId(), "EA", BigDecimal.ONE, FEBRUARY)).isEmpty());
        assertEquals(1, list.entries().size());
        ConcurrentCatalogChangeException stale = assertThrows(
                ConcurrentCatalogChangeException.class, () -> list.reactivate(1));
        assertEquals(2, stale.actualVersion());

        list.reactivate(2);
        list.inactivateEntry(new PriceEntryId(uuid(10)), 3);
        assertTrue(list.quote(new CatalogPriceQuoteRequest(
                list.id(), itemId(), "EA", BigDecimal.ONE, FEBRUARY)).isEmpty());
    }

    private static PriceList list() {
        return PriceList.create(
                company(), listId(), new PriceListCode(" retail "), new PriceListName(" Retail "),
                "pyg", CatalogTaxMode.TAX_INCLUDED, 2, RoundingMode.HALF_UP);
    }

    private static PriceEntry entry(
            long id, String minimum, String amount, Instant from, Optional<Instant> until) {
        return PriceEntry.active(
                new PriceEntryId(uuid(id)), itemId(), new UnitCode("EA"),
                new BigDecimal(minimum), new BigDecimal(amount), from, until);
    }

    private static CompanyId company() { return new CompanyId(uuid(1)); }
    private static PriceListId listId() { return new PriceListId(uuid(2)); }
    private static CatalogItemId itemId() { return new CatalogItemId(uuid(3)); }
    private static UUID uuid(long suffix) { return new UUID(0, suffix); }
}
