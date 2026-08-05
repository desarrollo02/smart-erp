package py.com.logixone.plugins.inventory.api;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

/** Purpose-specific query contract for one exact stock key. */
public interface InventoryAvailability {
    Optional<StockAvailability> find(CompanyId companyId, StockKey key);
}
