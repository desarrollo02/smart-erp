package py.com.logixone.plugins.purchasing.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;

public interface PurchaseOrderRepository {
    Optional<PurchaseOrder> findById(CompanyId companyId, PurchaseOrderId orderId);
    Optional<PurchaseOrder> findByNumber(CompanyId companyId, String number);
    PurchaseOrder insert(PurchaseOrder order);
    PurchaseOrder update(PurchaseOrder order, long expectedPersistedVersion);
}
