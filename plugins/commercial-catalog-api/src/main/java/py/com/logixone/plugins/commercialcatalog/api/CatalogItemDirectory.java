package py.com.logixone.plugins.commercialcatalog.api;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

/** Company-scoped synchronous lookup and search owned by commercial_catalog. */
public interface CatalogItemDirectory {

    Optional<CatalogItemReference> findById(CompanyId companyId, CatalogItemId itemId);

    CatalogSearchPage search(CompanyId companyId, CatalogSearchCriteria criteria);
}
