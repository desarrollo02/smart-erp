package py.com.logixone.plugins.purchasing.application.port;

import java.util.List;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;
import py.com.logixone.plugins.purchasing.domain.SupplierReturn;

public interface SupplierReturnRepository {
    Optional<SupplierReturn> findById(CompanyId companyId, SupplierReturnId supplierReturnId);
    Optional<SupplierReturn> findByNumber(CompanyId companyId, String number);
    List<SupplierReturn> findByOrderId(CompanyId companyId, PurchaseOrderId orderId);
    SupplierReturn insert(SupplierReturn supplierReturn);
    SupplierReturn update(SupplierReturn supplierReturn, long expectedPersistedVersion);
}
