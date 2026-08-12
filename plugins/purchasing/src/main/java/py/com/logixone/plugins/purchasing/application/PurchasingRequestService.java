package py.com.logixone.plugins.purchasing.application;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.application.command.PurchasingCommands;
import py.com.logixone.plugins.purchasing.application.port.PurchaseRequestRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchasingIdGenerator;
import py.com.logixone.plugins.purchasing.application.port.PurchasingOperationRepository;
import py.com.logixone.plugins.purchasing.domain.PurchaseRequest;

public final class PurchasingRequestService extends PurchasingApplicationService {
    private static final String RESOURCE = "purchase_request";

    private final PurchaseRequestRepository requests;
    private final PurchasingIdGenerator ids;
    private final PurchasingReferenceResolver references;

    public PurchasingRequestService(
            PurchaseRequestRepository requests,
            PurchasingIdGenerator ids,
            PurchasingOperationRepository operations,
            py.com.logixone.plugins.businesspartners.api.BusinessPartnerDirectory partners,
            py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory catalog,
            py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversions conversions,
            py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory referenceData,
            TechnicalAudit audit,
            Clock clock) {
        super(operations, audit, clock);
        this.requests = java.util.Objects.requireNonNull(requests, "requests");
        this.ids = java.util.Objects.requireNonNull(ids, "ids");
        this.references = new PurchasingReferenceResolver(
                partners, catalog, conversions, referenceData);
    }

    public PurchasingOperationResult<PurchaseRequest> create(
            PurchasingOperationContext context, PurchasingCommands.CreateRequest command) {
        return createOrMutate(context, PurchasingPermissions.REQUESTS_CREATE,
                "CREATE_PURCHASE_REQUEST", command.idempotencyKey(), command, Optional.empty(),
                () -> {
                    CompanyId companyId = context.companyContext().companyId();
                    PurchaseRequest request = PurchaseRequest.draft(
                            companyId, ids.nextRequestId(), command.number(),
                            context.companyContext().actor().userId(), command.requestedOn(),
                            lines(companyId, command.lines()));
                    return requests.insert(request);
                });
    }

    public PurchasingOperationResult<PurchaseRequest> replaceLines(
            PurchasingOperationContext context, PurchasingCommands.ReplaceRequestLines command) {
        return createOrMutate(context, PurchasingPermissions.REQUESTS_CREATE,
                "REPLACE_PURCHASE_REQUEST_LINES", command.idempotencyKey(), command,
                Optional.of(command.requestId()), () -> {
                    PurchaseRequest request = required(
                            context.companyContext().companyId(), command.requestId());
                    long previous = request.version();
                    request.replaceLines(
                            lines(context.companyContext().companyId(), command.lines()),
                            context.companyContext().actor().userId(), command.expectedVersion());
                    return requests.update(request, previous);
                });
    }

    public PurchasingOperationResult<PurchaseRequest> cloneRequest(
            PurchasingOperationContext context, PurchasingCommands.CloneRequest command) {
        return createOrMutate(context, PurchasingPermissions.REQUESTS_CREATE,
                "CLONE_PURCHASE_REQUEST", command.idempotencyKey(), command, Optional.empty(),
                () -> {
                    CompanyId companyId = context.companyContext().companyId();
                    PurchaseRequest source = required(companyId, command.sourceRequestId());
                    if (source.lines().size() != command.newLineIds().size()) {
                        throw new IllegalArgumentException(
                                "newLineIds must match the source line count");
                    }
                    List<PurchaseRequest.Line> cloned = java.util.stream.IntStream
                            .range(0, source.lines().size())
                            .mapToObj(index -> {
                                PurchaseRequest.Line line = source.lines().get(index);
                                return new PurchaseRequest.Line(
                                        command.newLineIds().get(index), line.item(),
                                        line.quantity(), line.expectedPrice());
                            }).toList();
                    return requests.insert(PurchaseRequest.draft(
                            companyId, ids.nextRequestId(), command.number(),
                            context.companyContext().actor().userId(),
                            command.requestedOn(), cloned));
                });
    }

    public PurchasingOperationResult<PurchaseRequest> submit(
            PurchasingOperationContext context, PurchasingCommands.RequestTransition command) {
        return transition(context, PurchasingPermissions.REQUESTS_SUBMIT,
                "SUBMIT_PURCHASE_REQUEST", command, request -> request.submit(
                        context.companyContext().actor().userId(), clock.instant(),
                        command.expectedVersion()));
    }

