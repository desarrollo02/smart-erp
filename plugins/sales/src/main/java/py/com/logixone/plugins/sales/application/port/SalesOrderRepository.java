package py.com.logixone.plugins.sales.application.port;
import java.util.Optional; import py.com.logixone.kernel.api.company.CompanyId; import py.com.logixone.plugins.sales.api.SalesOrderId; import py.com.logixone.plugins.sales.domain.SalesOrder;
public interface SalesOrderRepository { Optional<SalesOrder> findById(CompanyId companyId,SalesOrderId id); Optional<SalesOrder> findByNumber(CompanyId companyId,String number); SalesOrder insert(SalesOrder order); SalesOrder update(SalesOrder order,long expectedPersistedVersion); }
