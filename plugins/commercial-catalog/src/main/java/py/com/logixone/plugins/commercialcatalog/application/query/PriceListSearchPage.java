package py.com.logixone.plugins.commercialcatalog.application.query;

import java.util.List;
import java.util.Objects;

public record PriceListSearchPage(
        List<PriceListSummary> items, long total, int offset, int limit) {
    public PriceListSearchPage {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (total < 0 || offset < 0 || limit < 1 || limit > 100
                || items.size() > limit || total < items.size()) {
            throw new IllegalArgumentException("Invalid price-list search page");
        }
    }
}
