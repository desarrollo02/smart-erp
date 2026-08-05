package py.com.logixone.plugins.commercialcatalog.api;

import java.text.Normalizer;
import java.util.Objects;
import java.util.Set;

/** Stable company-scoped catalog search contract. */
public record CatalogSearchCriteria(
        String query,
        Set<CatalogItemType> types,
        Set<CatalogItemState> states,
        int offset,
        int limit) {

    public CatalogSearchCriteria {
        Objects.requireNonNull(query, "query");
        query = Normalizer.normalize(query, Normalizer.Form.NFKC).trim();
        if (query.length() > 100) {
            throw new IllegalArgumentException("query must not exceed 100 characters");
        }
        types = Set.copyOf(Objects.requireNonNull(types, "types"));
        states = Set.copyOf(Objects.requireNonNull(states, "states"));
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
    }
}
