package py.com.logixone.plugins.sales.api;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

public interface SalesDirectory {
    Optional<SalesQuoteReference> findQuote(CompanyId companyId, SalesQuoteId quoteId);
    Optional<SalesOrderReference> findOrder(CompanyId companyId, SalesOrderId orderId);
}
