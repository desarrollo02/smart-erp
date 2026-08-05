package py.com.logixone.plugins.referencedata.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReferenceDataContractTest {

    @Test
    void normalizesStableCountryAndCurrencyCodes() {
        assertEquals("PY", new CountryCode(" py ").value());
        assertEquals("PYG", new CurrencyCode(" pyg ").value());
        assertThrows(IllegalArgumentException.class, () -> new CountryCode("PAR"));
        assertThrows(IllegalArgumentException.class, () -> new CurrencyCode("US"));
    }

    @Test
    void requiresVerifiableReleaseProvenanceAndExplicitCompleteness() {
        var release = new ReferenceDataRelease(
                ReferenceDataCatalog.CURRENCY,
                "six-list-one-2026-08-04",
                "ISO 4217:2015",
                "SIX Financial Information",
                URI.create("https://example.test/list-one.xml"),
                "838dfb991648cf36df939edd5fe3811737962b75a32252847d239cedd1e291c9",
                LocalDate.of(2026, 8, 4),
                CatalogCompleteness.BOOTSTRAP_SUBSET,
                2);

        assertEquals(CatalogCompleteness.BOOTSTRAP_SUBSET, release.completeness());
        assertThrows(IllegalArgumentException.class, () -> new ReferenceDataRelease(
                ReferenceDataCatalog.COUNTRY,
                "release",
                "ISO 3166-1:2020",
                "ISO",
                URI.create("http://example.test/countries"),
                "748f6ff7380c8a50ea9448f068b79e3a1ee31be63207249e8cc89bf1eb969d11",
                LocalDate.of(2026, 8, 4),
                CatalogCompleteness.FULL,
                249));
    }

    @Test
    void validatesNormativeEntryShapes() {
        var country = new CountryReference(
                new CountryCode("PY"), "PRY", "600", "Paraguay", "un-m49-2026-08-04", true);
        var currency = new CurrencyReference(
                new CurrencyCode("PYG"), "600", 0, "Guarani", "six-list-one-2026-08-04", true);

        assertEquals("PRY", country.alpha3Code());
        assertEquals(0, currency.minorUnit());
        assertThrows(IllegalArgumentException.class, () -> new CurrencyReference(
                new CurrencyCode("PYG"), "600", 10, "Guarani", "release", true));
    }
}
