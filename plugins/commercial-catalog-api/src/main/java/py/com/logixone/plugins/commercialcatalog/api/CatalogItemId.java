package py.com.logixone.plugins.commercialcatalog.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity used to reference a catalog item across plugin boundaries. */
public record CatalogItemId(UUID value) implements Comparable<CatalogItemId> {

    public CatalogItemId {
        Objects.requireNonNull(value, "value");
    }

    public static CatalogItemId parse(String value) {
        Objects.requireNonNull(value, "value");
        UUID parsed;
        try {
            parsed = UUID.fromString(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Catalog item id must be a canonical UUID", failure);
        }
        if (!parsed.toString().equals(value)) {
            throw new IllegalArgumentException(
                    "Catalog item id must be a canonical lower-case UUID");
        }
        return new CatalogItemId(parsed);
    }

    @Override
    public int compareTo(CatalogItemId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
