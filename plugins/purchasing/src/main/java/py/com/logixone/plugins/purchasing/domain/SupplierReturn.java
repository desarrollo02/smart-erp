package py.com.logixone.plugins.purchasing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderLineId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnLineId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnState;

/** Explicit compensation for received quantities; it never edits the receipt. */
public final class SupplierReturn {
    private final CompanyId companyId;
    private final SupplierReturnId id;
    private final String number;
    private final PurchaseOrderId orderId;
    private final String reason;
    private final List<Line> lines;
    private SupplierReturnState state = SupplierReturnState.DRAFT;
    private Optional<AppUserId> confirmedBy = Optional.empty();
    private Optional<Instant> confirmedAt = Optional.empty();
    private Map<SupplierReturnLineId, StockMovementId> stockMovements = Map.of();
    private long version;

    private SupplierReturn(
            CompanyId companyId,
            SupplierReturnId id,
            String number,
            PurchaseOrderId orderId,
            String reason,
            List<Line> lines) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.id = Objects.requireNonNull(id, "id");
        this.number = PurchasingValues.code(number, "number", 64);
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.reason = PurchasingValues.text(reason, "reason", 240);
        this.lines = validateLines(lines);
    }

    public static SupplierReturn draft(
            CompanyId companyId,
            SupplierReturnId id,
            String number,
            PurchaseOrderId orderId,
            String reason,
            List<Line> lines) {
        return new SupplierReturn(companyId, id, number, orderId, reason, lines);
    }

    public static SupplierReturn restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        SupplierReturn supplierReturn = new SupplierReturn(
                snapshot.companyId(), snapshot.id(), snapshot.number(), snapshot.orderId(),
                snapshot.reason(), snapshot.lines());
        supplierReturn.state = Objects.requireNonNull(snapshot.state(), "state");
        supplierReturn.confirmedBy = Objects.requireNonNull(snapshot.confirmedBy(), "confirmedBy");
        supplierReturn.confirmedAt = Objects.requireNonNull(snapshot.confirmedAt(), "confirmedAt");
        supplierReturn.stockMovements = Map.copyOf(
                Objects.requireNonNull(snapshot.stockMovements(), "stockMovements"));
        if (snapshot.version() < 0
                || supplierReturn.confirmedBy.isPresent() != supplierReturn.confirmedAt.isPresent()) {
            throw new IllegalArgumentException("Invalid supplier-return confirmation metadata");
        }
        if (supplierReturn.state == SupplierReturnState.CONFIRMED) {
            if (supplierReturn.confirmedAt.isEmpty()) {
                throw new IllegalArgumentException("Confirmed supplier return requires actor and time");
            }
            supplierReturn.validateStockMovementCoverage(supplierReturn.stockMovements);
        } else if (supplierReturn.confirmedAt.isPresent() || !supplierReturn.stockMovements.isEmpty()) {
            throw new IllegalArgumentException("Draft supplier return cannot contain confirmation metadata");
        }
        supplierReturn.version = snapshot.version();
        return supplierReturn;
    }

    public void confirm(
            AppUserId actorId,
            Instant at,
            Map<SupplierReturnLineId, StockMovementId> stockMovements,
            long expectedVersion) {
        verifyVersion(expectedVersion);
        if (state != SupplierReturnState.DRAFT) {
            throw new IllegalStateException("Confirmed supplier return is immutable");
        }
        stockMovements = Map.copyOf(Objects.requireNonNull(stockMovements, "stockMovements"));
        validateStockMovementCoverage(stockMovements);
        this.stockMovements = stockMovements;
        confirmedBy = Optional.of(Objects.requireNonNull(actorId, "actorId"));
        confirmedAt = Optional.of(Objects.requireNonNull(at, "at"));
        state = SupplierReturnState.CONFIRMED;
        version++;
    }

    private void validateStockMovementCoverage(Map<SupplierReturnLineId, StockMovementId> movements) {
        Set<SupplierReturnLineId> stockLineIds = lines.stream()
                .filter(line -> line.kind() == PurchaseLineKind.STOCK)
                .map(Line::id)
                .collect(Collectors.toUnmodifiableSet());
        if (!movements.keySet().equals(stockLineIds)) {
            throw new IllegalArgumentException("Every STOCK return line requires one movement reference");
        }
    }

    private static List<Line> validateLines(List<Line> lines) {
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Supplier return must contain at least one line");
        }
        HashSet<SupplierReturnLineId> ids = new HashSet<>();
        HashSet<GoodsReceiptLineId> receiptLines = new HashSet<>();
        for (Line line : lines) {
            line = Objects.requireNonNull(line, "line");
            if (!ids.add(line.id()) || !receiptLines.add(line.receiptLineId())) {
                throw new IllegalArgumentException("Return line and receipt line ids must be unique");
            }
        }
        return lines;
    }

    private void verifyVersion(long expectedVersion) {
        if (expectedVersion != version) {
            throw new ConcurrentPurchasingChangeException(expectedVersion, version);
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(
                companyId, id, number, orderId, reason, lines, state, confirmedBy,
                confirmedAt, stockMovements, version);
    }

    public CompanyId companyId() { return companyId; }
    public SupplierReturnId id() { return id; }
    public PurchaseOrderId orderId() { return orderId; }
    public SupplierReturnState state() { return state; }
    public List<Line> lines() { return lines; }
    public long version() { return version; }

    public record Line(
            SupplierReturnLineId id,
            GoodsReceiptId receiptId,
            GoodsReceiptLineId receiptLineId,
            PurchaseOrderLineId orderLineId,
            PurchaseLineKind kind,
            BigDecimal quantity,
            Optional<WarehouseId> warehouseId,
            Optional<StockLocationId> locationId,
            Optional<String> lotCode,
            Optional<String> serialNumber,
            Optional<LocalDate> expiryDate,
            Optional<StockCondition> condition) {
        public Line {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(receiptId, "receiptId");
            Objects.requireNonNull(receiptLineId, "receiptLineId");
            Objects.requireNonNull(orderLineId, "orderLineId");
            Objects.requireNonNull(kind, "kind");
            quantity = PurchasingValues.quantity(quantity, "quantity");
            warehouseId = Objects.requireNonNull(warehouseId, "warehouseId");
            locationId = Objects.requireNonNull(locationId, "locationId");
            lotCode = normalize(lotCode, "lotCode", 80);
            serialNumber = normalize(serialNumber, "serialNumber", 120);
            expiryDate = Objects.requireNonNull(expiryDate, "expiryDate");
            condition = Objects.requireNonNull(condition, "condition");
            boolean hasSourceLocation = warehouseId.isPresent() && locationId.isPresent();
            if (warehouseId.isPresent() != locationId.isPresent()
                    || (kind == PurchaseLineKind.STOCK) != hasSourceLocation
                    || (kind == PurchaseLineKind.STOCK) != condition.isPresent()
                    || (kind != PurchaseLineKind.STOCK
                        && (lotCode.isPresent() || serialNumber.isPresent()
                            || expiryDate.isPresent()))) {
                throw new IllegalArgumentException("Only STOCK lines require a complete inventory source");
            }
        }

        public Line(
                SupplierReturnLineId id,
                GoodsReceiptId receiptId,
                GoodsReceiptLineId receiptLineId,
                PurchaseOrderLineId orderLineId,
                PurchaseLineKind kind,
                BigDecimal quantity,
                Optional<WarehouseId> warehouseId,
                Optional<StockLocationId> locationId) {
            this(id, receiptId, receiptLineId, orderLineId, kind, quantity,
                    warehouseId, locationId, Optional.empty(), Optional.empty(),
                    Optional.empty(), kind == PurchaseLineKind.STOCK
                            ? Optional.of(StockCondition.AVAILABLE) : Optional.empty());
        }

        private static Optional<String> normalize(
                Optional<String> value, String field, int maximumLength) {
            Objects.requireNonNull(value, field);
            return value.map(text -> PurchasingValues.text(text, field, maximumLength));
        }
    }

    public record Snapshot(
            CompanyId companyId,
            SupplierReturnId id,
            String number,
            PurchaseOrderId orderId,
            String reason,
            List<Line> lines,
            SupplierReturnState state,
            Optional<AppUserId> confirmedBy,
            Optional<Instant> confirmedAt,
            Map<SupplierReturnLineId, StockMovementId> stockMovements,
            long version) {
        public Snapshot {
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
            stockMovements = Map.copyOf(Objects.requireNonNull(stockMovements, "stockMovements"));
        }
    }
}
