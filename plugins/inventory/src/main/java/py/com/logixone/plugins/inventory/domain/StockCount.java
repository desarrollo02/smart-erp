package py.com.logixone.plugins.inventory.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockKey;

/** Controlled count whose close produces differences instead of overwriting balances. */
public final class StockCount {
    private final CompanyId companyId;
    private final StockCountId id;
    private final StockCountScope scope;
    private final Map<StockKey, CountLine> lines = new LinkedHashMap<>();
    private StockCountState state = StockCountState.DRAFT;
    private long version;

    private StockCount(CompanyId companyId, StockCountId id, StockCountScope scope) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.id = Objects.requireNonNull(id, "id");
        this.scope = Objects.requireNonNull(scope, "scope");
    }

    public static StockCount draft(CompanyId companyId, StockCountId id, StockCountScope scope) {
        return new StockCount(companyId, id, scope);
    }

    public static StockCount restore(StockCountSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        StockCount count = new StockCount(snapshot.companyId(), snapshot.id(), snapshot.scope());
        snapshot.lines().forEach(line -> {
            if (!snapshot.scope().contains(line.key())
                    || count.lines.putIfAbsent(line.key(), new CountLine(
                            line.theoreticalQuantity(), line.countedQuantity().orElse(null))) != null) {
                throw new IllegalArgumentException("Invalid stock count line snapshot");
            }
        });
        count.state = snapshot.state();
        count.version = snapshot.version();
        return count;
    }

    public void addLine(StockKey key, BigDecimal theoreticalQuantity, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireState(StockCountState.DRAFT);
        Objects.requireNonNull(key, "key");
        if (!scope.contains(key)) {
            throw new IllegalArgumentException("Count line is outside the count scope");
        }
        if (lines.putIfAbsent(key, new CountLine(theoreticalQuantity, null)) != null) {
            throw new IllegalArgumentException("Stock key is already present in this count");
        }
        version++;
    }

    public void start(long expectedVersion) {
        verifyVersion(expectedVersion);
        requireState(StockCountState.DRAFT);
        if (lines.isEmpty()) {
            throw new IllegalStateException("A count must contain at least one line");
        }
        state = StockCountState.COUNTING;
        version++;
    }

    public void record(StockKey key, BigDecimal countedQuantity, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireState(StockCountState.COUNTING);
        CountLine line = lines.get(Objects.requireNonNull(key, "key"));
        if (line == null) {
            throw new IllegalArgumentException("Stock key does not belong to this count");
        }
        lines.put(key, new CountLine(line.theoreticalQuantity(), countedQuantity));
        version++;
    }

    public void sendToReview(long expectedVersion) {
        verifyVersion(expectedVersion);
        requireState(StockCountState.COUNTING);
        if (lines.values().stream().anyMatch(line -> line.countedQuantity() == null)) {
            throw new IllegalStateException("Every count line must be captured before review");
        }
        state = StockCountState.REVIEW;
        version++;
    }

    public List<StockCountAdjustment> post(long expectedVersion) {
        verifyVersion(expectedVersion);
        requireState(StockCountState.REVIEW);
        List<StockCountAdjustment> adjustments = adjustments();
        state = StockCountState.POSTED;
        version++;
        return adjustments;
    }

    public void cancel(long expectedVersion) {
        verifyVersion(expectedVersion);
        if (state == StockCountState.POSTED || state == StockCountState.CANCELLED) {
            throw new IllegalStateException("Posted or cancelled count cannot be cancelled again");
        }
        state = StockCountState.CANCELLED;
        version++;
    }

    public boolean blocks(StockKey key) {
        return (state == StockCountState.COUNTING || state == StockCountState.REVIEW)
                && scope.contains(key);
    }

    private List<StockCountAdjustment> adjustments() {
        return lines.entrySet().stream()
                .map(entry -> new StockCountAdjustment(
                        entry.getKey(), entry.getValue().theoreticalQuantity(),
                        entry.getValue().countedQuantity(),
                        entry.getValue().countedQuantity().subtract(entry.getValue().theoreticalQuantity())))
                .filter(adjustment -> adjustment.difference().signum() != 0)
                .toList();
    }

    private void verifyVersion(long expectedVersion) {
        if (expectedVersion != version) {
            throw new ConcurrentInventoryChangeException(expectedVersion, version);
        }
    }

    private void requireState(StockCountState expected) {
        if (state != expected) {
            throw new IllegalStateException("Count must be in " + expected + " state");
        }
    }

    private record CountLine(BigDecimal theoreticalQuantity, BigDecimal countedQuantity) {
        private CountLine {
            theoreticalQuantity = InventoryValues.quantity(theoreticalQuantity, "theoreticalQuantity", false);
            if (countedQuantity != null) {
                countedQuantity = InventoryValues.quantity(countedQuantity, "countedQuantity", false);
            }
        }
    }

    public CompanyId companyId() { return companyId; }
    public StockCountId id() { return id; }
    public StockCountScope scope() { return scope; }
    public StockCountState state() { return state; }
    public long version() { return version; }
    public StockCountSnapshot snapshot() {
        int[] lineNumber = {0};
        return new StockCountSnapshot(
                companyId, id, scope, state, version,
                lines.entrySet().stream()
                        .map(entry -> new StockCountLineSnapshot(
                                ++lineNumber[0], entry.getKey(), entry.getValue().theoreticalQuantity(),
                                Optional.ofNullable(entry.getValue().countedQuantity())))
                        .toList());
    }
}
