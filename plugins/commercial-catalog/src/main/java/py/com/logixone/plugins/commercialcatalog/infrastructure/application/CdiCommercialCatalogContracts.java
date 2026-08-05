package py.com.logixone.plugins.commercialcatalog.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemReference;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPriceQuote;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPriceQuoteRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPricing;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchPage;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionResult;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversions;
import py.com.logixone.plugins.commercialcatalog.application.RepositoryCatalogItemDirectory;
import py.com.logixone.plugins.commercialcatalog.application.RepositoryCatalogPricing;
import py.com.logixone.plugins.commercialcatalog.application.RepositoryCatalogUnitConversions;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogItemRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.PriceListRepository;

/** CDI/JTA adapters for the stable contracts published by commercial-catalog-api. */
@ApplicationScoped
@Transactional(TxType.SUPPORTS)
public class CdiCommercialCatalogContracts
        implements CatalogItemDirectory, CatalogUnitConversions, CatalogPricing {

    @Inject CatalogItemRepository items;
    @Inject PriceListRepository priceLists;

    @Override
    public Optional<CatalogItemReference> findById(
            CompanyId companyId, CatalogItemId itemId) {
        return new RepositoryCatalogItemDirectory(items).findById(companyId, itemId);
    }

    @Override
    public CatalogSearchPage search(CompanyId companyId, CatalogSearchCriteria criteria) {
        return new RepositoryCatalogItemDirectory(items).search(companyId, criteria);
    }

    @Override
    public Optional<CatalogUnitConversionResult> convert(
            CompanyId companyId, CatalogUnitConversionRequest request) {
        return new RepositoryCatalogUnitConversions(items).convert(companyId, request);
    }

    @Override
    public Optional<CatalogPriceQuote> quote(
            CompanyId companyId, CatalogPriceQuoteRequest request) {
        return new RepositoryCatalogPricing(priceLists).quote(companyId, request);
    }
}
