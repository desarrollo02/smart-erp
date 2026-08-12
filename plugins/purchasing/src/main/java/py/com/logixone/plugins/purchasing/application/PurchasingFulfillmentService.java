package py.com.logixone.plugins.purchasing.application;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.inventory.api.CatalogStockMovementRequest;
import py.com.logixone.plugins.inventory.api.InventoryPurchaseMovements;
import py.com.logixone.plugins.inventory.api.MovementQuantity;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockMovementType;
import py.com.logixone.plugins.inventory.api.StockSourceReference;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptLineId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptState;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;
import py.com.logixone.plugins.purchasing.application.command.PurchasingCommands;
import py.com.logixone.plugins.purchasing.application.port.GoodsReceiptRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchaseOrderRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchasingIdGenerator;
import py.com.logixone.plugins.purchasing.application.port.PurchasingOperationRepository;
import py.com.logixone.plugins.purchasing.application.port.SupplierReturnRepository;
import py.com.logixone.plugins.purchasing.domain.GoodsReceipt;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;
import py.com.logixone.plugins.purchasing.domain.SupplierReturn;

public final class PurchasingFulfillmentService extends PurchasingApplicationService {
    private static final String RECEIPT = "goods_receipt";
    private static final String SUPPLIER_RETURN = "supplier_return";

    private final PurchaseOrderRepository orders;
    private final GoodsReceiptRepository receipts;
    private final SupplierReturnRepository returns;
    private final PurchasingIdGenerator ids;
    private final InventoryPurchaseMovements inventory;

    public PurchasingFulfillmentService(
            PurchaseOrderRepository orders,
            GoodsReceiptRepository receipts,
            SupplierReturnRepository returns,
            PurchasingIdGenerator ids,
            PurchasingOperationRepository operations,
            InventoryPurchaseMovements inventory,
            TechnicalAudit audit,
            Clock clock) {
        super(operations, audit, clock);
        this.orders = java.util.Objects.requireNonNull(orders, "orders");
        this.receipts = java.util.Objects.requireNonNull(receipts, "receipts");
        this.returns = java.util.Objects.requireNonNull(returns, "returns");
        this.ids = java.util.Objects.requireNonNull(ids, "ids");
        this.inventory = java.util.Objects.requireNonNull(inventory, "inventory");
    }

    public PurchasingOperationResult<GoodsReceipt> createReceipt(
            PurchasingOperationContext context, PurchasingCommands.CreateReceipt command) {
        return receiptMutation(context, PurchasingPermissions.RECEIPTS_CREATE,
                "CREATE_GOODS_RECEIPT", command.idempotencyKey(), command,
                Optional.empty(), () -> {
                    CompanyId companyId = context.companyContext().companyId();
                    PurchaseOrder order = requiredOrder(companyId, command.orderId());
                    if (order.state() != PurchaseOrderState.ISSUED) {
                        throw new IllegalStateException("Only issued orders can be received");
                    }
                    List<GoodsReceipt.Line> lines = command.lines().stream()
                            .map(input -> receiptLine(order, input)).toList();
                    return receipts.insert(GoodsReceipt.draft(
                            companyId, ids.nextReceiptId(), command.number(),
                            order.id(), lines));
                });
    }

    public PurchasingOperationResult<GoodsReceipt> confirmReceipt(
            PurchasingOperationContext context, PurchasingCommands.ConfirmReceipt command) {
        return receiptMutation(context, PurchasingPermissions.RECEIPTS_CONFIRM,
                "CONFIRM_GOODS_RECEIPT", command.idempotencyKey(), command,
                Optional.of(command.receiptId()), () -> {
                    CompanyId companyId = context.companyContext().companyId();
                    GoodsReceipt receipt = requiredReceipt(companyId, command.receiptId());
                    PurchaseOrder order = requiredOrder(companyId, receipt.orderId());
                    Map<GoodsReceiptLineId, StockMovementId> movements =
                            postReceiptMovements(companyId, receipt, order);
                    Map<PurchaseOrderLineId, java.math.BigDecimal> quantities =
                            quantities(receipt.lines());
                    long previousOrder = order.version();
                    order.applyReceipt(quantities, command.expectedOrderVersion());
                    orders.update(order, previousOrder);
                    long previousReceipt = receipt.version();
                    receipt.confirm(context.companyContext().actor().userId(), clock.instant(),
                            movements, command.expectedReceiptVersion());
                    return receipts.update(receipt, previousReceipt);
                });
    }

