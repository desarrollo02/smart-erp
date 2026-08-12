package py.com.logixone.plugins.purchasing.application;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerDirectory;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversions;
import py.com.logixone.plugins.purchasing.api.OpenPurchaseOrderImport;
import py.com.logixone.plugins.purchasing.api.OpenPurchaseRequestImport;
import py.com.logixone.plugins.purchasing.api.PurchaseImportLine;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderReference;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestReference;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestState;
import py.com.logixone.plugins.purchasing.application.command.PurchasingCommands.ItemInput;
import py.com.logixone.plugins.purchasing.application.port.PurchaseOrderRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchaseRequestRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchasingIdGenerator;
import py.com.logixone.plugins.purchasing.application.port.PurchasingImportRecord;
import py.com.logixone.plugins.purchasing.application.port.PurchasingImportRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchasingOperationRepository;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;
import py.com.logixone.plugins.purchasing.domain.PurchaseRequest;
import py.com.logixone.plugins.purchasing.domain.PurchasedItemSnapshot;
import py.com.logixone.plugins.purchasing.domain.SupplierSnapshot;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;

public final class PurchasingImportService extends PurchasingApplicationService {
    private final PurchaseRequestRepository requests;
    private final PurchaseOrderRepository orders;
    private final PurchasingImportRepository imports;
    private final PurchasingIdGenerator ids;
    private final PurchasingReferenceResolver references;

    public PurchasingImportService(
            PurchaseRequestRepository requests,
            PurchaseOrderRepository orders,
            PurchasingImportRepository imports,
            PurchasingOperationRepository operations,
            PurchasingIdGenerator ids,
            BusinessPartnerDirectory partners,
            CatalogItemDirectory catalog,
            CatalogUnitConversions conversions,
            ReferenceDataDirectory referenceData,
            TechnicalAudit audit,
            Clock clock) {
        super(operations, audit, clock);
        this.requests = java.util.Objects.requireNonNull(requests, "requests");
        this.orders = java.util.Objects.requireNonNull(orders, "orders");
        this.imports = java.util.Objects.requireNonNull(imports, "imports");
        this.ids = java.util.Objects.requireNonNull(ids, "ids");
        this.references = new PurchasingReferenceResolver(
                partners, catalog, conversions, referenceData);
    }

    public PurchasingOperationResult<PurchaseRequestReference> importRequest(
            PurchasingOperationContext context, OpenPurchaseRequestImport command) {
        return importDocument(context, "IMPORT_PURCHASE_REQUEST", "purchase_request",
                "PURCHASE_REQUEST", command, command.source().sourceSystem(),
                command.source().sourceRecordKey(), command.source().batchChecksum(),
                id -> requests.findById(context.companyContext().companyId(),
                                new PurchaseRequestId(id))
                        .map(PurchaseRequest::reference),
                () -> importRequestNew(context, command));
    }

    public PurchasingOperationResult<PurchaseOrderReference> importOrder(
            PurchasingOperationContext context, OpenPurchaseOrderImport command) {
        return importDocument(context, "IMPORT_PURCHASE_ORDER", "purchase_order",
                "PURCHASE_ORDER", command, command.source().sourceSystem(),
                command.source().sourceRecordKey(), command.source().batchChecksum(),
                id -> orders.findById(context.companyContext().companyId(),
                                new PurchaseOrderId(id))
                        .map(PurchaseOrder::reference),
                () -> importOrderNew(context, command));
    }

    private PurchaseRequestReference importRequestNew(
            PurchasingOperationContext context, OpenPurchaseRequestImport command) {
        CompanyId companyId = context.companyContext().companyId();
        AppUserId legacyRequester = syntheticActor(
                command.source().sourceSystem(), command.source().sourceRecordKey());
        List<PurchaseRequest.Line> lines = command.lines().stream()
                .map(line -> new PurchaseRequest.Line(
                        new PurchaseRequestLineId(stableUuid(
                                command.source().sourceSystem(),
                                command.source().sourceRecordKey(), line.sourceLineKey())),
                        importedItem(companyId, line), line.quantity(),
                        line.expectedUnitPrice().map(amount -> new PurchaseRequest.ExpectedPrice(
                                amount, references.currency(companyId, new CurrencyCode(
                                        command.expectedCurrencyCode().orElseThrow()))))))
                .toList();
        PurchaseRequest request = PurchaseRequest.draft(
                companyId, ids.nextRequestId(), command.number(), legacyRequester,
                command.requestedOn(), lines);
        requests.insert(request);
        if (command.state() == PurchaseRequestState.SUBMITTED
                || command.state() == PurchaseRequestState.APPROVED) {
            long previous = request.version();
            request.submit(legacyRequester, clock.instant(), previous);
            requests.update(request, previous);
        }
        if (command.state() == PurchaseRequestState.APPROVED) {
            long previous = request.version();
            request.approve(context.companyContext().actor().userId(),
                    clock.instant(), previous);
            requests.update(request, previous);
        }
        return request.reference();
    }

