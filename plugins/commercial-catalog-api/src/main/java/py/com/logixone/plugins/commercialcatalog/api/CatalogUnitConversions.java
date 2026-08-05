package py.com.logixone.plugins.commercialcatalog.api;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

/** Company-scoped item conversion contract owned by commercial_catalog. */
public interface CatalogUnitConversions {

    Optional<CatalogUnitConversionResult> convert(
            CompanyId companyId, CatalogUnitConversionRequest request);
}
