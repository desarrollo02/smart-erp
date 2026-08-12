package py.com.logixone.plugins.purchasing.application;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderReference;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestReference;
import py.com.logixone.plugins.purchasing.application.port.PurchaseOrderRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchaseRequestRepository;
import py.com.logixone.plugins.purchasing.application.port.GoodsReceiptRepository;
import py.com.logixone.plugins.purchasing.application.port.SupplierReturnRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchasingDirectoryRepository;
import py.com.logixone.plugins.purchasing.application.query.PurchasingDirectoryQueries;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;
import py.com.logixone.plugins.purchasing.domain.PurchaseRequest;
import py.com.logixone.plugins.purchasing.domain.GoodsReceipt;
import py.com.logixone.plugins.purchasing.domain.SupplierReturn;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;

public final class PurchasingQueryService {
    private final PurchaseRequestRepository requests;
    private final PurchaseOrderRepository orders;
    private final PurchasingDirectoryRepository directory;
    private final GoodsReceiptRepository receipts;
    private final SupplierReturnRepository returns;
    private final PurchasingAuditRecorder audit;

    public PurchasingQueryService(
            PurchaseRequestRepository requests,
            PurchaseOrderRepository orders,
            GoodsReceiptRepository receipts,
            SupplierReturnRepository returns,
            PurchasingDirectoryRepository directory,
            TechnicalAudit technicalAudit,
            Clock clock) {
        this.requests = Objects.requireNonNull(requests, "requests");
        this.orders = Objects.requireNonNull(orders, "orders");
        this.receipts = Objects.requireNonNull(receipts, "receipts");
        this.returns = Objects.requireNonNull(returns, "returns");
        this.directory = Objects.requireNonNull(directory, "directory");
        this.audit = new PurchasingAuditRecorder(technicalAudit, clock);
    }

    public PurchasingOperationResult<PurchaseRequestReference> request(
            PurchasingOperationContext context, PurchaseRequestId id) {
        if (!PurchasingApplicationSupport.authorized(context, PurchasingPermissions.VIEW)) {
            return audit.rejected(context, PurchasingPermissions.VIEW,
                    "VIEW_PURCHASE_REQUEST", "purchase_request",
                    Optional.of(id.toString()), Optional.empty(),
                    PurchasingResultCode.ACCESS_DENIED);
        }
        Optional<PurchaseRequest> request = requests.findById(
                context.companyContext().companyId(), id);
        if (request.isEmpty()) {
            return audit.rejected(context, PurchasingPermissions.VIEW,
                    "VIEW_PURCHASE_REQUEST", "purchase_request",
                    Optional.of(id.toString()), Optional.empty(), PurchasingResultCode.NOT_FOUND);
        }
        PurchaseRequest value = request.orElseThrow();
        audit.unchanged(context, PurchasingPermissions.VIEW, "VIEW_PURCHASE_REQUEST",
                "purchase_request", id.toString(), value.version());
        return PurchasingOperationResult.success(value.reference());
    }

    public PurchasingOperationResult<PurchaseOrderReference> order(
            PurchasingOperationContext context, PurchaseOrderId id) {
        if (!PurchasingApplicationSupport.authorized(context, PurchasingPermissions.VIEW)) {
            return audit.rejected(context, PurchasingPermissions.VIEW,
                    "VIEW_PURCHASE_ORDER", "purchase_order",
                    Optional.of(id.toString()), Optional.empty(),
                    PurchasingResultCode.ACCESS_DENIED);
        }
        Optional<PurchaseOrder> order = orders.findById(
                context.companyContext().companyId(), id);
        if (order.isEmpty()) {
            return audit.rejected(context, PurchasingPermissions.VIEW,
                    "VIEW_PURCHASE_ORDER", "purchase_order",
                    Optional.of(id.toString()), Optional.empty(), PurchasingResultCode.NOT_FOUND);
        }
        PurchaseOrder value = order.orElseThrow();
        audit.unchanged(context, PurchasingPermissions.VIEW, "VIEW_PURCHASE_ORDER",
                "purchase_order", id.toString(), value.version());
        return PurchasingOperationResult.success(value.reference());
    }

    public PurchasingOperationResult<PurchaseRequest> requestDetail(
            PurchasingOperationContext context, PurchaseRequestId id) {
        return detail(context, "VIEW_PURCHASE_REQUEST_DETAIL", "purchase_request",
                id.toString(), () -> requests.findById(
                        context.companyContext().companyId(), id));
    }

