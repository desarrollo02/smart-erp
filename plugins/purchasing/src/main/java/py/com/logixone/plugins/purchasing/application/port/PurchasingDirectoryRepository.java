package py.com.logixone.plugins.purchasing.application.port;

import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.purchasing.application.query.PurchasingDirectoryQueries;

/** Company-scoped read model for purchasing directories. */
public interface PurchasingDirectoryRepository {
    PurchasingDirectoryQueries.Page<PurchasingDirectoryQueries.RequestSummary> requests(
            CompanyId companyId, PurchasingDirectoryQueries.RequestCriteria criteria);

    PurchasingDirectoryQueries.Page<PurchasingDirectoryQueries.OrderSummary> orders(
            CompanyId companyId, PurchasingDirectoryQueries.OrderCriteria criteria);

    PurchasingDirectoryQueries.Page<PurchasingDirectoryQueries.ReceiptSummary> receipts(
            CompanyId companyId, PurchasingDirectoryQueries.ReceiptCriteria criteria);

    PurchasingDirectoryQueries.Page<PurchasingDirectoryQueries.ReturnSummary> returns(
            CompanyId companyId, PurchasingDirectoryQueries.ReturnCriteria criteria);
}
