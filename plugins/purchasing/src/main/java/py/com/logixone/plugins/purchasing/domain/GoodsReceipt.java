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
import py.com.logixone.plugins.purchasing.api.GoodsReceiptState;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderLineId;

/** Append-only receipt whose stock lines require a posted inventory movement. */
public final class GoodsReceipt {
    private final CompanyId companyId;
    private final GoodsReceiptId id;
    private final String number;
    private final PurchaseOrderId orderId;
    private final List<Line> lines;
    private GoodsReceiptState state = GoodsReceiptState.DRAFT;
    private Optional<AppUserId> confirmedBy = Optional.empty();
    private Optional<Instant> confirmedAt = Optional.empty();
    private Map<GoodsReceiptLineId, StockMovementId> stockMovements = Map.of();
    private long version;

    private GoodsReceipt(
            CompanyId companyId,
            GoodsReceiptId id,
            String number,
            PurchaseOrderId orderId,
            List<Line> lines) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.id = Objects.requireNonNull(id, "id");
        this.number = PurchasingValues.code(number, "number", 64);
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.lines = validateLines(lines);
    }

    public static GoodsReceipt draft(
            CompanyId companyId,
            GoodsReceiptId id,
            String number,
            PurchaseOrderId orderId,
            List<Line> lines) {
        return new GoodsReceipt(companyId, id, number, orderId, lines);
    }

    public static GoodsReceipt restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        GoodsReceipt receipt = new GoodsReceipt(
                snapshot.companyId(), snapshot.id(), snapshot.number(), snapshot.orderId(), snapshot.lines());
        receipt.state = Objects.requireNonNull(snapshot.state(), "state");
        receipt.confirmedBy = Objects.requireNonNull(snapshot.confirmedBy(), "confirmedBy");
        receipt.confirmedAt = Objects.requireNonNull(snapshot.confirmedAt(), "confirmedAt");
        receipt.stockMovements = Map.copyOf(Objects.requireNonNull(snapshot.stockMovements(), "stockMovements"));
        if (snapshot.version() < 0 || receipt.confirmedBy.isPresent() != receipt.confirmedAt.isPresent()) {
            throw new IllegalArgumentException("Invalid receipt confirmation metadata");
        }
        if (receipt.state == GoodsReceiptState.CONFIRMED) {
            if (receipt.confirmedAt.isEmpty()) {
                throw new IllegalArgumentException("Confirmed receipt requires actor and time");
            }
            receipt.validateStockMovementCoverage(receipt.stockMovements);
        } else if (receipt.confirmedAt.isPresent() || !receipt.stockMovements.isEmpty()) {
            throw new IllegalArgumentException("Draft receipt cannot contain confirmation metadata");
        }
        receipt.version = snapshot.version();
        return receipt;
    }

    public void confirm(
            AppUserId actorId,
            Instant at,
            Map<GoodsReceiptLineId, StockMovementId> stockMovements,
            long expectedVersion) {
        verifyVersion(expectedVersion);
        if (state != GoodsReceiptState.DRAFT) {
            throw new IllegalStateException("Confirmed receipt is immutable");
        }
        stockMovements = Map.copyOf(Objects.requireNonNull(stockMovements, "stockMovements"));
        validateStockMovementCoverage(stockMovements);
        this.stockMovements = stockMovements;
        confirmedBy = Optional.of(Objects.requireNonNull(actorId, "actorId"));
        confirmedAt = Optional.of(Objects.requireNonNull(at, "at"));
        state = GoodsReceiptState.CONFIRMED;
        version++;
    }

    private void validateStockMovementCoverage(Map<GoodsReceiptLineId, StockMovementId> movements) {
        Set<GoodsReceiptLineId> stockLineIds = lines.stream()
                .filter(line -> line.kind() == PurchaseLineKind.STOCK)
                .map(Line::id)
                .collect(Collectors.toUnmodifiableSet());
        if (!movements.keySet().equals(stockLineIds)) {
            throw new IllegalArgumentException("Every STOCK receipt line requires one movement reference");
        }
    }

    private static List<Line> validateLines(List<Line> lines) {
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Receipt must contain at least one line");
        }
        HashSet<GoodsReceiptLineId> ids = new HashSet<>();
        HashSet<PurchaseOrderLineId> orderLines = new HashSet<>();
        for (Line line : lines) {
            line = Objects.requireNonNull(line, "line");
            if (!ids.add(line.id()) || !orderLines.add(line.orderLineId())) {
                throw new IllegalArgumentException("Receipt line and order line ids must be unique");
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
                companyId, id, number, orderId, lines, state, confirmedBy, confirmedAt,
                stockMovements, version);
    }

    public CompanyId companyId() { return companyId; }
    public GoodsReceiptId id() { return id; }
    public PurchaseOrderId orderId() { return orderId; }
    public GoodsReceiptState state() { return state; }
    public List<Line> lines() { return lines; }
    public long version() { return version; }

    public record Line(
            GoodsReceiptLineId id,
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
            Objects.requireNonNull(orderLineId, "orderLineId");
            Objects.requireNonNull(kind, "kind");
            quantity = PurchasingValues.quantity(quantity, "quantity");
            warehouseId = Objects.requireNonNull(warehouseId, "warehouseId");
            locationId = Objects.requireNonNull(locationId, "locationId");
            lotCode = normalize(lotCode, "lotCode", 80);
            serialNumber = normalize(serialNumber, "serialNumber", 120);
            expiryDate = Objects.requireNonNull(expiryDate, "expiryDate");
            condition = Objects.requireNonNull(condition, "condition");
            boolean hasDestination = warehouseId.isPresent() && locationId.isPresent();
            if (warehouseId.isPresent() != locationId.isPresent()
                    || (kind == PurchaseLineKind.STOCK) != hasDestination
                    || (kind == PurchaseLineKind.STOCK) != condition.isPresent()
                    || (kind != PurchaseLineKind.STOCK
                        && (lotCode.isPresent() || serialNumber.isPresent()
                            || expiryDate.isPresent()))) {
                throw new IllegalArgumentException("Only STOCK lines require a complete inventory destination");
            }
        }

        public Line(
                GoodsReceiptLineId id,
                PurchaseOrderLineId orderLineId,
                PurchaseLineKind kind,
                BigDecimal quantity,
                Optional<WarehouseId> warehouseId,
                Optional<StockLocationId> locationId) {
            this(id, orderLineId, kind, quantity, warehouseId, locationId,
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    kind == PurchaseLineKind.STOCK
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
            GoodsReceiptId id,
            String number,
            PurchaseOrderId orderId,
            List<Line> lines,
            GoodsReceiptState state,
            Optional<AppUserId> confirmedBy,
            Optional<Instant> confirmedAt,
            Map<GoodsReceiptLineId, StockMovementId> stockMovements,
            long version) {
        public Snapshot {
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
            stockMovements = Map.copyOf(Objects.requireNonNull(stockMovements, "stockMovements"));
        }
    }
}
