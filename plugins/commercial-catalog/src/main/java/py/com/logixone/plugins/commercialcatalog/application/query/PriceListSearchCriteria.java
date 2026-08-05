package py.com.logixone.plugins.commercialcatalog.application.query;

import java.text.Normalizer;
import java.util.Objects;
import java.util.Set;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListState;

public record PriceListSearchCriteria(
        String query, Set<PriceListState> states, int offset, int limit) {
    public PriceListSearchCriteria {
        query = Normalizer.normalize(
                Objects.requireNonNull(query, "query"), Normalizer.Form.NFKC).trim();
        if (query.length() > 100) {
            throw new IllegalArgumentException("query must not exceed 100 characters");
        }
        states = Set.copyOf(Objects.requireNonNull(states, "states"));
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
    }
}
