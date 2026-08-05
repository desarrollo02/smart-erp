package py.com.logixone.plugins.inventory.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.domain.StockCount;

public interface StockCountRepository {
    Optional<StockCount> findById(CompanyId companyId, StockCountId countId);
    boolean blocks(CompanyId companyId, StockKey key);
    StockCount insert(StockCount count);
    StockCount update(StockCount count, long expectedPersistedVersion);
}