    public PurchasingOperationResult<SupplierReturn> createSupplierReturn(
            PurchasingOperationContext context,
            PurchasingCommands.CreateSupplierReturn command) {
        return returnMutation(context, PurchasingPermissions.RETURNS_CREATE,
                "CREATE_SUPPLIER_RETURN", command.idempotencyKey(), command,
                Optional.empty(), () -> {
                    CompanyId companyId = context.companyContext().companyId();
                    PurchaseOrder order = requiredOrder(companyId, command.orderId());
                    List<SupplierReturn.Line> lines = command.lines().stream()
                            .map(input -> returnLine(companyId, order, input)).toList();
                    return returns.insert(SupplierReturn.draft(
                            companyId, ids.nextSupplierReturnId(), command.number(),
                            order.id(), command.reason(), lines));
                });
    }

    public PurchasingOperationResult<SupplierReturn> confirmSupplierReturn(
            PurchasingOperationContext context,
            PurchasingCommands.ConfirmSupplierReturn command) {
        return returnMutation(context, PurchasingPermissions.RETURNS_CONFIRM,
                "CONFIRM_SUPPLIER_RETURN", command.idempotencyKey(), command,
                Optional.of(command.supplierReturnId()), () -> {
                    CompanyId companyId = context.companyContext().companyId();
                    SupplierReturn supplierReturn = requiredReturn(
                            companyId, command.supplierReturnId());
                    PurchaseOrder order = requiredOrder(companyId, supplierReturn.orderId());
                    Map<py.com.logixone.plugins.purchasing.api.SupplierReturnLineId,
                            StockMovementId> movements = postReturnMovements(
                                    companyId, supplierReturn, order);
                    Map<PurchaseOrderLineId, java.math.BigDecimal> quantities =
                            returnQuantities(supplierReturn.lines());
                    long previousOrder = order.version();
                    order.applyReturn(quantities, command.expectedOrderVersion());
                    orders.update(order, previousOrder);
                    long previousReturn = supplierReturn.version();
                    supplierReturn.confirm(
                            context.companyContext().actor().userId(), clock.instant(), movements,
                            command.expectedReturnVersion());
                    return returns.update(supplierReturn, previousReturn);
                });
    }

    public Optional<GoodsReceipt> findReceipt(CompanyId companyId, GoodsReceiptId id) {
        return receipts.findById(companyId, id);
    }

    public Optional<SupplierReturn> findSupplierReturn(
            CompanyId companyId, SupplierReturnId id) {
        return returns.findById(companyId, id);
    }

    private GoodsReceipt.Line receiptLine(
            PurchaseOrder order, PurchasingCommands.ReceiptLineInput input) {
        PurchaseOrder.LineSnapshot orderLine = order.lines().stream()
                .filter(line -> line.id().equals(input.orderLineId())).findFirst()
                .orElseThrow(PurchasingReferenceResolver.ReferenceFailure::new);
        return new GoodsReceipt.Line(
                input.id(), input.orderLineId(), orderLine.item().kind(), input.quantity(),
                input.warehouseId(), input.locationId(), input.lotCode(),
                input.serialNumber(), input.expiryDate(), input.condition());
    }

    private SupplierReturn.Line returnLine(
            CompanyId companyId, PurchaseOrder order,
            PurchasingCommands.ReturnLineInput input) {
        GoodsReceipt receipt = requiredReceipt(companyId, input.receiptId());
        if (receipt.state() != GoodsReceiptState.CONFIRMED
                || !receipt.orderId().equals(order.id())) {
            throw new PurchasingReferenceResolver.ReferenceFailure();
        }
        GoodsReceipt.Line source = receipt.lines().stream()
                .filter(line -> line.id().equals(input.receiptLineId())
                        && line.orderLineId().equals(input.orderLineId()))
                .findFirst().orElseThrow(PurchasingReferenceResolver.ReferenceFailure::new);
        return new SupplierReturn.Line(
                input.id(), input.receiptId(), input.receiptLineId(), input.orderLineId(),
                source.kind(), input.quantity(), source.warehouseId(), source.locationId(),
                source.lotCode(), source.serialNumber(), source.expiryDate(), source.condition());
    }

