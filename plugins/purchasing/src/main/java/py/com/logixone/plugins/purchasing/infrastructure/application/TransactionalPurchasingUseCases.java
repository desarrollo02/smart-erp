package py.com.logixone.plugins.purchasing.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.time.Clock;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversions;
import py.com.logixone.plugins.inventory.api.InventoryPurchaseMovements;
import py.com.logixone.plugins.purchasing.api.OpenPurchaseOrderImport;
import py.com.logixone.plugins.purchasing.api.OpenPurchaseRequestImport;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderReference;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestReference;
import py.com.logixone.plugins.purchasing.application.PurchasingFulfillmentService;
import py.com.logixone.plugins.purchasing.application.PurchasingImportService;
import py.com.logixone.plugins.purchasing.application.PurchasingOperationContext;
import py.com.logixone.plugins.purchasing.application.PurchasingOperationResult;
import py.com.logixone.plugins.purchasing.application.PurchasingOrderService;
import py.com.logixone.plugins.purchasing.application.PurchasingQueryService;
import py.com.logixone.plugins.purchasing.application.PurchasingRequestService;
import py.com.logixone.plugins.purchasing.application.PurchasingUseCases;
import py.com.logixone.plugins.purchasing.application.command.PurchasingCommands;
import py.com.logixone.plugins.purchasing.application.port.GoodsReceiptRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchaseOrderRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchaseRequestRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchasingIdGenerator;
import py.com.logixone.plugins.purchasing.application.port.PurchasingImportRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchasingDirectoryRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchasingOperationRepository;
import py.com.logixone.plugins.purchasing.application.port.SupplierReturnRepository;
import py.com.logixone.plugins.purchasing.domain.GoodsReceipt;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;
import py.com.logixone.plugins.purchasing.domain.PurchaseRequest;
import py.com.logixone.plugins.purchasing.domain.SupplierReturn;
import py.com.logixone.plugins.purchasing.application.query.PurchasingDirectoryQueries;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;

@ApplicationScoped
@Transactional(rollbackOn = RuntimeException.class)
public class TransactionalPurchasingUseCases implements PurchasingUseCases {
    @Inject PurchaseRequestRepository requests;
    @Inject PurchaseOrderRepository orders;
    @Inject GoodsReceiptRepository receipts;
    @Inject SupplierReturnRepository returns;
    @Inject PurchasingOperationRepository operations;
    @Inject PurchasingImportRepository imports;
    @Inject PurchasingDirectoryRepository directory;
    @Inject PurchasingIdGenerator ids;
    @Inject BusinessPartnerDirectory partners;
    @Inject CatalogItemDirectory catalog;
    @Inject CatalogUnitConversions conversions;
    @Inject ReferenceDataDirectory referenceData;
    @Inject InventoryPurchaseMovements inventory;
    @Inject TechnicalAudit audit;
    @Inject TransactionSynchronizationRegistry transactions;

