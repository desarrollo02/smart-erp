package py.com.logixone.plugins.commercialcatalog.application;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemReference;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchPage;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogItemRepository;

/** Public projection for callers that already hold their own authorized use case. */
public final class RepositoryCatalogItemDirectory implements CatalogItemDirectory {

    private final CatalogItemRepository items;

    public RepositoryCatalogItemDirectory(CatalogItemRepository items) {
        this.items = Objects.requireNonNull(items, "items");
    }

    @Override
    public Optional<CatalogItemReference> findById(
            CompanyId companyId, CatalogItemId itemId) {
        return items.findById(
                        Objects.requireNonNull(companyId, "companyId"),
                        Objects.requireNonNull(itemId, "itemId"))
                .map(item -> item.reference());
    }

    @Override
    public CatalogSearchPage search(
            CompanyId companyId, CatalogSearchCriteria criteria) {
        return items.search(
                Objects.requireNonNull(companyId, "companyId"),
                Objects.requireNonNull(criteria, "criteria"));
    }
}
