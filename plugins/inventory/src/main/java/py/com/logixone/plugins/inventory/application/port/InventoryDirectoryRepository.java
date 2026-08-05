package py.com.logixone.plugins.inventory.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.application.query.InventoryDirectoryQueries;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;

/** Company-scoped read model for UI directories; it owns no mutable aggregate. */
public interface InventoryDirectoryRepository {
    InventoryDirectoryQueries.Page<WarehouseSnapshot> warehouses(
            CompanyId companyId, InventoryDirectoryQueries.Criteria criteria);

    InventoryDirectoryQueries.Page<InventoryDirectoryQueries.ItemSummary> items(
            CompanyId companyId, InventoryDirectoryQueries.Criteria criteria);

    Optional<InventoryDirectoryQueries.ItemSummary> item(
            CompanyId companyId, InventoryItemId inventoryItemId);

    InventoryDirectoryQueries.Page<InventoryDirectoryQueries.CountSummary> counts(
            CompanyId companyId, InventoryDirectoryQueries.CountCriteria criteria);
}
