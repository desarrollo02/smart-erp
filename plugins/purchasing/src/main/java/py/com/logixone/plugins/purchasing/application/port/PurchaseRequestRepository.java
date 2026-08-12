package py.com.logixone.plugins.purchasing.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.domain.PurchaseRequest;

public interface PurchaseRequestRepository {
    Optional<PurchaseRequest> findById(CompanyId companyId, PurchaseRequestId requestId);
    Optional<PurchaseRequest> findByNumber(CompanyId companyId, String number);
    PurchaseRequest insert(PurchaseRequest request);
    PurchaseRequest update(PurchaseRequest request, long expectedPersistedVersion);
}
