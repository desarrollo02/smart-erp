package py.com.logixone.plugins.purchasing.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;
import py.com.logixone.plugins.purchasing.application.port.PurchasingIdGenerator;

@ApplicationScoped
public class UuidPurchasingIdGenerator implements PurchasingIdGenerator {
    @Override public PurchaseRequestId nextRequestId() { return new PurchaseRequestId(UUID.randomUUID()); }
    @Override public PurchaseOrderId nextOrderId() { return new PurchaseOrderId(UUID.randomUUID()); }
    @Override public GoodsReceiptId nextReceiptId() { return new GoodsReceiptId(UUID.randomUUID()); }
    @Override public SupplierReturnId nextSupplierReturnId() { return new SupplierReturnId(UUID.randomUUID()); }
}
