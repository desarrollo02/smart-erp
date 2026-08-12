package py.com.logixone.plugins.purchasing.application.query;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptState;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestState;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnState;

/** Private, bounded read projections used by purchasing screens. */
public final class PurchasingDirectoryQueries {
    private PurchasingDirectoryQueries() {
    }

    public record RequestCriteria(
            Optional<String> text, Optional<PurchaseRequestState> state,
            int offset, int limit) {
        public RequestCriteria {
            text = normalize(text);
            state = Objects.requireNonNull(state, "state");
            page(offset, limit);
        }
    }

    public record OrderCriteria(
            Optional<String> text, Optional<PurchaseOrderState> state,
            int offset, int limit) {
        public OrderCriteria {
            text = normalize(text);
            state = Objects.requireNonNull(state, "state");
            page(offset, limit);
        }
    }

    public record ReceiptCriteria(
            Optional<String> text, Optional<GoodsReceiptState> state,
            int offset, int limit) {
        public ReceiptCriteria {
            text = normalize(text);
            state = Objects.requireNonNull(state, "state");
            page(offset, limit);
        }
    }

    public record ReturnCriteria(
            Optional<String> text, Optional<SupplierReturnState> state,
            int offset, int limit) {
        public ReturnCriteria {
            text = normalize(text);
            state = Objects.requireNonNull(state, "state");
            page(offset, limit);
        }
    }

    public record Page<T>(List<T> items, long total, int offset, int limit) {
        public Page {
            items = List.copyOf(Objects.requireNonNull(items, "items"));
            page(offset, limit);
            if (total < items.size() || offset > total) {
                throw new IllegalArgumentException("Invalid purchasing page metadata");
            }
        }
    }

    public record RequestSummary(
            PurchaseRequestId id, String number, LocalDate requestedOn,
            PurchaseRequestState state, long lineCount, long version) {
        public RequestSummary {
            Objects.requireNonNull(id, "id");
            number = text(number, "number");
            Objects.requireNonNull(requestedOn, "requestedOn");
            Objects.requireNonNull(state, "state");
            nonNegative(lineCount, version);
        }
    }

    public record OrderSummary(
            PurchaseOrderId id, String number, String supplierName, String currencyCode,
            PurchaseOrderState state, long lineCount, long version) {
        public OrderSummary {
            Objects.requireNonNull(id, "id");
            number = text(number, "number");
            supplierName = text(supplierName, "supplierName");
            currencyCode = text(currencyCode, "currencyCode");
            Objects.requireNonNull(state, "state");
            nonNegative(lineCount, version);
        }
    }

    public record ReceiptSummary(
            GoodsReceiptId id, String number, PurchaseOrderId orderId,
            GoodsReceiptState state, long lineCount, long version) {
        public ReceiptSummary {
            Objects.requireNonNull(id, "id");
            number = text(number, "number");
            Objects.requireNonNull(orderId, "orderId");
            Objects.requireNonNull(state, "state");
            nonNegative(lineCount, version);
        }
    }

    public record ReturnSummary(
            SupplierReturnId id, String number, PurchaseOrderId orderId,
            String reason, SupplierReturnState state, long lineCount, long version) {
        public ReturnSummary {
            Objects.requireNonNull(id, "id");
            number = text(number, "number");
            Objects.requireNonNull(orderId, "orderId");
            reason = text(reason, "reason");
            Objects.requireNonNull(state, "state");
            nonNegative(lineCount, version);
        }
    }

    private static Optional<String> normalize(Optional<String> value) {
        return Objects.requireNonNull(value, "text")
                .map(String::strip).filter(text -> !text.isEmpty())
                .map(text -> text.toLowerCase(Locale.ROOT));
    }

    private static void page(int offset, int limit) {
        if (offset < 0 || offset > 100_000 || limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Invalid purchasing page");
        }
    }

    private static String text(String value, String field) {
        value = Objects.requireNonNull(value, field).strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static void nonNegative(long lineCount, long version) {
        if (lineCount < 0 || version < 0) {
            throw new IllegalArgumentException("Invalid line count or version");
        }
    }
}
