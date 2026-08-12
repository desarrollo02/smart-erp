package py.com.logixone.plugins.purchasing.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

public interface PurchasingImportRepository {
    Optional<PurchasingImportRecord> find(
            CompanyId companyId, String sourceSystem, String sourceRecordKey);
    void append(PurchasingImportRecord record);
}
