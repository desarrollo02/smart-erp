package py.com.logixone.plugins.commercialcatalog.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of a company-scoped price list. */
public record PriceListId(UUID value) implements Comparable<PriceListId> {

    public PriceListId {
        Objects.requireNonNull(value, "value");
    }

    public static PriceListId parse(String value) {
        Objects.requireNonNull(value, "value");
        UUID parsed;
        try {
            parsed = UUID.fromString(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Price list id must be a canonical UUID", failure);
        }
        if (!parsed.toString().equals(value)) {
            throw new IllegalArgumentException("Price list id must be a canonical lower-case UUID");
        }
        return new PriceListId(parsed);
    }

    @Override
    public int compareTo(PriceListId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
