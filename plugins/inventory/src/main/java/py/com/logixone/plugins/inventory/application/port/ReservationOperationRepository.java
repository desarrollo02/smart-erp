package py.com.logixone.plugins.inventory.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.domain.ReservationOperation;

public interface ReservationOperationRepository {
    Optional<ReservationOperation> findByIdempotencyKey(
            CompanyId companyId, String idempotencyKey);
    ReservationOperation append(ReservationOperation operation);
}