    private Map<GoodsReceiptLineId, StockMovementId> postReceiptMovements(
            CompanyId companyId, GoodsReceipt receipt, PurchaseOrder order) {
        Map<GoodsReceiptLineId, StockMovementId> result = new LinkedHashMap<>();
        for (GoodsReceipt.Line line : receipt.lines()) {
            if (line.kind() != PurchaseLineKind.STOCK) {
                continue;
            }
            PurchaseOrder.LineSnapshot orderLine = orderLine(order, line.orderLineId());
            try {
                var movement = inventory.postCatalogItem(companyId,
                        movement(StockMovementType.RECEIPT, "PURCHASE_RECEIPT",
                                "PURCHASING_RECEIPT", receipt.id().toString(),
                                "receipt-line-" + line.id(), orderLine, line.quantity(),
                                line.warehouseId().orElseThrow(),
                                line.locationId().orElseThrow(), line.lotCode(),
                                line.serialNumber(), line.expiryDate(),
                                line.condition().orElseThrow()));
                result.put(line.id(), movement.id());
            } catch (RuntimeException failure) {
                throw new PurchasingInventoryFailure(failure);
            }
        }
        return result;
    }

    private Map<py.com.logixone.plugins.purchasing.api.SupplierReturnLineId, StockMovementId>
            postReturnMovements(
                    CompanyId companyId, SupplierReturn supplierReturn, PurchaseOrder order) {
        Map<py.com.logixone.plugins.purchasing.api.SupplierReturnLineId, StockMovementId> result =
                new LinkedHashMap<>();
        for (SupplierReturn.Line line : supplierReturn.lines()) {
            if (line.kind() != PurchaseLineKind.STOCK) {
                continue;
            }
            PurchaseOrder.LineSnapshot orderLine = orderLine(order, line.orderLineId());
            try {
                var movement = inventory.postCatalogItem(companyId,
                        movement(StockMovementType.ISSUE, "PURCHASE_RETURN",
                                "PURCHASING_RETURN", supplierReturn.id().toString(),
                                "return-line-" + line.id(), orderLine, line.quantity(),
                                line.warehouseId().orElseThrow(),
                                line.locationId().orElseThrow(), line.lotCode(),
                                line.serialNumber(), line.expiryDate(),
                                line.condition().orElseThrow()));
                result.put(line.id(), movement.id());
            } catch (RuntimeException failure) {
                throw new PurchasingInventoryFailure(failure);
            }
        }
        return result;
    }

    private static CatalogStockMovementRequest movement(
            StockMovementType type,
            String reason,
            String sourceType,
            String sourceId,
            String idempotencyKey,
            PurchaseOrder.LineSnapshot orderLine,
            java.math.BigDecimal quantity,
            py.com.logixone.plugins.inventory.api.WarehouseId warehouseId,
            py.com.logixone.plugins.inventory.api.StockLocationId locationId,
            Optional<String> lotCode,
            Optional<String> serialNumber,
            Optional<java.time.LocalDate> expiryDate,
            py.com.logixone.plugins.inventory.api.StockCondition condition) {
        var item = orderLine.item();
        return new CatalogStockMovementRequest(
                type, reason, new StockSourceReference(sourceType, sourceId), idempotencyKey,
                item.catalogItemId().orElseThrow().value(), warehouseId, locationId,
                lotCode, serialNumber, expiryDate, condition,
                new MovementQuantity(
                        item.presentedUnitCode(), quantity, item.baseUnitCode(),
                        item.conversionFactor(), item.toBaseQuantity(quantity),
                        item.sourceVersion()));
    }

