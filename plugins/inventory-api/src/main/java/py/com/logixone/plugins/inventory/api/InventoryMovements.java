package py.com.logixone.plugins.inventory.api;

import py.com.logixone.kernel.api.company.CompanyId;

/** Public command contract for inventory-owning movement posting. */
public interface InventoryMovements {
    StockMovementReference post(CompanyId companyId, StockMovementRequest request);
}
