package py.com.logixone.plugins.inventory.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.domain.InventoryBalance;

public interface InventoryBalanceRepository {
    Optional<InventoryBalance> find(CompanyId companyId, StockKey key);
    InventoryBalance insert(InventoryBalance balance);
    InventoryBalance update(InventoryBalance balance, long expectedPersistedVersion);
    boolean hasQuantity(
            CompanyId companyId, WarehouseId warehouseId, Optional<StockLocationId> locationId);
    boolean hasQuantity(CompanyId companyId, InventoryItemId inventoryItemId);
}
