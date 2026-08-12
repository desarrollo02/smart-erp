package py.com.logixone.plugins.purchasing.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

public interface PurchasingOperationRepository {
    Optional<PurchasingOperationRecord> find(CompanyId companyId, String idempotencyKey);
    void append(PurchasingOperationRecord operation);
}
