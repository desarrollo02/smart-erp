package py.com.logixone.plugins.purchasing.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestState;
import py.com.logixone.plugins.purchasing.application.command.PurchasingCommands;
import py.com.logixone.plugins.purchasing.application.port.PurchaseOrderRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchaseRequestRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchasingIdGenerator;
import py.com.logixone.plugins.purchasing.application.port.PurchasingOperationRepository;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;
import py.com.logixone.plugins.purchasing.domain.PurchaseRequest;

public final class PurchasingOrderService extends PurchasingApplicationService {
    private static final String RESOURCE = "purchase_order";

    private final PurchaseOrderRepository orders;
    private final PurchaseRequestRepository requests;
    private final PurchasingIdGenerator ids;
    private final PurchasingReferenceResolver references;

    public PurchasingOrderService(
            PurchaseOrderRepository orders,
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
        this.orders = java.util.Objects.requireNonNull(orders, "orders");
        this.requests = java.util.Objects.requireNonNull(requests, "requests");
        this.ids = java.util.Objects.requireNonNull(ids, "ids");
        this.references = new PurchasingReferenceResolver(
                partners, catalog, conversions, referenceData);
    }

    public PurchasingOperationResult<PurchaseOrder> create(
            PurchasingOperationContext context, PurchasingCommands.CreateOrder command) {
        return mutate(context, PurchasingPermissions.ORDERS_CREATE,
                "CREATE_PURCHASE_ORDER", command.idempotencyKey(), command, Optional.empty(),
                () -> {
                    CompanyId companyId = context.companyContext().companyId();
                    List<PurchaseOrder.LineDraft> lines = command.lines().stream()
                            .map(input -> line(companyId, input)).toList();
                    PurchaseOrder order = PurchaseOrder.draft(
                            companyId, ids.nextOrderId(), command.number(),
                            references.supplier(companyId, command.supplierId()),
                            references.currency(companyId, command.currencyCode()), lines,
                            command.directOrderJustification());
                    return orders.insert(order);
                });
    }

    public PurchasingOperationResult<PurchaseOrder> issue(
            PurchasingOperationContext context, PurchasingCommands.OrderTransition command) {
        return transition(context, PurchasingPermissions.ORDERS_ISSUE,
                "ISSUE_PURCHASE_ORDER", command, order -> order.issue(
                        context.companyContext().actor().userId(), clock.instant(),
                command.expectedVersion()));
    }

    public PurchasingOperationResult<PurchaseOrder> addLine(
            PurchasingOperationContext context, PurchasingCommands.AddOrderLine command) {
        return mutate(context, PurchasingPermissions.ORDERS_CREATE,
                "ADD_PURCHASE_ORDER_LINE", command.idempotencyKey(), command,
                Optional.of(command.orderId()), () -> {
                    PurchaseOrder order = required(
                            context.companyContext().companyId(), command.orderId());
                    long previous = order.version();
                    order.addLine(line(context.companyContext().companyId(), command.line()),
                            command.expectedVersion());
                    return orders.update(order, previous);
                });
    }

    public PurchasingOperationResult<PurchaseOrder> cancel(
            PurchasingOperationContext context, PurchasingCommands.OrderTransition command) {
        return transition(context, PurchasingPermissions.ORDERS_CLOSE,
                "CANCEL_PURCHASE_ORDER", command, order -> order.cancel(
                        command.reason().orElseThrow(), command.expectedVersion()));
    }

    public PurchasingOperationResult<PurchaseOrder> closeShort(
            PurchasingOperationContext context, PurchasingCommands.OrderTransition command) {
        return transition(context, PurchasingPermissions.ORDERS_CLOSE,
                "CLOSE_PURCHASE_ORDER_SHORT", command, order -> {
                    Map<py.com.logixone.plugins.purchasing.api.PurchaseOrderLineId,
                            java.math.BigDecimal> pending = order.lines().stream()
                            .filter(line -> line.pendingQuantity().signum() > 0)
                            .collect(Collectors.toMap(
                                    PurchaseOrder.LineSnapshot::id,
                                    PurchaseOrder.LineSnapshot::pendingQuantity));
                    order.closeShort(pending, command.reason().orElseThrow(),
                            command.expectedVersion());
                });
    }

