package py.com.logixone.plugins.commercialcatalog.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchPage;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItem;

public interface CatalogItemRepository {
    Optional<CatalogItem> findById(CompanyId companyId, CatalogItemId itemId);
    CatalogSearchPage search(CompanyId companyId, CatalogSearchCriteria criteria);
    CatalogItem insert(CatalogItem item);
    CatalogItem update(CatalogItem item, long expectedPersistedVersion);
}
