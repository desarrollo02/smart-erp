package py.com.logixone.plugins.commercialcatalog.application;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPriceQuote;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPriceQuoteRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPricing;
import py.com.logixone.plugins.commercialcatalog.application.port.PriceListRepository;

/** Public deterministic price lookup for already-authorized plugin use cases. */
public final class RepositoryCatalogPricing implements CatalogPricing {

    private final PriceListRepository priceLists;

    public RepositoryCatalogPricing(PriceListRepository priceLists) {
        this.priceLists = Objects.requireNonNull(priceLists, "priceLists");
    }

    @Override
    public Optional<CatalogPriceQuote> quote(
            CompanyId companyId, CatalogPriceQuoteRequest request) {
        Objects.requireNonNull(request, "request");
        return priceLists.findById(
                        Objects.requireNonNull(companyId, "companyId"), request.priceListId())
                .flatMap(list -> list.quote(request));
    }
}
