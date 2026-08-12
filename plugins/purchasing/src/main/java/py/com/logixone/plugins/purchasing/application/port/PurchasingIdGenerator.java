package py.com.logixone.plugins.purchasing.application.port;

import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;

public interface PurchasingIdGenerator {
    PurchaseRequestId nextRequestId();
    PurchaseOrderId nextOrderId();
    GoodsReceiptId nextReceiptId();
    SupplierReturnId nextSupplierReturnId();
}
