package py.com.logixone.plugins.inventory.application.query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.domain.InventoryItemSnapshot;
import py.com.logixone.plugins.inventory.domain.StockCountScope;
import py.com.logixone.plugins.inventory.domain.StockCountState;

/** Private read projections used by the inventory user journeys. */
public final class InventoryDirectoryQueries {
    private InventoryDirectoryQueries() {
    }

    public record Criteria(Optional<String> text, Optional<Boolean> active, int offset, int limit) {
        public Criteria {
            Objects.requireNonNull(text, "text");
            text = text.map(value -> value.trim().toLowerCase(Locale.ROOT))
                    .filter(value -> !value.isEmpty());
            if (text.map(String::length).orElse(0) > 100) {
                throw new IllegalArgumentException("text must not exceed 100 characters");
            }
            Objects.requireNonNull(active, "active");
            page(offset, limit);
        }
    }

    public record CountCriteria(Optional<StockCountState> state, int offset, int limit) {
        public CountCriteria {
            Objects.requireNonNull(state, "state");
            page(offset, limit);
        }
    }

    public record Page<T>(List<T> items, long total, int offset, int limit) {
        public Page {
            items = List.copyOf(Objects.requireNonNull(items, "items"));
            page(offset, limit);
            if (total < items.size() || total < 0) {
                throw new IllegalArgumentException("total must cover returned items");
            }
        }
    }

    public record ItemSummary(
            InventoryItemSnapshot item,
            BigDecimal physicalQuantity,
            BigDecimal reservedQuantity,
            long balanceBuckets) {
        public ItemSummary {
            Objects.requireNonNull(item, "item");
            physicalQuantity = nonNegative(physicalQuantity, "physicalQuantity");
            reservedQuantity = nonNegative(reservedQuantity, "reservedQuantity");
            if (reservedQuantity.compareTo(physicalQuantity) > 0 || balanceBuckets < 0) {
                throw new IllegalArgumentException("Invalid inventory summary");
            }
        }

        public InventoryItemId id() {
            return item.id();
        }

        public BigDecimal availableQuantity() {
            return physicalQuantity.subtract(reservedQuantity);
        }
    }

    public record CountSummary(
            StockCountId id,
            StockCountScope scope,
            StockCountState state,
            long version,
            long lineCount) {
        public CountSummary {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(state, "state");
            if (version < 0 || lineCount < 0) {
                throw new IllegalArgumentException("Invalid count summary");
            }
        }
    }

    private static void page(int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Invalid page range");
        }
    }

    private static BigDecimal nonNegative(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }
}
