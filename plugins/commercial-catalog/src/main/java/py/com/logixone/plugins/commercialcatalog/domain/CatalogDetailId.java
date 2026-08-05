package py.com.logixone.plugins.commercialcatalog.domain;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity for an item-owned detail such as an identifier. */
public record CatalogDetailId(UUID value) {

    public CatalogDetailId {
        Objects.requireNonNull(value, "value");
    }
}
