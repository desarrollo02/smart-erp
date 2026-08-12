package py.com.logixone.plugins.purchasing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderReference;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestLineId;

/** Supplier commitment with immutable ordered quantities and explicit fulfillment. */
public final class PurchaseOrder {
    private final CompanyId companyId;
    private final PurchaseOrderId id;
    private final String number;
    private final SupplierSnapshot supplier;
    private final CurrencySnapshot currency;
    private final Map<PurchaseOrderLineId, OrderLine> lines = new LinkedHashMap<>();
    private final Optional<String> directOrderJustification;
    private PurchaseOrderState state = PurchaseOrderState.DRAFT;
    private Optional<AppUserId> issuedBy = Optional.empty();
    private Optional<Instant> issuedAt = Optional.empty();
    private Optional<String> terminalReason = Optional.empty();
    private long version;

    private PurchaseOrder(
            CompanyId companyId,
            PurchaseOrderId id,
            String number,
            SupplierSnapshot supplier,
            CurrencySnapshot currency,
            List<LineDraft> lineDrafts,
            Optional<String> directOrderJustification) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.id = Objects.requireNonNull(id, "id");
        this.number = PurchasingValues.code(number, "number", 64);
        this.supplier = Objects.requireNonNull(supplier, "supplier");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.directOrderJustification = Objects.requireNonNull(
                directOrderJustification, "directOrderJustification")
                .map(value -> PurchasingValues.text(value, "directOrderJustification", 240));
        initializeLines(lineDrafts);
        boolean containsDirectQuantity = lines.values().stream()
                .anyMatch(line -> line.allocatedQuantity().compareTo(line.orderedQuantity) < 0);
        if (containsDirectQuantity && this.directOrderJustification.isEmpty()) {
            throw new IllegalArgumentException("Direct order quantities require justification");
        }
    }

    public static PurchaseOrder draft(
            CompanyId companyId,
            PurchaseOrderId id,
            String number,
            SupplierSnapshot supplier,
            CurrencySnapshot currency,
            List<LineDraft> lines,
            Optional<String> directOrderJustification) {
        return new PurchaseOrder(
                companyId, id, number, supplier, currency, lines, directOrderJustification);
    }

    public static PurchaseOrder restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<LineDraft> drafts = snapshot.lines().stream()
                .map(line -> new LineDraft(
                        line.id(), line.item(), line.orderedQuantity(), line.unitPrice(), line.allocations()))
                .toList();
        PurchaseOrder order = new PurchaseOrder(
                snapshot.companyId(), snapshot.id(), snapshot.number(), snapshot.supplier(),
                snapshot.currency(), drafts, snapshot.directOrderJustification());
        order.lines.clear();
        snapshot.lines().forEach(line -> order.lines.put(line.id(), OrderLine.restore(line)));
        order.state = Objects.requireNonNull(snapshot.state(), "state");
        order.issuedBy = Objects.requireNonNull(snapshot.issuedBy(), "issuedBy");
        order.issuedAt = Objects.requireNonNull(snapshot.issuedAt(), "issuedAt");
        order.terminalReason = Objects.requireNonNull(snapshot.terminalReason(), "terminalReason")
                .map(value -> PurchasingValues.text(value, "terminalReason", 240));
        if (snapshot.version() < 0 || order.issuedBy.isPresent() != order.issuedAt.isPresent()) {
            throw new IllegalArgumentException("Invalid order version or issue metadata");
        }
        if (order.state == PurchaseOrderState.DRAFT && order.issuedAt.isPresent()) {
            throw new IllegalArgumentException("Draft order cannot have issue metadata");
        }
        if ((order.state == PurchaseOrderState.ISSUED || order.state == PurchaseOrderState.CLOSED)
                && order.issuedAt.isEmpty()) {
            throw new IllegalArgumentException("Issued or closed order requires issue metadata");
        }
        if (order.state == PurchaseOrderState.CLOSED && !order.allPendingZero()) {
            throw new IllegalArgumentException("Closed order cannot retain pending quantity");
        }
        if (order.state == PurchaseOrderState.CANCELLED
                && (order.terminalReason.isEmpty()
                    || order.lines.values().stream().anyMatch(line -> line.receivedQuantity.signum() > 0))) {
            throw new IllegalArgumentException("Invalid cancelled order metadata");
        }
        if ((order.state == PurchaseOrderState.DRAFT || order.state == PurchaseOrderState.ISSUED)
                && order.terminalReason.isPresent()) {
            throw new IllegalArgumentException("Active order cannot have a terminal reason");
        }
        order.version = snapshot.version();
        return order;
    }

    private void initializeLines(List<LineDraft> lineDrafts) {
        lineDrafts = List.copyOf(Objects.requireNonNull(lineDrafts, "lineDrafts"));
        if (lineDrafts.isEmpty()) {
            throw new IllegalArgumentException("Purchase order must contain at least one line");
        }
        for (LineDraft draft : lineDrafts) {
            OrderLine previous = lines.putIfAbsent(
                    Objects.requireNonNull(draft, "lineDraft").id(), new OrderLine(draft));
            if (previous != null) {
                throw new IllegalArgumentException("Purchase order line ids must be unique");
            }
        }
    }

    public void issue(AppUserId actorId, Instant at, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireState(PurchaseOrderState.DRAFT);
        state = PurchaseOrderState.ISSUED;
        issuedBy = Optional.of(Objects.requireNonNull(actorId, "actorId"));
        issuedAt = Optional.of(Objects.requireNonNull(at, "at"));
        version++;
    }

    public void addLine(LineDraft draft, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireState(PurchaseOrderState.DRAFT);
        Objects.requireNonNull(draft, "draft");
        if (draft.allocations().stream().map(Allocation::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(draft.orderedQuantity()) < 0
                && directOrderJustification.isEmpty()) {
            throw new IllegalArgumentException(
                    "Direct order quantities require justification");
        }
        if (lines.putIfAbsent(draft.id(), new OrderLine(draft)) != null) {
            throw new IllegalArgumentException("Purchase order line id already exists");
        }
        version++;
    }

    public void applyReceipt(Map<PurchaseOrderLineId, BigDecimal> quantities, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireState(PurchaseOrderState.ISSUED);
        Map<PurchaseOrderLineId, BigDecimal> normalized =
                nonEmptyQuantities(quantities, "receipt quantities");
        normalized.forEach((lineId, quantity) -> line(lineId).requireReceivable(quantity));
        normalized.forEach((lineId, quantity) -> line(lineId).receive(quantity));
        if (allPendingZero()) {
            state = PurchaseOrderState.CLOSED;
        }
        version++;
    }

    public void applyReturn(Map<PurchaseOrderLineId, BigDecimal> quantities, long expectedVersion) {
        verifyVersion(expectedVersion);
        if (state != PurchaseOrderState.ISSUED && state != PurchaseOrderState.CLOSED) {
            throw new IllegalStateException("Only issued or fulfilled orders accept returns");
        }
        Map<PurchaseOrderLineId, BigDecimal> normalized =
                nonEmptyQuantities(quantities, "return quantities");
        normalized.forEach((lineId, quantity) -> line(lineId).requireReturnable(quantity));
        normalized.forEach((lineId, quantity) -> line(lineId).returnToSupplier(quantity));
        if (!allPendingZero()) {
            state = PurchaseOrderState.ISSUED;
        }
        version++;
    }

    public void closeShort(
            Map<PurchaseOrderLineId, BigDecimal> quantities,
            String reason,
            long expectedVersion) {
        verifyVersion(expectedVersion);
        requireState(PurchaseOrderState.ISSUED);
        Map<PurchaseOrderLineId, BigDecimal> normalized =
                nonEmptyQuantities(quantities, "short-close quantities");
        List<OrderLine> pendingLines = lines.values().stream()
                .filter(line -> line.pendingQuantity().signum() > 0)
                .toList();
        if (normalized.size() != pendingLines.size()
                || pendingLines.stream().anyMatch(line -> {
                    BigDecimal supplied = normalized.get(line.id);
                    return supplied == null
                            || PurchasingValues.quantity(supplied, "shortCloseQuantity")
                                    .compareTo(line.pendingQuantity()) != 0;
                })) {
            throw new IllegalArgumentException("Short close must cover every pending quantity exactly");
        }
        normalized.forEach((lineId, quantity) -> line(lineId).closeShort(quantity));
        terminalReason = Optional.of(PurchasingValues.text(reason, "reason", 240));
        state = PurchaseOrderState.CLOSED;
        version++;
    }

    public void cancel(String reason, long expectedVersion) {
        verifyVersion(expectedVersion);
        if (state != PurchaseOrderState.DRAFT && state != PurchaseOrderState.ISSUED) {
            throw new IllegalStateException("Only draft or unfulfilled issued orders can be cancelled");
        }
        if (lines.values().stream().anyMatch(line -> line.receivedQuantity.signum() > 0)) {
            throw new IllegalStateException("An order with confirmed receipts cannot be cancelled");
        }
        state = PurchaseOrderState.CANCELLED;
        terminalReason = Optional.of(PurchasingValues.text(reason, "reason", 240));
        version++;
    }

    private Map<PurchaseOrderLineId, BigDecimal> nonEmptyQuantities(
            Map<PurchaseOrderLineId, BigDecimal> quantities, String field) {
        quantities = Map.copyOf(Objects.requireNonNull(quantities, field));
        if (quantities.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return quantities;
    }

    private boolean allPendingZero() {
        return lines.values().stream().allMatch(line -> line.pendingQuantity().signum() == 0);
    }

    private OrderLine line(PurchaseOrderLineId id) {
        OrderLine line = lines.get(Objects.requireNonNull(id, "lineId"));
        if (line == null) {
            throw new IllegalArgumentException("Line does not belong to this purchase order");
        }
        return line;
    }

    private void verifyVersion(long expectedVersion) {
        if (expectedVersion != version) {
            throw new ConcurrentPurchasingChangeException(expectedVersion, version);
        }
    }

    private void requireState(PurchaseOrderState expected) {
        if (state != expected) {
            throw new IllegalStateException("Purchase order must be " + expected);
        }
    }

    public BigDecimal orderedTotal() {
        return lines.values().stream()
                .map(line -> PurchasingValues.total(
                        line.orderedQuantity, line.unitPrice, currency.minorUnit()))
                .reduce(BigDecimal.ZERO.setScale(currency.minorUnit()), BigDecimal::add);
    }

    public PurchaseOrderReference reference() {
        return new PurchaseOrderReference(
                id, number, supplier.id().toString(), state, currency.code().value(),
                orderedTotal(), version);
    }

    public Snapshot snapshot() {
        return new Snapshot(
                companyId, id, number, supplier, currency,
                lines.values().stream().map(OrderLine::snapshot).toList(),
                directOrderJustification, state, issuedBy, issuedAt, terminalReason, version);
    }

    public CompanyId companyId() { return companyId; }
    public PurchaseOrderId id() { return id; }
    public PurchaseOrderState state() { return state; }
    public long version() { return version; }
    public List<LineSnapshot> lines() { return lines.values().stream().map(OrderLine::snapshot).toList(); }

    public record Allocation(
            PurchaseRequestId requestId,
            PurchaseRequestLineId requestLineId,
            BigDecimal quantity) {
        public Allocation {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(requestLineId, "requestLineId");
            quantity = PurchasingValues.quantity(quantity, "allocationQuantity");
        }
    }

    public record LineDraft(
            PurchaseOrderLineId id,
            PurchasedItemSnapshot item,
            BigDecimal orderedQuantity,
            BigDecimal unitPrice,
            List<Allocation> allocations) {
        public LineDraft {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(item, "item");
            orderedQuantity = PurchasingValues.quantity(orderedQuantity, "orderedQuantity");
            unitPrice = PurchasingValues.amount(unitPrice, "unitPrice");
            allocations = List.copyOf(Objects.requireNonNull(allocations, "allocations"));
            HashSet<PurchaseRequestLineId> ids = new HashSet<>();
            if (allocations.stream().anyMatch(allocation ->
                    !ids.add(Objects.requireNonNull(allocation, "allocation").requestLineId()))) {
                throw new IllegalArgumentException("A request line can be allocated only once per order line");
            }
            BigDecimal allocated = allocations.stream()
                    .map(Allocation::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (allocated.compareTo(orderedQuantity) > 0) {
                throw new IllegalArgumentException("Allocated quantity exceeds ordered quantity");
            }
        }
    }

    public record LineSnapshot(
            PurchaseOrderLineId id,
            PurchasedItemSnapshot item,
            BigDecimal orderedQuantity,
            BigDecimal unitPrice,
            List<Allocation> allocations,
            BigDecimal receivedQuantity,
            BigDecimal returnedQuantity,
            BigDecimal shortClosedQuantity,
            BigDecimal pendingQuantity) {
        public LineSnapshot {
            allocations = List.copyOf(Objects.requireNonNull(allocations, "allocations"));
        }
    }

    public record Snapshot(
            CompanyId companyId,
            PurchaseOrderId id,
            String number,
            SupplierSnapshot supplier,
            CurrencySnapshot currency,
            List<LineSnapshot> lines,
            Optional<String> directOrderJustification,
            PurchaseOrderState state,
            Optional<AppUserId> issuedBy,
            Optional<Instant> issuedAt,
            Optional<String> terminalReason,
            long version) {
        public Snapshot {
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        }
    }

    private static final class OrderLine {
        private final PurchaseOrderLineId id;
        private final PurchasedItemSnapshot item;
        private final BigDecimal orderedQuantity;
        private final BigDecimal unitPrice;
        private final List<Allocation> allocations;
        private BigDecimal receivedQuantity = BigDecimal.ZERO;
        private BigDecimal returnedQuantity = BigDecimal.ZERO;
        private BigDecimal shortClosedQuantity = BigDecimal.ZERO;

        private OrderLine(LineDraft draft) {
            id = draft.id();
            item = draft.item();
            orderedQuantity = draft.orderedQuantity();
            unitPrice = draft.unitPrice();
            allocations = draft.allocations();
        }

        private static OrderLine restore(LineSnapshot snapshot) {
            LineDraft draft = new LineDraft(
                    snapshot.id(), snapshot.item(), snapshot.orderedQuantity(),
                    snapshot.unitPrice(), snapshot.allocations());
            OrderLine line = new OrderLine(draft);
            line.receivedQuantity = nonNegative(snapshot.receivedQuantity(), "receivedQuantity");
            line.returnedQuantity = nonNegative(snapshot.returnedQuantity(), "returnedQuantity");
            line.shortClosedQuantity = nonNegative(snapshot.shortClosedQuantity(), "shortClosedQuantity");
            if (line.returnedQuantity.compareTo(line.receivedQuantity) > 0
                    || line.pendingQuantity().signum() < 0
                    || line.pendingQuantity().compareTo(snapshot.pendingQuantity()) != 0) {
                throw new IllegalArgumentException("Invalid purchase order line fulfillment snapshot");
            }
            return line;
        }

        private static BigDecimal nonNegative(BigDecimal value, String field) {
            value = Objects.requireNonNull(value, field).stripTrailingZeros();
            if (value.signum() < 0 || Math.max(value.scale(), 0) > 6) {
                throw new IllegalArgumentException("Invalid " + field);
            }
            return value;
        }

        private BigDecimal allocatedQuantity() {
            return allocations.stream().map(Allocation::quantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal pendingQuantity() {
            return orderedQuantity.subtract(receivedQuantity)
                    .add(returnedQuantity).subtract(shortClosedQuantity);
        }

        private void requireReceivable(BigDecimal quantity) {
            quantity = PurchasingValues.quantity(quantity, "receiptQuantity");
            if (quantity.compareTo(pendingQuantity()) > 0) {
                throw new IllegalArgumentException("Receipt quantity exceeds order remainder");
            }
        }

        private void receive(BigDecimal quantity) {
            receivedQuantity = receivedQuantity.add(
                    PurchasingValues.quantity(quantity, "receiptQuantity"));
        }

        private void requireReturnable(BigDecimal quantity) {
            quantity = PurchasingValues.quantity(quantity, "returnQuantity");
            if (quantity.compareTo(receivedQuantity.subtract(returnedQuantity)) > 0) {
                throw new IllegalArgumentException("Return quantity exceeds net received quantity");
            }
        }

        private void returnToSupplier(BigDecimal quantity) {
            returnedQuantity = returnedQuantity.add(
                    PurchasingValues.quantity(quantity, "returnQuantity"));
        }

        private void closeShort(BigDecimal quantity) {
            quantity = PurchasingValues.quantity(quantity, "shortCloseQuantity");
            if (quantity.compareTo(pendingQuantity()) != 0) {
                throw new IllegalArgumentException("Short close must equal the pending quantity");
            }
            shortClosedQuantity = shortClosedQuantity.add(quantity);
        }

        private LineSnapshot snapshot() {
            return new LineSnapshot(
                    id, item, orderedQuantity, unitPrice, allocations, receivedQuantity,
                    returnedQuantity, shortClosedQuantity, pendingQuantity());
        }
    }
}
