package py.com.logixone.plugins.inventory.api;

import java.util.List;
import java.util.Objects;

/** One bounded page of public warehouse projections. */
public record StorageSearchPage(
        List<WarehouseReference> items,
        long total,
        int offset,
        int limit) {

    public StorageSearchPage {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (total < items.size() || offset < 0 || limit < 1 || limit > 100
                || items.size() > limit || offset > total) {
            throw new IllegalArgumentException("Invalid warehouse search page");
        }
    }
}
