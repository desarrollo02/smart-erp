package py.com.logixone.plugins.referencedata.application.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.kernel.api.security.AuthorizedCompanyOperation;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;
import py.com.logixone.plugins.referencedata.application.ReferenceDataPermissions;

class ReferenceDataPolicyServiceTest {

    private static final CompanyId COMPANY = new CompanyId(new UUID(0, 4600));
    private static final AppUserId ACTOR = new AppUserId(new UUID(0, 4601));
    private static final Instant NOW = Instant.parse("2026-08-05T15:00:00Z");

    private FakeRepository repository;
    private List<TechnicalAuditEvent> audit;
    private ReferenceDataPolicyService service;

    @BeforeEach
    void setUp() {
        repository = new FakeRepository();
        repository.known.add("COUNTRY:PY");
        audit = new ArrayList<>();
        service = new ReferenceDataPolicyService(
                repository, audit::add, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsTheFirstOverrideFromTheEffectiveEnabledDefault() {
        var result = service.change(manage(), new ChangeReferenceDataPolicy(
                ReferenceDataCatalog.COUNTRY, "py", false, 0));

        assertTrue(result.successful());
        assertFalse(result.value().orElseThrow().enabled());
        assertEquals(1, result.value().orElseThrow().version());
        assertEquals(1, repository.history(COMPANY, ReferenceDataCatalog.COUNTRY, "PY").size());
        assertEquals("CHANGED", audit.getFirst().outcome().name());
        assertEquals(Optional.of(0L), audit.getFirst().previousVersion());
        assertEquals(Optional.of(1L), audit.getFirst().resultingVersion());
    }

    @Test
    void treatsTheEffectiveDefaultAsAnIdempotentUnchangedPolicy() {
        var result = service.change(manage(), new ChangeReferenceDataPolicy(
                ReferenceDataCatalog.COUNTRY, "PY", true, 0));

        assertTrue(result.successful());
        assertEquals(0, result.value().orElseThrow().version());
        assertTrue(repository.values.isEmpty());
        assertEquals("UNCHANGED", audit.getFirst().outcome().name());
    }

    @Test
    void rejectsUnknownCodesWrongPermissionsAndStaleVersions() {
        assertEquals(ReferenceDataPolicyResult.Code.NOT_FOUND,
                service.change(manage(), new ChangeReferenceDataPolicy(
                        ReferenceDataCatalog.COUNTRY, "AR", false, 0)).code());
        assertEquals(ReferenceDataPolicyResult.Code.ACCESS_DENIED,
                service.change(view(), new ChangeReferenceDataPolicy(
                        ReferenceDataCatalog.COUNTRY, "PY", false, 0)).code());

        repository.values.put("COUNTRY:PY", new ReferenceDataPolicy(
                COMPANY, ReferenceDataCatalog.COUNTRY, "PY", false, 3));
        assertEquals(ReferenceDataPolicyResult.Code.VERSION_CONFLICT,
                service.change(manage(), new ChangeReferenceDataPolicy(
                        ReferenceDataCatalog.COUNTRY, "PY", true, 2)).code());
        assertEquals(3, repository.values.get("COUNTRY:PY").version());
    }

    @Test
    void returnsAppendOnlyHistoryToViewOrManagePermission() {
        service.change(manage(), new ChangeReferenceDataPolicy(
                ReferenceDataCatalog.COUNTRY, "PY", false, 0));

        var result = service.history(view(), ReferenceDataCatalog.COUNTRY, "PY");

        assertTrue(result.successful());
        assertEquals(1, result.value().orElseThrow().size());
        assertEquals(ACTOR, result.value().orElseThrow().getFirst().actorUserId());
    }

    @Test
    void returnsTheEffectiveDefaultOrPersistedPolicyToViewPermission() {
        var defaultPolicy = service.current(view(), ReferenceDataCatalog.COUNTRY, "PY");

        assertTrue(defaultPolicy.successful());
        assertTrue(defaultPolicy.value().orElseThrow().enabled());
        assertEquals(0, defaultPolicy.value().orElseThrow().version());

        repository.values.put("COUNTRY:PY", new ReferenceDataPolicy(
                COMPANY, ReferenceDataCatalog.COUNTRY, "PY", false, 4));
        var persisted = service.current(view(), ReferenceDataCatalog.COUNTRY, "PY");

        assertFalse(persisted.value().orElseThrow().enabled());
        assertEquals(4, persisted.value().orElseThrow().version());
    }

    private static AuthorizedCompanyOperation manage() {
        return authorization(ReferenceDataPermissions.POLICY_MANAGE.value());
    }

    private static AuthorizedCompanyOperation view() {
        return authorization(ReferenceDataPermissions.VIEW.value());
    }

    private static AuthorizedCompanyOperation authorization(String permission) {
        return new AuthorizedCompanyOperation(
                new AuthenticatedCompanyContext(new AuthenticatedActor(ACTOR), COMPANY),
                "reference_data",
                permission,
                "rd04-test");
    }

    private static final class FakeRepository implements ReferenceDataPolicyRepository {

        private final java.util.Set<String> known = new java.util.HashSet<>();
        private final Map<String, ReferenceDataPolicy> values = new HashMap<>();
        private final List<ReferenceDataPolicyRevision> revisions = new ArrayList<>();

        @Override
        public boolean existsInCurrentRelease(ReferenceDataCatalog catalog, String code) {
            return known.contains(key(catalog, code));
        }

        @Override
        public Optional<ReferenceDataPolicy> find(
                CompanyId companyId, ReferenceDataCatalog catalog, String code) {
            return Optional.ofNullable(values.get(key(catalog, code)));
        }

        @Override
        public ReferenceDataPolicy change(
                CompanyId companyId,
                ChangeReferenceDataPolicy command,
                AppUserId actorUserId,
                String correlationId,
                Instant changedAt) {
            String key = key(command.catalog(), command.code());
            ReferenceDataPolicy current = values.getOrDefault(key,
                    ReferenceDataPolicy.defaultEnabled(companyId, command.catalog(), command.code()));
            if (current.version() != command.expectedVersion()) {
                throw new ConcurrentReferenceDataPolicyChangeException();
            }
            ReferenceDataPolicy changed = new ReferenceDataPolicy(
                    companyId, command.catalog(), command.code(), command.enabled(),
                    current.version() + 1);
            values.put(key, changed);
            revisions.add(new ReferenceDataPolicyRevision(
                    companyId, command.catalog(), command.code(), command.enabled(),
                    changed.version(), actorUserId, correlationId, changedAt));
            return changed;
        }

        @Override
        public List<ReferenceDataPolicyRevision> history(
                CompanyId companyId, ReferenceDataCatalog catalog, String code) {
            return revisions.stream()
                    .filter(value -> value.companyId().equals(companyId)
                            && value.catalog() == catalog
                            && value.code().equals(code))
                    .toList();
        }

        private static String key(ReferenceDataCatalog catalog, String code) {
            return catalog.name() + ":" + ReferenceDataPolicy.canonicalCode(catalog, code);
        }
    }
}