    @Override public PurchasingOperationResult<PurchaseRequest> createRequest(
            PurchasingOperationContext context, PurchasingCommands.CreateRequest command) {
        return mutation(requests().create(context, command));
    }
    @Override public PurchasingOperationResult<PurchaseRequest> replaceRequestLines(
            PurchasingOperationContext context, PurchasingCommands.ReplaceRequestLines command) {
        return mutation(requests().replaceLines(context, command));
    }
    @Override public PurchasingOperationResult<PurchaseRequest> cloneRequest(
            PurchasingOperationContext context, PurchasingCommands.CloneRequest command) {
        return mutation(requests().cloneRequest(context, command));
    }
    @Override public PurchasingOperationResult<PurchaseRequest> submitRequest(
            PurchasingOperationContext context, PurchasingCommands.RequestTransition command) {
        return mutation(requests().submit(context, command));
    }
    @Override public PurchasingOperationResult<PurchaseRequest> approveRequest(
            PurchasingOperationContext context, PurchasingCommands.RequestTransition command) {
        return mutation(requests().approve(context, command));
    }
    @Override public PurchasingOperationResult<PurchaseRequest> rejectRequest(
            PurchasingOperationContext context, PurchasingCommands.RequestTransition command) {
        return mutation(requests().reject(context, command));
    }
    @Override public PurchasingOperationResult<PurchaseRequest> cancelRequest(
            PurchasingOperationContext context, PurchasingCommands.RequestTransition command) {
        return mutation(requests().cancel(context, command));
    }
    @Override public PurchasingOperationResult<PurchaseOrder> createOrder(
            PurchasingOperationContext context, PurchasingCommands.CreateOrder command) {
        return mutation(orders().create(context, command));
    }
    @Override public PurchasingOperationResult<PurchaseOrder> addOrderLine(
            PurchasingOperationContext context, PurchasingCommands.AddOrderLine command) {
        return mutation(orders().addLine(context, command));
    }
    @Override public PurchasingOperationResult<PurchaseOrder> issueOrder(
            PurchasingOperationContext context, PurchasingCommands.OrderTransition command) {
        return mutation(orders().issue(context, command));
    }
    @Override public PurchasingOperationResult<PurchaseOrder> cancelOrder(
            PurchasingOperationContext context, PurchasingCommands.OrderTransition command) {
        return mutation(orders().cancel(context, command));
    }
    @Override public PurchasingOperationResult<PurchaseOrder> closeOrderShort(
            PurchasingOperationContext context, PurchasingCommands.OrderTransition command) {
        return mutation(orders().closeShort(context, command));
    }
    @Override public PurchasingOperationResult<GoodsReceipt> createReceipt(
            PurchasingOperationContext context, PurchasingCommands.CreateReceipt command) {
        return mutation(fulfillment().createReceipt(context, command));
    }
    @Override public PurchasingOperationResult<GoodsReceipt> confirmReceipt(
            PurchasingOperationContext context, PurchasingCommands.ConfirmReceipt command) {
        return mutation(fulfillment().confirmReceipt(context, command));
    }
    @Override public PurchasingOperationResult<SupplierReturn> createSupplierReturn(
            PurchasingOperationContext context, PurchasingCommands.CreateSupplierReturn command) {
        return mutation(fulfillment().createSupplierReturn(context, command));
    }
    @Override public PurchasingOperationResult<SupplierReturn> confirmSupplierReturn(
            PurchasingOperationContext context, PurchasingCommands.ConfirmSupplierReturn command) {
        return mutation(fulfillment().confirmSupplierReturn(context, command));
    }
    @Override @Transactional(TxType.SUPPORTS)
    public PurchasingOperationResult<PurchaseRequestReference> request(
            PurchasingOperationContext context, PurchaseRequestId id) {
        return queries().request(context, id);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public PurchasingOperationResult<PurchaseOrderReference> order(
            PurchasingOperationContext context, PurchaseOrderId id) {
        return queries().order(context, id);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public PurchasingOperationResult<PurchaseRequest> requestDetail(
            PurchasingOperationContext context, PurchaseRequestId id) {
        return queries().requestDetail(context, id);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public PurchasingOperationResult<PurchaseOrder> orderDetail(
            PurchasingOperationContext context, PurchaseOrderId id) {
        return queries().orderDetail(context, id);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public PurchasingOperationResult<GoodsReceipt> receiptDetail(
            PurchasingOperationContext context,
            py.com.logixone.plugins.purchasing.api.GoodsReceiptId id) {
        return queries().receiptDetail(context, id);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public PurchasingOperationResult<SupplierReturn> returnDetail(
            PurchasingOperationContext context,
            py.com.logixone.plugins.purchasing.api.SupplierReturnId id) {
        return queries().returnDetail(context, id);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public PurchasingOperationResult<PurchasingDirectoryQueries.Page<
            PurchasingDirectoryQueries.RequestSummary>> searchRequests(
            PurchasingOperationContext context, PurchasingDirectoryQueries.RequestCriteria criteria) {
        return queries().requests(context, criteria);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public PurchasingOperationResult<PurchasingDirectoryQueries.Page<
            PurchasingDirectoryQueries.OrderSummary>> searchOrders(
            PurchasingOperationContext context, PurchasingDirectoryQueries.OrderCriteria criteria) {
        return queries().orders(context, criteria);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public PurchasingOperationResult<PurchasingDirectoryQueries.Page<
            PurchasingDirectoryQueries.ReceiptSummary>> searchReceipts(
            PurchasingOperationContext context, PurchasingDirectoryQueries.ReceiptCriteria criteria) {
        return queries().receipts(context, criteria);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public PurchasingOperationResult<PurchasingDirectoryQueries.Page<
            PurchasingDirectoryQueries.ReturnSummary>> searchReturns(
            PurchasingOperationContext context, PurchasingDirectoryQueries.ReturnCriteria criteria) {
        return queries().returns(context, criteria);
    }
    @Override public PurchasingOperationResult<PurchaseRequestReference> importRequest(
            PurchasingOperationContext context, OpenPurchaseRequestImport command) {
        return mutation(imports().importRequest(context, command));
    }
    @Override public PurchasingOperationResult<PurchaseOrderReference> importOrder(
            PurchasingOperationContext context, OpenPurchaseOrderImport command) {
        return mutation(imports().importOrder(context, command));
    }

    private PurchasingRequestService requests() {
        return new PurchasingRequestService(requests, ids, operations, partners, catalog,
                conversions, referenceData, audit, Clock.systemUTC());
    }
    private PurchasingOrderService orders() {
        return new PurchasingOrderService(orders, requests, ids, operations, partners,
                catalog, conversions, referenceData, audit, Clock.systemUTC());
    }
    private PurchasingFulfillmentService fulfillment() {
        return new PurchasingFulfillmentService(orders, receipts, returns, ids,
                operations, inventory, audit, Clock.systemUTC());
    }
    private PurchasingImportService imports() {
        return new PurchasingImportService(requests, orders, imports, operations, ids,
                partners, catalog, conversions, referenceData, audit, Clock.systemUTC());
    }
    private PurchasingQueryService queries() {
        return new PurchasingQueryService(
                requests, orders, receipts, returns, directory, audit, Clock.systemUTC());
    }
    private <T> PurchasingOperationResult<T> mutation(PurchasingOperationResult<T> result) {
        if (!result.successful()) {
            transactions.setRollbackOnly();
        }
        return result;
    }
}
