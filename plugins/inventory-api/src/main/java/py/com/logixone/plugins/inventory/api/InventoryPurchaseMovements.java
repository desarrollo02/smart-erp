package py.com.logixone.plugins.inventory.api;

import py.com.logixone.kernel.api.company.CompanyId;

/** Purchase-specific movement boundary addressed by the public catalog identity. */
public interface InventoryPurchaseMovements {
    StockMovementReference postCatalogItem(
            CompanyId companyId, CatalogStockMovementRequest request);
}
