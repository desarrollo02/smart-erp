package py.com.logixone.plugins.commercialcatalog.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity of the price entry used by a quote. */
public record PriceEntryId(UUID value) implements Comparable<PriceEntryId> {

    public PriceEntryId {
        Objects.requireNonNull(value, "value");
    }

    public static PriceEntryId parse(String value) {
        Objects.requireNonNull(value, "value");
        UUID parsed;
        try {
            parsed = UUID.fromString(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Price entry id must be a canonical UUID", failure);
        }
        if (!parsed.toString().equals(value)) {
            throw new IllegalArgumentException("Price entry id must be a canonical lower-case UUID");
        }
        return new PriceEntryId(parsed);
    }

    @Override
    public int compareTo(PriceEntryId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
