package py.com.logixone.plugins.commercialcatalog.application.query;

import java.util.Objects;
import py.com.logixone.plugins.commercialcatalog.api.CatalogTaxMode;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListCode;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListName;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListState;

public record PriceListSummary(
        PriceListId id,
        PriceListCode code,
        PriceListName name,
        String currency,
        CatalogTaxMode taxMode,
        PriceListState state,
        long entries,
        long activeEntries,
        long version) {
    public PriceListSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(taxMode, "taxMode");
        Objects.requireNonNull(state, "state");
        if (entries < 0 || activeEntries < 0 || activeEntries > entries || version < 0) {
            throw new IllegalArgumentException("Invalid price-list summary counts or version");
        }
    }
}
