package py.com.logixone.plugins.purchasing.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversions;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestLineId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;
import py.com.logixone.plugins.purchasing.application.command.PurchasingCommands;
import py.com.logixone.plugins.purchasing.application.port.PurchaseRequestRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchasingIdGenerator;
import py.com.logixone.plugins.purchasing.application.port.PurchasingOperationRecord;
import py.com.logixone.plugins.purchasing.application.port.PurchasingOperationRepository;
import py.com.logixone.plugins.purchasing.domain.PurchaseRequest;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;

class PurchasingRequestServiceTest {
    private static final CompanyId COMPANY = new CompanyId(uuid(1));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-11T16:00:00Z"), ZoneOffset.UTC);

    private MemoryRequests requests;
    private MemoryOperations operations;
    private CountingIds ids;
    private List<TechnicalAuditEvent> audit;
    private PurchasingRequestService service;

    @BeforeEach
    void setUp() {
        requests = new MemoryRequests();
        operations = new MemoryOperations();
        ids = new CountingIds();
        audit = new ArrayList<>();
        service = new PurchasingRequestService(
                requests, ids, operations,
                unused(BusinessPartnerDirectory.class),
                unused(CatalogItemDirectory.class),
                unused(CatalogUnitConversions.class),
                unused(ReferenceDataDirectory.class), audit::add, CLOCK);
    }

    @Test
    void rejectsBeforeGeneratingIdentityWhenPermissionIsMissing() {
        var result = service.create(context(PurchasingPermissions.VIEW), command("create-1", "SC-1"));

        assertEquals(PurchasingResultCode.ACCESS_DENIED, result.code());
        assertEquals(0, ids.requestCalls);
        assertTrue(requests.values.isEmpty());
    }

    @Test
    void exactRetryReturnsStoredRequestAndConflictingRetryIsRejected() {
        var first = service.create(
                context(PurchasingPermissions.REQUESTS_CREATE), command("create-2", "SC-2"));
        var retry = service.create(
                context(PurchasingPermissions.REQUESTS_CREATE), command("create-2", "SC-2"));
        var conflict = service.create(
                context(PurchasingPermissions.REQUESTS_CREATE), command("create-2", "SC-OTHER"));

        assertTrue(first.successful());
        assertEquals(first.value(), retry.value());
        assertEquals(PurchasingResultCode.IDEMPOTENCY_CONFLICT, conflict.code());
        assertEquals(1, ids.requestCalls);
        assertEquals(1, operations.values.size());
        assertEquals("UNCHANGED", audit.get(1).outcome().name());
    }

    private static PurchasingCommands.CreateRequest command(String key, String number) {
        return new PurchasingCommands.CreateRequest(
                key, number, LocalDate.of(2026, 8, 11), List.of(
                        new PurchasingCommands.RequestLineInput(
                                new PurchaseRequestLineId(uuid(20)),
                                new PurchasingCommands.ItemInput(
                                        PurchaseLineKind.SERVICE, Optional.empty(),
                                        "Servicio técnico", "UN"),
                                BigDecimal.ONE, Optional.empty())));
    }

    private static PurchasingOperationContext context(ContributionId permission) {
        return new PurchasingOperationContext(
                new AuthenticatedCompanyContext(
                        new AuthenticatedActor(new AppUserId(uuid(99))), COMPANY),
                PurchasingIdentity.PLUGIN_ID, permission, "test:purchasing-request");
    }

    @SuppressWarnings("unchecked")
    private static <T> T unused(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, arguments) -> {
                    if (method.getReturnType().equals(Optional.class)) {
                        return Optional.empty();
                    }
                    if (method.getReturnType().equals(List.class)) {
                        return List.of();
                    }
                    throw new AssertionError("Unexpected external lookup: " + method.getName());
                });
    }

    private static UUID uuid(long value) { return new UUID(0, value); }

    private static final class MemoryRequests implements PurchaseRequestRepository {
        private final Map<PurchaseRequestId, PurchaseRequest> values = new LinkedHashMap<>();
        @Override public Optional<PurchaseRequest> findById(CompanyId companyId, PurchaseRequestId id) {
            return Optional.ofNullable(values.get(id));
        }
        @Override public Optional<PurchaseRequest> findByNumber(CompanyId companyId, String number) {
            return values.values().stream().filter(value -> value.reference().number().equals(number)).findFirst();
        }
        @Override public PurchaseRequest insert(PurchaseRequest request) { values.put(request.id(), request); return request; }
        @Override public PurchaseRequest update(PurchaseRequest request, long version) { values.put(request.id(), request); return request; }
    }

    private static final class MemoryOperations implements PurchasingOperationRepository {
        private final Map<String, PurchasingOperationRecord> values = new LinkedHashMap<>();
        @Override public Optional<PurchasingOperationRecord> find(CompanyId companyId, String key) {
            return Optional.ofNullable(values.get(key));
        }
        @Override public void append(PurchasingOperationRecord operation) {
            values.put(operation.idempotencyKey(), operation);
        }
    }

    private static final class CountingIds implements PurchasingIdGenerator {
        private int requestCalls;
        @Override public PurchaseRequestId nextRequestId() { requestCalls++; return new PurchaseRequestId(uuid(10)); }
        @Override public PurchaseOrderId nextOrderId() { return new PurchaseOrderId(uuid(11)); }
        @Override public GoodsReceiptId nextReceiptId() { return new GoodsReceiptId(uuid(12)); }
        @Override public SupplierReturnId nextSupplierReturnId() { return new SupplierReturnId(uuid(13)); }
    }
}
