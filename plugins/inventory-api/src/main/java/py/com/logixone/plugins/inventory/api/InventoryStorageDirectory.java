package py.com.logixone.plugins.inventory.api;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

/** Authorized public read boundary for warehouse and location selectors. */
public interface InventoryStorageDirectory {
    StorageSearchPage searchWarehouses(CompanyId companyId, StorageSearchQuery query);

    Optional<WarehouseReference> findWarehouse(CompanyId companyId, WarehouseId warehouseId);
}
