package py.com.logixone.plugins.referencedata.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
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
import py.com.logixone.plugins.referencedata.application.ReferenceDataPermissions;
import py.com.logixone.plugins.referencedata.application.policy.ChangeReferenceDataPolicy;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicy;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicyResult;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicyRevision;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicyUseCases;

class ReferenceDataScreenHandlerTest {

    private static final CompanyId COMPANY = new CompanyId(new UUID(0, 8038));
    private static final AppUserId ACTOR = new AppUserId(new UUID(0, 38));
    private ReferenceDataScreenHandler handler;
    private FakePolicies policies;
    private List<String> requestedPermissions;

    @BeforeEach
    void setUp() {
        handler = new ReferenceDataScreenHandler();
        policies = new FakePolicies();
        handler.directory = new FakeDirectory(policies);
        handler.policies = policies;
        requestedPermissions = new ArrayList<>();
        handler.authorization = (plugin, permission) -> {
            requestedPermissions.add(permission);
            return authorization(permission);
        };
    }

    @Test
    void showsOnlyVerifiedEntriesAndExplicitBootstrapProvenance() {
        ScreenInteraction.Result result = handler.interact(ScreenInteraction.Request.load(Map.of()));

        assertEquals(1, result.table().orElseThrow().rows().size());
        assertEquals(6, result.table().orElseThrow().columns().size());
        assertEquals(50, result.table().orElseThrow().page().orElseThrow().limit());
        assertTrue(result.notices().getFirst().detail().contains("BOOTSTRAP_SUBSET"));
        assertTrue(result.notices().getFirst().detail().contains("SHA-256"));
        assertEquals(ReferenceDataScreenContract.CATALOGS, handler.screenId());
        assertEquals(2, result.options().get(
                ReferenceDataScreenContract.SEARCH_CATALOG).size());
        assertEquals(List.of(ReferenceDataPermissions.POLICY_MANAGE.value()), requestedPermissions);
    }

