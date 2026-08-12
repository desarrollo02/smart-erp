package py.com.logixone.plugins.purchasing.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of a receipt line. */
public record GoodsReceiptLineId(UUID value) implements Comparable<GoodsReceiptLineId> {
    public GoodsReceiptLineId { Objects.requireNonNull(value, "value"); }
    public static GoodsReceiptLineId parse(String value) {
        return new GoodsReceiptLineId(ContractValues.uuid(value, "goodsReceiptLineId"));
    }
    @Override public int compareTo(GoodsReceiptLineId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }
    @Override public String toString() { return value.toString(); }
}