    public Optional<PurchaseOrder> find(CompanyId companyId, PurchaseOrderId id) {
        return orders.findById(companyId, id);
    }

    private PurchasingOperationResult<PurchaseOrder> transition(
            PurchasingOperationContext context, ContributionId permission,
            String operation, PurchasingCommands.OrderTransition command,
            java.util.function.Consumer<PurchaseOrder> action) {
        return mutate(context, permission, operation, command.idempotencyKey(), command,
                Optional.of(command.orderId()), () -> {
                    PurchaseOrder order = required(
                            context.companyContext().companyId(), command.orderId());
                    long previous = order.version();
                    action.accept(order);
                    return orders.update(order, previous);
                });
    }

    private PurchasingOperationResult<PurchaseOrder> mutate(
            PurchasingOperationContext context, ContributionId permission,
            String operation, String idempotencyKey, Object command,
            Optional<PurchaseOrderId> requestedId,
            java.util.function.Supplier<PurchaseOrder> action) {
        if (!PurchasingApplicationSupport.authorized(context, permission)) {
            return audit.rejected(context, permission, operation, RESOURCE,
                    requestedId.map(Object::toString), Optional.empty(),
                    PurchasingResultCode.ACCESS_DENIED);
        }
        var replay = replay(context, permission, operation, RESOURCE, idempotencyKey,
                command, id -> orders.findById(
                        context.companyContext().companyId(), new PurchaseOrderId(id)),
                PurchaseOrder::version);
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }
        try {
            long previous = requestedId.flatMap(id -> orders.findById(
                            context.companyContext().companyId(), id))
                    .map(PurchaseOrder::version).orElse(-1L);
            PurchaseOrder stored = action.get();
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

    private PurchaseOrder.LineDraft line(
            CompanyId companyId, PurchasingCommands.OrderLineInput input) {
        var item = references.item(companyId, input.item());
        List<PurchaseOrder.Allocation> allocations = input.allocations().stream()
                .map(allocation -> {
                    PurchaseRequest request = requests.findById(
                                    companyId, allocation.requestId())
                            .orElseThrow(PurchasingReferenceResolver.ReferenceFailure::new);
                    if (request.state() != PurchaseRequestState.APPROVED) {
                        throw new PurchasingReferenceResolver.ReferenceFailure();
                    }
                    PurchaseRequest.Line requestLine = request.lines().stream()
                            .filter(candidate -> candidate.id().equals(allocation.requestLineId()))
                            .findFirst().orElseThrow(
                                    PurchasingReferenceResolver.ReferenceFailure::new);
                    if (requestLine.item().kind() != item.kind()
                            || !requestLine.item().catalogItemId().equals(item.catalogItemId())
                            || !requestLine.item().presentedUnitCode()
                                    .equals(item.presentedUnitCode())
                            || !requestLine.item().baseUnitCode().equals(item.baseUnitCode())
                            || requestLine.item().conversionFactor()
                                    .compareTo(item.conversionFactor()) != 0
                            || (item.catalogItemId().isEmpty()
                                && !requestLine.item().description()
                                        .equals(item.description()))) {
                        throw new PurchasingReferenceResolver.ReferenceFailure();
                    }
                    return new PurchaseOrder.Allocation(
                            allocation.requestId(), allocation.requestLineId(),
                            allocation.quantity());
                }).toList();
        return new PurchaseOrder.LineDraft(
                input.id(), item, input.orderedQuantity(), input.unitPrice(), allocations);
    }

    private PurchaseOrder required(CompanyId companyId, PurchaseOrderId id) {
        return orders.findById(companyId, id)
                .orElseThrow(PurchasingReferenceResolver.ReferenceFailure::new);
    }
}
