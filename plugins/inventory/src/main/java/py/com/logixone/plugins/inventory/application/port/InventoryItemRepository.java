package py.com.logixone.plugins.inventory.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.domain.InventoryItem;

public interface InventoryItemRepository {
    Optional<InventoryItem> findById(CompanyId companyId, InventoryItemId inventoryItemId);
    Optional<InventoryItem> findByCatalogItemId(CompanyId companyId, CatalogItemId catalogItemId);
    InventoryItem insert(InventoryItem item);
    InventoryItem update(InventoryItem item, long expectedPersistedVersion);
}