    @Test
    void filtersOneCatalogOnTheServerAndNeverReturnsMoreThanTheRequestedPage() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(ReferenceDataScreenContract.SEARCH),
                Map.of(
                        ReferenceDataScreenContract.SEARCH_CATALOG,
                        ReferenceDataCatalog.CURRENCY.name(),
                        ReferenceDataScreenContract.SEARCH_TEXT,
                        "dollar"),
                Optional.empty(),
                Optional.empty(),
                Optional.of(new ScreenInteraction.TablePageRequest(0, 50))));

        assertEquals(1, result.table().orElseThrow().rows().size());
        assertEquals("USD", result.table().orElseThrow().rows().getFirst().cells().get(1));
        assertEquals(1, result.table().orElseThrow().total());
    }

    @Test
    void opensEffectivePolicyAndAppendOnlyHistoryWithoutChangingThePublication() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.empty(), Map.of(), Optional.of("COUNTRY:PY"), Optional.empty()));

        assertEquals("COUNTRY:PY", result.detail().orElseThrow().resourceId());
        assertEquals("Habilitada", result.detail().orElseThrow().items().get(4).value());
        assertEquals(0, result.selectedResourceVersion().orElseThrow());
        assertEquals(ReferenceDataScreenContract.HISTORY,
                result.table().orElseThrow().elementId());
        assertTrue(result.table().orElseThrow().rows().isEmpty());
        assertEquals(List.of(ReferenceDataPermissions.POLICY_MANAGE.value()), requestedPermissions);
    }

    @Test
    void revalidatesManagePermissionAndReturnsTheNewVersionAfterAChange() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(ReferenceDataScreenContract.DISABLE_REFERENCE),
                Map.of(), Optional.of("COUNTRY:PY"), Optional.of(0L)));

        assertEquals(List.of(
                ReferenceDataPermissions.POLICY_MANAGE.value(),
                ReferenceDataPermissions.POLICY_MANAGE.value()), requestedPermissions);
        assertEquals(1, result.selectedResourceVersion().orElseThrow());
        assertEquals("Inhabilitada", result.detail().orElseThrow().items().get(4).value());
        assertEquals(1, result.table().orElseThrow().rows().size());
        assertEquals(ScreenInteraction.NoticeLevel.SUCCESS, result.notices().get(1).level());
    }

    @Test
    void failsClosedWhenTheActorCannotRevalidatePolicyManagement() {
        handler.authorization = (plugin, permission) -> {
            if (ReferenceDataPermissions.POLICY_MANAGE.value().equals(permission)) {
                throw new SecurityException("denied");
            }
            return authorization(permission);
        };

        assertThrows(SecurityException.class, () -> handler.interact(new ScreenInteraction.Request(
                Optional.of(ReferenceDataScreenContract.DISABLE_REFERENCE),
                Map.of(), Optional.of("COUNTRY:PY"), Optional.of(0L))));
        assertTrue(policies.values.isEmpty());
    }

    @Test
    void rejectsActionsThatCouldInventOrModifyNormativeCodes() {
        assertThrows(IllegalArgumentException.class, () -> handler.interact(
                new ScreenInteraction.Request(
                        Optional.of(new ScreenElementId("invent_country")),
                        Map.of(), Optional.empty(), Optional.empty())));
    }

    private static AuthorizedCompanyOperation authorization(String permission) {
        return new AuthorizedCompanyOperation(
                new AuthenticatedCompanyContext(new AuthenticatedActor(ACTOR), COMPANY),
                "reference_data", permission, "reference-data-test");
    }

    private static final class FakePolicies implements ReferenceDataPolicyUseCases {

        private final Map<String, ReferenceDataPolicy> values = new HashMap<>();
        private final List<ReferenceDataPolicyRevision> revisions = new ArrayList<>();

        @Override
        public ReferenceDataPolicyResult<ReferenceDataPolicy> current(
                AuthorizedCompanyOperation authorization,
                ReferenceDataCatalog catalog,
                String code) {
            return ReferenceDataPolicyResult.success(values.getOrDefault(
                    key(catalog, code),
                    ReferenceDataPolicy.defaultEnabled(COMPANY, catalog, code)));
        }

        @Override
        public ReferenceDataPolicyResult<ReferenceDataPolicy> change(
                AuthorizedCompanyOperation authorization, ChangeReferenceDataPolicy command) {
            ReferenceDataPolicy current = values.getOrDefault(
                    key(command.catalog(), command.code()),
                    ReferenceDataPolicy.defaultEnabled(
                            COMPANY, command.catalog(), command.code()));
            if (current.version() != command.expectedVersion()) {
                return ReferenceDataPolicyResult.failure(
                        ReferenceDataPolicyResult.Code.VERSION_CONFLICT);
            }
            if (current.enabled() == command.enabled()) {
                return ReferenceDataPolicyResult.success(current);
            }
            ReferenceDataPolicy changed = new ReferenceDataPolicy(
                    COMPANY, command.catalog(), command.code(), command.enabled(),
                    current.version() + 1);
            values.put(key(command.catalog(), command.code()), changed);
            revisions.add(new ReferenceDataPolicyRevision(
                    COMPANY, command.catalog(), command.code(), command.enabled(),
                    changed.version(), ACTOR, authorization.correlationId(),
                    Instant.parse("2026-08-05T15:00:00Z")));
            return ReferenceDataPolicyResult.success(changed);
        }

        @Override
        public ReferenceDataPolicyResult<List<ReferenceDataPolicyRevision>> history(
                AuthorizedCompanyOperation authorization,
                ReferenceDataCatalog catalog,
                String code) {
            return ReferenceDataPolicyResult.success(revisions.stream()
                    .filter(value -> value.catalog() == catalog && value.code().equals(code))
                    .toList());
        }

        private static String key(ReferenceDataCatalog catalog, String code) {
            return catalog.name() + ":" + code;
        }
    }

    private static final class FakeDirectory implements ReferenceDataDirectory {

        private final FakePolicies policies;

        private FakeDirectory(FakePolicies policies) {
            this.policies = policies;
        }

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
                    "un-m49-2026-08-04-bootstrap",
                    policies.values.getOrDefault(
                            "COUNTRY:PY",
                            ReferenceDataPolicy.defaultEnabled(
                                    COMPANY, ReferenceDataCatalog.COUNTRY, "PY"))
                            .enabled()));
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
