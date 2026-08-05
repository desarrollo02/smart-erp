package py.com.logixone.plugins.businesspartners.application.query;

import java.util.List;
import java.util.Objects;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerReference;

public record BusinessPartnerSearchPage(
        List<BusinessPartnerReference> items,
        long total,
        int offset,
        int limit) {

    public BusinessPartnerSearchPage {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (total < 0 || offset < 0 || limit < 1 || items.size() > limit) {
            throw new IllegalArgumentException("Invalid business partner page metadata");
        }
    }
}
