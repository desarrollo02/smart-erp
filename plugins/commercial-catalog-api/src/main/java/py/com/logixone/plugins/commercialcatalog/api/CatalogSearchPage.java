package py.com.logixone.plugins.commercialcatalog.api;

import java.util.List;
import java.util.Objects;

/** Immutable page returned by the public catalog directory. */
public record CatalogSearchPage(
        List<CatalogItemReference> items, long total, int offset, int limit) {

    public CatalogSearchPage {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (total < 0 || offset < 0 || limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Invalid catalog search page metadata");
        }
        if (items.size() > limit || total < items.size()) {
            throw new IllegalArgumentException("Catalog search page contents are inconsistent");
        }
    }
}
