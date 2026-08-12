package py.com.logixone.plugins.purchasing.api;

import py.com.logixone.kernel.api.company.CompanyId;

/** Controlled idempotent import boundary for open purchasing documents. */
public interface PurchasingImports {
    PurchaseRequestReference importOpenRequest(CompanyId companyId, OpenPurchaseRequestImport request);
    PurchaseOrderReference importOpenOrder(CompanyId companyId, OpenPurchaseOrderImport request);
}
