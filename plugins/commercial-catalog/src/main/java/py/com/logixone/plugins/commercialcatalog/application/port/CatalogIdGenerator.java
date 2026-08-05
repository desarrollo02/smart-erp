package py.com.logixone.plugins.commercialcatalog.application.port;

import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.PriceEntryId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogDetailId;
import py.com.logixone.plugins.commercialcatalog.domain.BrandId;
import py.com.logixone.plugins.commercialcatalog.domain.CategoryId;
import py.com.logixone.plugins.commercialcatalog.domain.TagId;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;

public interface CatalogIdGenerator {
    CatalogItemId nextItemId();
    CatalogDetailId nextDetailId();
    PriceListId nextPriceListId();
    PriceEntryId nextPriceEntryId();
    CategoryId nextCategoryId();
    BrandId nextBrandId();
    TagId nextTagId();
    TaxProfileId nextTaxProfileId();
    VariantFamilyId nextVariantFamilyId();
}