    private PurchasingOperationResult<GoodsReceipt> receiptMutation(
            PurchasingOperationContext context, ContributionId permission,
            String operation, String idempotencyKey, Object command,
            Optional<GoodsReceiptId> requestedId,
            java.util.function.Supplier<GoodsReceipt> action) {
        if (!PurchasingApplicationSupport.authorized(context, permission)) {
            return audit.rejected(context, permission, operation, RECEIPT,
                    requestedId.map(Object::toString), Optional.empty(),
                    PurchasingResultCode.ACCESS_DENIED);
        }
        var replay = replay(context, permission, operation, RECEIPT, idempotencyKey,
                command, id -> receipts.findById(
                        context.companyContext().companyId(), new GoodsReceiptId(id)),
                GoodsReceipt::version);
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }
        try {
            long previous = requestedId.flatMap(id -> receipts.findById(
                            context.companyContext().companyId(), id))
                    .map(GoodsReceipt::version).orElse(-1L);
            GoodsReceipt stored = action.get();
            remember(context, idempotencyKey, operation, command, RECEIPT,
                    stored.id().value(), stored.version());
            audit.changed(context, permission, operation, RECEIPT, stored.id().toString(),
                    previous < 0 ? Optional.empty() : Optional.of(previous), stored.version());
            return PurchasingOperationResult.success(stored);
        } catch (RuntimeException failure) {
            return failure(context, permission, operation, RECEIPT,
                    requestedId.map(Object::toString), Optional.empty(), failure);
        }
    }

    private PurchasingOperationResult<SupplierReturn> returnMutation(
            PurchasingOperationContext context, ContributionId permission,
            String operation, String idempotencyKey, Object command,
            Optional<SupplierReturnId> requestedId,
            java.util.function.Supplier<SupplierReturn> action) {
        if (!PurchasingApplicationSupport.authorized(context, permission)) {
            return audit.rejected(context, permission, operation, SUPPLIER_RETURN,
                    requestedId.map(Object::toString), Optional.empty(),
                    PurchasingResultCode.ACCESS_DENIED);
        }
        var replay = replay(context, permission, operation, SUPPLIER_RETURN,
                idempotencyKey, command, id -> returns.findById(
                        context.companyContext().companyId(), new SupplierReturnId(id)),
                SupplierReturn::version);
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }
        try {
            long previous = requestedId.flatMap(id -> returns.findById(
                            context.companyContext().companyId(), id))
                    .map(SupplierReturn::version).orElse(-1L);
            SupplierReturn stored = action.get();
            remember(context, idempotencyKey, operation, command, SUPPLIER_RETURN,
                    stored.id().value(), stored.version());
            audit.changed(context, permission, operation, SUPPLIER_RETURN,
                    stored.id().toString(), previous < 0 ? Optional.empty()
                            : Optional.of(previous), stored.version());
            return PurchasingOperationResult.success(stored);
        } catch (RuntimeException failure) {
            return failure(context, permission, operation, SUPPLIER_RETURN,
                    requestedId.map(Object::toString), Optional.empty(), failure);
        }
    }

    private static Map<PurchaseOrderLineId, java.math.BigDecimal> quantities(
            List<GoodsReceipt.Line> lines) {
        Map<PurchaseOrderLineId, java.math.BigDecimal> values = new LinkedHashMap<>();
        lines.forEach(line -> values.merge(
                line.orderLineId(), line.quantity(), java.math.BigDecimal::add));
        return values;
    }

    private static Map<PurchaseOrderLineId, java.math.BigDecimal> returnQuantities(
            List<SupplierReturn.Line> lines) {
        Map<PurchaseOrderLineId, java.math.BigDecimal> values = new LinkedHashMap<>();
        lines.forEach(line -> values.merge(
                line.orderLineId(), line.quantity(), java.math.BigDecimal::add));
        return values;
    }

    private static PurchaseOrder.LineSnapshot orderLine(
            PurchaseOrder order, PurchaseOrderLineId id) {
        return order.lines().stream().filter(line -> line.id().equals(id)).findFirst()
                .orElseThrow(PurchasingReferenceResolver.ReferenceFailure::new);
    }

    private PurchaseOrder requiredOrder(CompanyId companyId,
            py.com.logixone.plugins.purchasing.api.PurchaseOrderId id) {
        return orders.findById(companyId, id)
                .orElseThrow(PurchasingReferenceResolver.ReferenceFailure::new);
    }

    private GoodsReceipt requiredReceipt(CompanyId companyId, GoodsReceiptId id) {
        return receipts.findById(companyId, id)
                .orElseThrow(PurchasingReferenceResolver.ReferenceFailure::new);
    }

    private SupplierReturn requiredReturn(CompanyId companyId, SupplierReturnId id) {
        return returns.findById(companyId, id)
                .orElseThrow(PurchasingReferenceResolver.ReferenceFailure::new);
    }
}
