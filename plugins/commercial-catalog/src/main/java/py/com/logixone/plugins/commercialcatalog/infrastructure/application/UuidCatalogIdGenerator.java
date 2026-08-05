package py.com.logixone.plugins.commercialcatalog.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.PriceEntryId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogIdGenerator;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogDetailId;
import py.com.logixone.plugins.commercialcatalog.domain.BrandId;
import py.com.logixone.plugins.commercialcatalog.domain.CategoryId;
import py.com.logixone.plugins.commercialcatalog.domain.TagId;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;

@ApplicationScoped
public class UuidCatalogIdGenerator implements CatalogIdGenerator {

    @Override
    public CatalogItemId nextItemId() {
        return new CatalogItemId(UUID.randomUUID());
    }

    @Override
    public CatalogDetailId nextDetailId() {
        return new CatalogDetailId(UUID.randomUUID());
    }

    @Override
    public PriceListId nextPriceListId() {
        return new PriceListId(UUID.randomUUID());
    }

    @Override
    public PriceEntryId nextPriceEntryId() {
        return new PriceEntryId(UUID.randomUUID());
    }

    @Override public CategoryId nextCategoryId() { return new CategoryId(UUID.randomUUID()); }
    @Override public BrandId nextBrandId() { return new BrandId(UUID.randomUUID()); }
    @Override public TagId nextTagId() { return new TagId(UUID.randomUUID()); }
    @Override public TaxProfileId nextTaxProfileId() { return new TaxProfileId(UUID.randomUUID()); }
    @Override public VariantFamilyId nextVariantFamilyId() { return new VariantFamilyId(UUID.randomUUID()); }
}
