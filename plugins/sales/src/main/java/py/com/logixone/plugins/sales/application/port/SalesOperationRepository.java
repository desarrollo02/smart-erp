package py.com.logixone.plugins.sales.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

public interface SalesOperationRepository {
    Optional<SalesOperationRecord> find(CompanyId companyId, String idempotencyKey);
    void append(SalesOperationRecord operation);
}
