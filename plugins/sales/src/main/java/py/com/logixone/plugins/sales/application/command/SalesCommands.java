package py.com.logixone.plugins.sales.application.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.sales.api.SalesOrderId;
import py.com.logixone.plugins.sales.api.SalesQuoteId;

public final class SalesCommands {
    private SalesCommands() { }

    public record CreateTerm(String code, String displayName, int dueDays,
            String idempotencyKey) { }
    public record ReviseTerm(UUID termId, String displayName, int dueDays,
            long expectedVersion, String idempotencyKey) { }
    public record DeactivateTerm(UUID termId, long expectedVersion,
            String idempotencyKey) { }

    public record LineInput(UUID id, CatalogItemId itemId, String unitCode,
            BigDecimal quantity, PriceListId priceListId,
            Optional<BigDecimal> overrideUnitPrice, Optional<String> overrideReason) {
        public LineInput {
            Objects.requireNonNull(id); Objects.requireNonNull(itemId);
            Objects.requireNonNull(unitCode); Objects.requireNonNull(quantity);
            Objects.requireNonNull(priceListId); Objects.requireNonNull(overrideUnitPrice);
            Objects.requireNonNull(overrideReason);
            if (overrideUnitPrice.isPresent() != overrideReason.isPresent()) {
                throw new IllegalArgumentException("Price override requires amount and reason");
            }
        }
    }
    public record CreateQuote(String number, BusinessPartnerId customerId,
            String customerTaxId, CurrencyCode currencyCode, UUID termId,
            LocalDate validUntil, List<LineInput> lines, String idempotencyKey) {
        public CreateQuote { lines = List.copyOf(lines); }
        public boolean overridesPrice() { return lines.stream().anyMatch(v -> v.overrideUnitPrice().isPresent()); }
    }
    public record QuoteTransition(SalesQuoteId quoteId, long expectedVersion,
            String idempotencyKey, Optional<String> reason) { }
    public record AcceptQuote(SalesQuoteId quoteId, long expectedVersion,
            String orderNumber, String idempotencyKey) { }
    public record CreateOrder(String number, BusinessPartnerId customerId,
            String customerTaxId, CurrencyCode currencyCode, UUID termId,
            List<LineInput> lines, String idempotencyKey) {
        public CreateOrder { lines = List.copyOf(lines); }
        public boolean overridesPrice() { return lines.stream().anyMatch(v -> v.overrideUnitPrice().isPresent()); }
    }
    public record ReservationInput(UUID lineId, WarehouseId warehouseId,
            StockLocationId locationId, Optional<String> lotCode,
            Optional<String> serialNumber, Optional<LocalDate> expiryDate,
            StockCondition condition, Instant expiresAt) { }
    public record ConfirmOrder(SalesOrderId orderId, long expectedVersion,
            List<ReservationInput> reservations, String idempotencyKey) {
        public ConfirmOrder { reservations = List.copyOf(reservations); }
    }
    public record CancelOrder(SalesOrderId orderId, long expectedVersion,
            String reason, String idempotencyKey) { }
    public record CloseOrder(SalesOrderId orderId, long expectedVersion,
            String idempotencyKey) { }
}
