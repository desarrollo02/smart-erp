package py.com.logixone.plugins.purchasing.application;

import py.com.logixone.plugins.purchasing.api.OpenPurchaseOrderImport;
import py.com.logixone.plugins.purchasing.api.OpenPurchaseRequestImport;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderReference;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestReference;
import py.com.logixone.plugins.purchasing.application.command.PurchasingCommands;
import py.com.logixone.plugins.purchasing.application.query.PurchasingDirectoryQueries;
import py.com.logixone.plugins.purchasing.domain.GoodsReceipt;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;
import py.com.logixone.plugins.purchasing.domain.PurchaseRequest;
import py.com.logixone.plugins.purchasing.domain.SupplierReturn;

public interface PurchasingUseCases {
    PurchasingOperationResult<PurchaseRequest> createRequest(
            PurchasingOperationContext context, PurchasingCommands.CreateRequest command);
    PurchasingOperationResult<PurchaseRequest> replaceRequestLines(
            PurchasingOperationContext context, PurchasingCommands.ReplaceRequestLines command);
    PurchasingOperationResult<PurchaseRequest> cloneRequest(
            PurchasingOperationContext context, PurchasingCommands.CloneRequest command);
    PurchasingOperationResult<PurchaseRequest> submitRequest(
            PurchasingOperationContext context, PurchasingCommands.RequestTransition command);
    PurchasingOperationResult<PurchaseRequest> approveRequest(
            PurchasingOperationContext context, PurchasingCommands.RequestTransition command);
    PurchasingOperationResult<PurchaseRequest> rejectRequest(
            PurchasingOperationContext context, PurchasingCommands.RequestTransition command);
    PurchasingOperationResult<PurchaseRequest> cancelRequest(
            PurchasingOperationContext context, PurchasingCommands.RequestTransition command);

    PurchasingOperationResult<PurchaseOrder> createOrder(
            PurchasingOperationContext context, PurchasingCommands.CreateOrder command);
    PurchasingOperationResult<PurchaseOrder> addOrderLine(
            PurchasingOperationContext context, PurchasingCommands.AddOrderLine command);
    PurchasingOperationResult<PurchaseOrder> issueOrder(
            PurchasingOperationContext context, PurchasingCommands.OrderTransition command);
    PurchasingOperationResult<PurchaseOrder> cancelOrder(
            PurchasingOperationContext context, PurchasingCommands.OrderTransition command);
    PurchasingOperationResult<PurchaseOrder> closeOrderShort(
            PurchasingOperationContext context, PurchasingCommands.OrderTransition command);

    PurchasingOperationResult<GoodsReceipt> createReceipt(
            PurchasingOperationContext context, PurchasingCommands.CreateReceipt command);
    PurchasingOperationResult<GoodsReceipt> confirmReceipt(
            PurchasingOperationContext context, PurchasingCommands.ConfirmReceipt command);
    PurchasingOperationResult<SupplierReturn> createSupplierReturn(
            PurchasingOperationContext context, PurchasingCommands.CreateSupplierReturn command);
    PurchasingOperationResult<SupplierReturn> confirmSupplierReturn(
            PurchasingOperationContext context, PurchasingCommands.ConfirmSupplierReturn command);

    PurchasingOperationResult<PurchaseRequestReference> request(
            PurchasingOperationContext context, PurchaseRequestId id);
    PurchasingOperationResult<PurchaseOrderReference> order(
            PurchasingOperationContext context, PurchaseOrderId id);
    PurchasingOperationResult<PurchaseRequest> requestDetail(
            PurchasingOperationContext context, PurchaseRequestId id);
    PurchasingOperationResult<PurchaseOrder> orderDetail(
            PurchasingOperationContext context, PurchaseOrderId id);
    PurchasingOperationResult<GoodsReceipt> receiptDetail(
            PurchasingOperationContext context, GoodsReceiptId id);
    PurchasingOperationResult<SupplierReturn> returnDetail(
            PurchasingOperationContext context, SupplierReturnId id);
    PurchasingOperationResult<PurchasingDirectoryQueries.Page<
            PurchasingDirectoryQueries.RequestSummary>> searchRequests(
            PurchasingOperationContext context, PurchasingDirectoryQueries.RequestCriteria criteria);
    PurchasingOperationResult<PurchasingDirectoryQueries.Page<
            PurchasingDirectoryQueries.OrderSummary>> searchOrders(
            PurchasingOperationContext context, PurchasingDirectoryQueries.OrderCriteria criteria);
    PurchasingOperationResult<PurchasingDirectoryQueries.Page<
            PurchasingDirectoryQueries.ReceiptSummary>> searchReceipts(
            PurchasingOperationContext context, PurchasingDirectoryQueries.ReceiptCriteria criteria);
    PurchasingOperationResult<PurchasingDirectoryQueries.Page<
            PurchasingDirectoryQueries.ReturnSummary>> searchReturns(
            PurchasingOperationContext context, PurchasingDirectoryQueries.ReturnCriteria criteria);
    PurchasingOperationResult<PurchaseRequestReference> importRequest(
            PurchasingOperationContext context, OpenPurchaseRequestImport command);
    PurchasingOperationResult<PurchaseOrderReference> importOrder(
            PurchasingOperationContext context, OpenPurchaseOrderImport command);
}
