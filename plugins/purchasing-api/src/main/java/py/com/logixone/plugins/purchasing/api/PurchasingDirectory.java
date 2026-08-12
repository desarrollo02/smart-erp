package py.com.logixone.plugins.purchasing.api;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

/** Public lookups by opaque identity, always scoped to a trusted company. */
public interface PurchasingDirectory {
    Optional<PurchaseRequestReference> findRequest(CompanyId companyId, PurchaseRequestId requestId);
    Optional<PurchaseOrderReference> findOrder(CompanyId companyId, PurchaseOrderId orderId);
}
