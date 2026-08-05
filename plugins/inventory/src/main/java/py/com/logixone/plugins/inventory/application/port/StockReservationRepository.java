package py.com.logixone.plugins.inventory.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.domain.StockReservation;

public interface StockReservationRepository {
    Optional<StockReservation> findById(CompanyId companyId, StockReservationId reservationId);
    Optional<StockReservation> findByIdempotencyKey(
            CompanyId companyId, String sourceType, String idempotencyKey);
    StockReservation insert(StockReservation reservation);
    StockReservation update(StockReservation reservation, long expectedPersistedVersion);
}
