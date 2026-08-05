package py.com.logixone.plugins.commercialcatalog.domain;

import java.util.Objects;

/** Versioned reference to the catalog's internal tax profile. */
public record TaxProfileReference(TaxProfileId id, long version) {
    public TaxProfileReference {
        Objects.requireNonNull(id, "id");
        if (version < 0) {
            throw new IllegalArgumentException("Tax profile version must not be negative");
        }
    }
}
