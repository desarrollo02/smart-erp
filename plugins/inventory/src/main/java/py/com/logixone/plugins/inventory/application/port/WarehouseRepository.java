package py.com.logixone.plugins.inventory.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.domain.Warehouse;

public interface WarehouseRepository {
    Optional<Warehouse> findById(CompanyId companyId, WarehouseId warehouseId);
    Warehouse insert(Warehouse warehouse);
    Warehouse update(Warehouse warehouse, long expectedPersistedVersion);
}