    public PurchasingOperationResult<PurchaseOrder> orderDetail(
            PurchasingOperationContext context, PurchaseOrderId id) {
        return detail(context, "VIEW_PURCHASE_ORDER_DETAIL", "purchase_order",
                id.toString(), () -> orders.findById(
                        context.companyContext().companyId(), id));
    }

    public PurchasingOperationResult<GoodsReceipt> receiptDetail(
            PurchasingOperationContext context, GoodsReceiptId id) {
        return detail(context, "VIEW_GOODS_RECEIPT_DETAIL", "goods_receipt",
                id.toString(), () -> receipts.findById(
                        context.companyContext().companyId(), id));
    }

    public PurchasingOperationResult<SupplierReturn> returnDetail(
            PurchasingOperationContext context, SupplierReturnId id) {
        return detail(context, "VIEW_SUPPLIER_RETURN_DETAIL", "supplier_return",
                id.toString(), () -> returns.findById(
                        context.companyContext().companyId(), id));
    }

    public PurchasingOperationResult<PurchasingDirectoryQueries.Page<
            PurchasingDirectoryQueries.RequestSummary>> requests(
            PurchasingOperationContext context,
            PurchasingDirectoryQueries.RequestCriteria criteria) {
        return directory(context, "SEARCH_PURCHASE_REQUESTS", "purchase_request_directory",
                () -> directory.requests(context.companyContext().companyId(), criteria));
    }

    public PurchasingOperationResult<PurchasingDirectoryQueries.Page<
            PurchasingDirectoryQueries.OrderSummary>> orders(
            PurchasingOperationContext context,
            PurchasingDirectoryQueries.OrderCriteria criteria) {
        return directory(context, "SEARCH_PURCHASE_ORDERS", "purchase_order_directory",
                () -> directory.orders(context.companyContext().companyId(), criteria));
    }

    public PurchasingOperationResult<PurchasingDirectoryQueries.Page<
            PurchasingDirectoryQueries.ReceiptSummary>> receipts(
            PurchasingOperationContext context,
            PurchasingDirectoryQueries.ReceiptCriteria criteria) {
        return directory(context, "SEARCH_GOODS_RECEIPTS", "goods_receipt_directory",
                () -> directory.receipts(context.companyContext().companyId(), criteria));
    }

    public PurchasingOperationResult<PurchasingDirectoryQueries.Page<
            PurchasingDirectoryQueries.ReturnSummary>> returns(
            PurchasingOperationContext context,
            PurchasingDirectoryQueries.ReturnCriteria criteria) {
        return directory(context, "SEARCH_SUPPLIER_RETURNS", "supplier_return_directory",
                () -> directory.returns(context.companyContext().companyId(), criteria));
    }

    private <T> PurchasingOperationResult<T> directory(
            PurchasingOperationContext context, String operation, String resource,
            java.util.function.Supplier<T> loader) {
        if (!PurchasingApplicationSupport.authorized(context, PurchasingPermissions.VIEW)) {
            return audit.rejected(context, PurchasingPermissions.VIEW, operation, resource,
                    Optional.empty(), Optional.empty(), PurchasingResultCode.ACCESS_DENIED);
        }
        try {
            T value = loader.get();
            audit.unchanged(context, PurchasingPermissions.VIEW, operation, resource,
                    context.companyContext().companyId().toString(), 0);
            return PurchasingOperationResult.success(value);
        } catch (RuntimeException failure) {
            return audit.rejected(context, PurchasingPermissions.VIEW, operation, resource,
                    Optional.empty(), Optional.empty(), PurchasingResultCode.STORAGE_FAILURE);
        }
    }

    private <T> PurchasingOperationResult<T> detail(
            PurchasingOperationContext context, String operation, String resource,
            String resourceId, java.util.function.Supplier<Optional<T>> loader) {
        if (!PurchasingApplicationSupport.authorized(context, PurchasingPermissions.VIEW)) {
            return audit.rejected(context, PurchasingPermissions.VIEW, operation, resource,
                    Optional.of(resourceId), Optional.empty(), PurchasingResultCode.ACCESS_DENIED);
        }
        Optional<T> value = loader.get();
        if (value.isEmpty()) {
            return audit.rejected(context, PurchasingPermissions.VIEW, operation, resource,
                    Optional.of(resourceId), Optional.empty(), PurchasingResultCode.NOT_FOUND);
        }
        audit.unchanged(context, PurchasingPermissions.VIEW, operation, resource,
                resourceId, 0);
        return PurchasingOperationResult.success(value.orElseThrow());
    }
}