    private PurchaseOrderReference importOrderNew(
            PurchasingOperationContext context, OpenPurchaseOrderImport command) {
        CompanyId companyId = context.companyContext().companyId();
        BusinessPartnerId supplierId = BusinessPartnerId.parse(command.supplierId());
        SupplierSnapshot currentSupplier = references.supplier(companyId, supplierId);
        SupplierSnapshot supplier = new SupplierSnapshot(
                supplierId, command.supplierCode(), command.supplierDisplayName(),
                currentSupplier.sourceVersion());
        var currency = references.currency(companyId, new CurrencyCode(command.currencyCode()));
        if (currency.minorUnit() != command.currencyMinorUnit()) {
            throw new PurchasingReferenceResolver.ReferenceFailure();
        }
        List<PurchaseOrder.LineDraft> lines = command.lines().stream()
                .map(line -> new PurchaseOrder.LineDraft(
                        new PurchaseOrderLineId(stableUuid(
                                command.source().sourceSystem(),
                                command.source().sourceRecordKey(), line.sourceLineKey())),
                        importedItem(companyId, line), line.quantity(),
                        line.expectedUnitPrice().orElseThrow(), List.of()))
                .toList();
        PurchaseOrder order = PurchaseOrder.draft(
                companyId, ids.nextOrderId(), command.number(), supplier, currency, lines,
                Optional.of("Imported open legacy order from "
                        + command.source().sourceSystem()));
        orders.insert(order);
        if (command.state() == PurchaseOrderState.ISSUED) {
            long previous = order.version();
            order.issue(context.companyContext().actor().userId(),
                    command.issuedOn().orElseThrow().atStartOfDay(
                            java.time.ZoneOffset.UTC).toInstant(), previous);
            orders.update(order, previous);
        }
        return order.reference();
    }

    private PurchasedItemSnapshot importedItem(CompanyId companyId, PurchaseImportLine line) {
        var resolved = references.item(companyId, new ItemInput(
                line.kind(), line.catalogItemId().map(CatalogItemId::parse),
                line.description(), line.unitCode()));
        return new PurchasedItemSnapshot(
                resolved.catalogItemId(), resolved.catalogCode(), line.description(),
                resolved.presentedUnitCode(), resolved.baseUnitCode(),
                resolved.conversionFactor(), resolved.kind(), resolved.sourceVersion());
    }

    private <T> PurchasingOperationResult<T> importDocument(
            PurchasingOperationContext context,
            String operation,
            String resourceType,
            String documentType,
            Object command,
            String sourceSystem,
            String sourceRecordKey,
            Optional<String> batchChecksum,
            java.util.function.Function<UUID, Optional<T>> loader,
            java.util.function.Supplier<T> importer) {
        if (!PurchasingApplicationSupport.authorized(
                context, PurchasingPermissions.IMPORTS_EXECUTE)) {
            return audit.rejected(context, PurchasingPermissions.IMPORTS_EXECUTE,
                    operation, resourceType, Optional.empty(), Optional.empty(),
                    PurchasingResultCode.ACCESS_DENIED);
        }
        String fingerprint = PurchasingFingerprint.of(operation, command);
        var previous = imports.find(context.companyContext().companyId(),
                sourceSystem, sourceRecordKey);
        if (previous.isPresent()) {
            var record = previous.orElseThrow();
            if (!record.documentType().equals(documentType)
                    || !record.requestFingerprint().equals(fingerprint)
                    || !record.batchChecksum().equals(batchChecksum)) {
                return audit.rejected(context, PurchasingPermissions.IMPORTS_EXECUTE,
                        operation, resourceType, Optional.of(record.documentId().toString()),
                        Optional.empty(), PurchasingResultCode.IDEMPOTENCY_CONFLICT);
            }
            Optional<T> loaded = loader.apply(record.documentId());
            if (loaded.isEmpty()) {
                return audit.rejected(context, PurchasingPermissions.IMPORTS_EXECUTE,
                        operation, resourceType, Optional.of(record.documentId().toString()),
                        Optional.empty(), PurchasingResultCode.STORAGE_FAILURE);
            }
            audit.unchanged(context, PurchasingPermissions.IMPORTS_EXECUTE,
                    operation, resourceType, record.documentId().toString(), 0);
            return PurchasingOperationResult.success(loaded.orElseThrow());
        }
        try {
            T value = importer.get();
            UUID documentId = switch (value) {
                case PurchaseRequestReference request -> request.id().value();
                case PurchaseOrderReference order -> order.id().value();
                default -> throw new IllegalStateException("Unsupported import result");
            };
            long version = switch (value) {
                case PurchaseRequestReference request -> request.version();
                case PurchaseOrderReference order -> order.version();
                default -> 0;
            };
            imports.append(new PurchasingImportRecord(
                    context.companyContext().companyId(), sourceSystem, sourceRecordKey,
                    batchChecksum, fingerprint, documentType, documentId, clock.instant()));
            audit.changed(context, PurchasingPermissions.IMPORTS_EXECUTE,
                    operation, resourceType, documentId.toString(), Optional.empty(), version);
            return PurchasingOperationResult.success(value);
        } catch (RuntimeException failure) {
            return failure(context, PurchasingPermissions.IMPORTS_EXECUTE,
                    operation, resourceType, Optional.empty(), Optional.empty(), failure);
        }
    }

    private static AppUserId syntheticActor(String sourceSystem, String sourceRecordKey) {
        return new AppUserId(UUID.nameUUIDFromBytes(("purchasing-import-actor|"
                + sourceSystem + "|" + sourceRecordKey).getBytes(StandardCharsets.UTF_8)));
    }

    private static UUID stableUuid(String sourceSystem, String sourceRecordKey, String lineKey) {
        return UUID.nameUUIDFromBytes(("purchasing-import-line|" + sourceSystem + "|"
                + sourceRecordKey + "|" + lineKey).getBytes(StandardCharsets.UTF_8));
    }
}
