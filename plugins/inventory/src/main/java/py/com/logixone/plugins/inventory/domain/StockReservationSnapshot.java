package py.com.logixone.plugins.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.StockReservationRequest;
import py.com.logixone.plugins.inventory.api.StockReservationState;

public record StockReservationSnapshot(
        CompanyId companyId,
        StockReservationId id,
        StockReservationRequest request,
        Instant createdAt,
        BigDecimal consumedQuantity,
        BigDecimal releasedQuantity,
        StockReservationState state,
        long version) {
    public StockReservationSnapshot {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(createdAt, "createdAt");
        consumedQuantity = InventoryValues.quantity(consumedQuantity, "consumedQuantity", false);
        releasedQuantity = InventoryValues.quantity(releasedQuantity, "releasedQuantity", false);
        Objects.requireNonNull(state, "state");
        if (consumedQuantity.add(releasedQuantity).compareTo(request.quantity()) > 0) {
            throw new IllegalArgumentException("Reservation consumption and release exceed original quantity");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
