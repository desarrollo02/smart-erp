package py.com.logixone.plugins.inventory.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.domain.StockMovementSnapshot;

public interface StockMovementRepository {
    Optional<StockMovementSnapshot> findById(CompanyId companyId, StockMovementId movementId);
    Optional<StockMovementSnapshot> findByIdempotencyKey(
            CompanyId companyId, String sourceType, String idempotencyKey);
    StockMovementSnapshot append(StockMovementSnapshot movement);
}
