package py.com.logixone.plugins.inventory.api;

import java.math.BigDecimal;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

/** Public reservation commands; application authorization remains server-side. */
public interface InventoryReservations {
    Optional<StockReservationReference> find(
            CompanyId companyId, StockReservationId reservationId);

    StockReservationReference reserve(CompanyId companyId, StockReservationRequest request);

    StockReservationReference reserveCatalogItem(
            CompanyId companyId, CatalogStockReservationRequest request);

    StockReservationReference consume(
            CompanyId companyId,
            StockReservationId reservationId,
            long expectedVersion,
            BigDecimal quantity,
            String idempotencyKey);

    StockReservationReference release(
            CompanyId companyId,
            StockReservationId reservationId,
            long expectedVersion,
            BigDecimal quantity,
            String idempotencyKey);

    StockReservationReference expire(
            CompanyId companyId,
            StockReservationId reservationId,
            long expectedVersion,
            String idempotencyKey);
}
