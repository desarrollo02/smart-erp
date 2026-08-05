package py.com.logixone.plugins.referencedata.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.kernel.api.security.AuthorizedCompanyOperation;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugins.referencedata.ReferenceDataScreenContract;
import py.com.logixone.plugins.referencedata.api.CatalogCompleteness;
import py.com.logixone.plugins.referencedata.api.CountryCode;
import py.com.logixone.plugins.referencedata.api.CountryReference;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.referencedata.api.CurrencyReference;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;
import py.com.logixone.plugins.referencedata.api.ReferenceDataRelease;

class ReferenceDataScreenHandlerTest {

    private static final CompanyId COMPANY = new CompanyId(new UUID(0, 8038));
    private ReferenceDataScreenHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ReferenceDataScreenHandler();
        handler.directory = new FakeDirectory();
        handler.authorization = (plugin, permission) -> new AuthorizedCompanyOperation(
                new AuthenticatedCompanyContext(
                        new AuthenticatedActor(new AppUserId(new UUID(0, 38))),
                        COMPANY),
                plugin,
                permission,
                "reference-data-test");
    }

    @Test
    void showsOnlyVerifiedEntriesAndExplicitBootstrapProvenance() {
        ScreenInteraction.Result result = handler.interact(ScreenInteraction.Request.load(Map.of()));

        assertEquals(3, result.table().orElseThrow().rows().size());
        assertEquals(6, result.table().orElseThrow().columns().size());
        assertTrue(result.notices().getFirst().detail().contains("BOOTSTRAP_SUBSET"));
        assertTrue(result.notices().getFirst().detail().contains("SHA-256"));
        assertEquals(ReferenceDataScreenContract.CATALOGS, handler.screenId());
        assertTrue(result.options().isEmpty());
    }

    @Test
    void rejectsBrowserMutationsBecausePublicationUpdatesAreDeploymentControlled() {
        assertThrows(IllegalArgumentException.class, () -> handler.interact(
                new ScreenInteraction.Request(
                        Optional.of(new ScreenElementId("invent_country")),
                        Map.of(), Optional.empty(), Optional.empty())));
    }

    private static final class FakeDirectory implements ReferenceDataDirectory {
        @Override
        public ReferenceDataRelease currentRelease(
                CompanyId companyId, ReferenceDataCatalog catalog) {
            return new ReferenceDataRelease(
                    catalog,
                    catalog == ReferenceDataCatalog.COUNTRY
                            ? "un-m49-2026-08-04-bootstrap"
                            : "six-list-one-2026-08-04-bootstrap",
                    catalog == ReferenceDataCatalog.COUNTRY
                            ? "ISO 3166-1:2020 / UN M49" : "ISO 4217:2015",
                    "Primary authority",
                    URI.create("https://example.test/" + catalog.name().toLowerCase()),
                    catalog == ReferenceDataCatalog.COUNTRY
                            ? "748f6ff7380c8a50ea9448f068b79e3a1ee31be63207249e8cc89bf1eb969d11"
                            : "838dfb991648cf36df939edd5fe3811737962b75a32252847d239cedd1e291c9",
                    LocalDate.of(2026, 8, 4),
                    CatalogCompleteness.BOOTSTRAP_SUBSET,
                    catalog == ReferenceDataCatalog.COUNTRY ? 1 : 2);
        }

        @Override
        public List<CountryReference> countries(CompanyId companyId) {
            return List.of(new CountryReference(
                    new CountryCode("PY"), "PRY", "600", "Paraguay",
                    "un-m49-2026-08-04-bootstrap", true));
        }

        @Override
        public Optional<CountryReference> findCountry(CompanyId companyId, CountryCode code) {
            return countries(companyId).stream().filter(value -> value.code().equals(code)).findFirst();
        }

        @Override
        public List<CurrencyReference> currencies(CompanyId companyId) {
            return List.of(
                    new CurrencyReference(new CurrencyCode("PYG"), "600", 0, "Guarani",
                            "six-list-one-2026-08-04-bootstrap", true),
                    new CurrencyReference(new CurrencyCode("USD"), "840", 2, "US Dollar",
                            "six-list-one-2026-08-04-bootstrap", true));
        }

        @Override
        public Optional<CurrencyReference> findCurrency(CompanyId companyId, CurrencyCode code) {
            return currencies(companyId).stream().filter(value -> value.code().equals(code)).findFirst();
        }
    }
}
