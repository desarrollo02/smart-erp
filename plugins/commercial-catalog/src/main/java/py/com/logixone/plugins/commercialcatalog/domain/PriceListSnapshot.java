package py.com.logixone.plugins.commercialcatalog.domain;

import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogTaxMode;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;

/** Complete neutral price-list state used by its private persistence adapter. */
public record PriceListSnapshot(
        CompanyId companyId,
        PriceListId id,
        PriceListCode code,
        PriceListName name,
        String currency,
        CatalogTaxMode taxMode,
        int scale,
        RoundingMode roundingMode,
        PriceListState state,
        List<PriceEntry> entries,
        long version) {

    public PriceListSnapshot {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        currency = DomainValues.currency(currency);
        Objects.requireNonNull(taxMode, "taxMode");
        if (scale < 0 || scale > 6) {
            throw new IllegalArgumentException("Price list scale must be between 0 and 6");
        }
        Objects.requireNonNull(roundingMode, "roundingMode");
        Objects.requireNonNull(state, "state");
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
