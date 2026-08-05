package py.com.logixone.plugins.commercialcatalog.api;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

/** Company-scoped deterministic quote contract owned by commercial_catalog. */
public interface CatalogPricing {

    Optional<CatalogPriceQuote> quote(
            CompanyId companyId, CatalogPriceQuoteRequest request);
}
