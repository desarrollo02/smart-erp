package py.com.logixone.plugins.purchasing.application.port;

import java.util.List;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.domain.GoodsReceipt;

public interface GoodsReceiptRepository {
    Optional<GoodsReceipt> findById(CompanyId companyId, GoodsReceiptId receiptId);
    Optional<GoodsReceipt> findByNumber(CompanyId companyId, String number);
    List<GoodsReceipt> findByOrderId(CompanyId companyId, PurchaseOrderId orderId);
    GoodsReceipt insert(GoodsReceipt receipt);
    GoodsReceipt update(GoodsReceipt receipt, long expectedPersistedVersion);
}
