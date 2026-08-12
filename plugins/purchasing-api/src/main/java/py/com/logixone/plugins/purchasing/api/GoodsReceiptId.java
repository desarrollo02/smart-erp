package py.com.logixone.plugins.purchasing.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of a goods or service receipt. */
public record GoodsReceiptId(UUID value) implements Comparable<GoodsReceiptId> {
    public GoodsReceiptId { Objects.requireNonNull(value, "value"); }
    public static GoodsReceiptId parse(String value) {
        return new GoodsReceiptId(ContractValues.uuid(value, "goodsReceiptId"));
    }
    @Override public int compareTo(GoodsReceiptId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }
    @Override public String toString() { return value.toString(); }
}