    public PurchasingOperationResult<PurchaseRequest> approve(
            PurchasingOperationContext context, PurchasingCommands.RequestTransition command) {
        return transition(context, PurchasingPermissions.REQUESTS_APPROVE,
                "APPROVE_PURCHASE_REQUEST", command, request -> request.approve(
                        context.companyContext().actor().userId(), clock.instant(),
                        command.expectedVersion()));
    }

    public PurchasingOperationResult<PurchaseRequest> reject(
            PurchasingOperationContext context, PurchasingCommands.RequestTransition command) {
        return transition(context, PurchasingPermissions.REQUESTS_APPROVE,
                "REJECT_PURCHASE_REQUEST", command, request -> request.reject(
                        context.companyContext().actor().userId(), clock.instant(),
                        command.reason().orElseThrow(), command.expectedVersion()));
    }

    public PurchasingOperationResult<PurchaseRequest> cancel(
            PurchasingOperationContext context, PurchasingCommands.RequestTransition command) {
        return transition(context, PurchasingPermissions.REQUESTS_CREATE,
                "CANCEL_PURCHASE_REQUEST", command, request -> request.cancel(
                        context.companyContext().actor().userId(), clock.instant(),
                        command.reason().orElseThrow(), command.expectedVersion()));
    }

    public Optional<PurchaseRequest> find(CompanyId companyId, PurchaseRequestId id) {
        return requests.findById(companyId, id);
    }

    private PurchasingOperationResult<PurchaseRequest> transition(
            PurchasingOperationContext context,
            ContributionId permission,
            String operation,
            PurchasingCommands.RequestTransition command,
            java.util.function.Consumer<PurchaseRequest> action) {
        return createOrMutate(context, permission, operation, command.idempotencyKey(), command,
                Optional.of(command.requestId()), () -> {
                    PurchaseRequest request = required(
                            context.companyContext().companyId(), command.requestId());
                    long previous = request.version();
                    action.accept(request);
                    return requests.update(request, previous);
                });
    }

    private PurchasingOperationResult<PurchaseRequest> createOrMutate(
            PurchasingOperationContext context,
            ContributionId permission,
            String operation,
            String idempotencyKey,
            Object command,
            Optional<PurchaseRequestId> requestedId,
            java.util.function.Supplier<PurchaseRequest> action) {
        if (!PurchasingApplicationSupport.authorized(context, permission)) {
            return audit.rejected(context, permission, operation, RESOURCE,
                    requestedId.map(Object::toString), Optional.empty(),
                    PurchasingResultCode.ACCESS_DENIED);
        }
        var replay = replay(context, permission, operation, RESOURCE, idempotencyKey,
                command, id -> requests.findById(
                        context.companyContext().companyId(), new PurchaseRequestId(id)),
                PurchaseRequest::version);
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }
        try {
            long previous = requestedId.flatMap(id -> requests.findById(
                            context.companyContext().companyId(), id))
                    .map(PurchaseRequest::version).orElse(-1L);
            PurchaseRequest stored = action.get();
            remember(context, idempotencyKey, operation, command, RESOURCE,
                    stored.id().value(), stored.version());
            audit.changed(context, permission, operation, RESOURCE, stored.id().toString(),
                    previous < 0 ? Optional.empty() : Optional.of(previous), stored.version());
            return PurchasingOperationResult.success(stored);
        } catch (RuntimeException failure) {
            return failure(context, permission, operation, RESOURCE,
                    requestedId.map(Object::toString), Optional.empty(), failure);
        }
    }

    private List<PurchaseRequest.Line> lines(
            CompanyId companyId, List<PurchasingCommands.RequestLineInput> inputs) {
        return inputs.stream().map(input -> new PurchaseRequest.Line(
                input.id(), references.item(companyId, input.item()), input.quantity(),
                input.expectedPrice().map(expected -> new PurchaseRequest.ExpectedPrice(
                        expected.amount(), references.currency(companyId, expected.currencyCode())))))
                .toList();
    }

    private PurchaseRequest required(CompanyId companyId, PurchaseRequestId id) {
        return requests.findById(companyId, id)
                .orElseThrow(PurchasingReferenceResolver.ReferenceFailure::new);
    }
}
