package py.com.logixone.plugins.businesspartners.api;

import java.util.List;
import java.util.Objects;

/** One bounded page of public partner references. */
public record BusinessPartnerSearchPage(
        List<BusinessPartnerReference> items,
        long total,
        int offset,
        int limit) {

    public BusinessPartnerSearchPage {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (total < items.size() || offset < 0 || limit < 1 || limit > 100
                || items.size() > limit || offset > total) {
            throw new IllegalArgumentException("Invalid business partner search page");
        }
    }
}
