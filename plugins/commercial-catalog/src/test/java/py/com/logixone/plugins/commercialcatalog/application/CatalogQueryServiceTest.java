package py.com.logixone.plugins.commercialcatalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPriceQuoteRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchPage;
import py.com.logixone.plugins.commercialcatalog.api.CatalogTaxMode;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionRequest;
import py.com.logixone.plugins.commercialcatalog.api.PriceEntryId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogItemRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.PriceListRepository;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchPage;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSummary;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItem;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemCode;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemName;
import py.com.logixone.plugins.commercialcatalog.domain.ItemUnitConversion;
import py.com.logixone.plugins.commercialcatalog.domain.PriceEntry;
import py.com.logixone.plugins.commercialcatalog.domain.PriceList;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListCode;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListName;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileReference;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;
import py.com.logixone.plugins.commercialcatalog.domain.UnitPurpose;

class CatalogQueryServiceTest {

    private static final CompanyId COMPANY_A = company(1);
    private static final CompanyId COMPANY_B = company(2);
    private static final CatalogItemId ITEM_ID = new CatalogItemId(new UUID(0, 10));
    private static final PriceListId PRICE_LIST_ID = new PriceListId(new UUID(0, 20));
    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void requiresViewPermissionAndKeepsSearchInsideTheAuthenticatedCompany() {
        MemoryItems items = new MemoryItems();
        items.insert(newItem(COMPANY_A));
        items.insert(newItem(COMPANY_B));
        items.searches = 0;
        CatalogQueryService queries = new CatalogQueryService(items, new MemoryPriceLists());
        CatalogSearchCriteria criteria = new CatalogSearchCriteria(
                "", Set.of(), Set.of(CatalogItemState.ACTIVE), 0, 20);

        var denied = queries.search(context(CommercialCatalogPermissions.ITEMS_MANAGE), criteria);
        var allowed = queries.search(context(CommercialCatalogPermissions.VIEW), criteria);

        assertEquals(CatalogResultCode.ACCESS_DENIED, denied.code());
        assertTrue(allowed.successful());
        assertEquals(1, allowed.value().orElseThrow().total());
        assertEquals(1, items.searches);
    }

    @Test
    void exposesDeterministicConversionsAndQuotesThroughInternalAndPublicContracts() {
        MemoryItems items = new MemoryItems();
        CatalogItem item = newItem(COMPANY_A);
        item.addUnitConversion(new ItemUnitConversion(
                new UnitCode("CJ"), new BigDecimal("12"), Set.of(UnitPurpose.SALE),
                Set.of(), true), 0);
        items.insert(item);
        MemoryPriceLists prices = new MemoryPriceLists();
        PriceList priceList = PriceList.create(
                COMPANY_A, PRICE_LIST_ID, new PriceListCode("MINORISTA"),
                new PriceListName("Minorista"), "PYG", CatalogTaxMode.TAX_INCLUDED,
                0, RoundingMode.HALF_UP);
        priceList.addEntry(PriceEntry.active(
                new PriceEntryId(new UUID(0, 21)), ITEM_ID, new UnitCode("UN"),
                BigDecimal.ONE, new BigDecimal("2500"), NOW.minusSeconds(60),
                Optional.empty()), 0);
        prices.insert(priceList);
        CatalogQueryService queries = new CatalogQueryService(items, prices);
        CatalogUnitConversionRequest conversion = new CatalogUnitConversionRequest(
                ITEM_ID, "CJ", "UN", new BigDecimal("2"));
        CatalogPriceQuoteRequest quote = new CatalogPriceQuoteRequest(
                PRICE_LIST_ID, ITEM_ID, "UN", new BigDecimal("3"), NOW);

        var converted = queries.convert(context(CommercialCatalogPermissions.VIEW), conversion);
        var quoted = queries.quote(context(CommercialCatalogPermissions.VIEW), quote);
        var publicConverted = new RepositoryCatalogUnitConversions(items)
                .convert(COMPANY_A, conversion);
        var publicQuote = new RepositoryCatalogPricing(prices).quote(COMPANY_A, quote);

        assertEquals(new BigDecimal("24"),
                converted.value().orElseThrow().convertedQuantity());
        assertEquals(new BigDecimal("7500"), quoted.value().orElseThrow().totalAmount());
        assertEquals(new BigDecimal("24"), publicConverted.orElseThrow().convertedQuantity());
        assertEquals(new BigDecimal("7500"), publicQuote.orElseThrow().totalAmount());
    }

    @Test
    void listsPriceListsOnlyWithViewPermissionAndInsideTheCompany() {
        MemoryPriceLists prices = new MemoryPriceLists();
        prices.insert(newPriceList(COMPANY_A));
        prices.insert(newPriceList(COMPANY_B));
        CatalogQueryService queries = new CatalogQueryService(new MemoryItems(), prices);
        PriceListSearchCriteria criteria = new PriceListSearchCriteria(
                "", Set.of(), 0, 20);

        var denied = queries.priceLists(
                context(CommercialCatalogPermissions.PRICES_MANAGE), criteria);
        var allowed = queries.priceLists(
                context(CommercialCatalogPermissions.VIEW), criteria);
        var detail = queries.priceListDetail(
                context(CommercialCatalogPermissions.VIEW), PRICE_LIST_ID);
        var deniedDetail = queries.priceListDetail(
                context(CommercialCatalogPermissions.PRICES_MANAGE), PRICE_LIST_ID);

        assertEquals(CatalogResultCode.ACCESS_DENIED, denied.code());
        assertEquals(1, allowed.value().orElseThrow().total());
        assertEquals("MINORISTA", detail.value().orElseThrow().code().value());
        assertEquals(CatalogResultCode.ACCESS_DENIED, deniedDetail.code());
    }

    private static CatalogOperationContext context(ContributionId permission) {
        return new CatalogOperationContext(
                new AuthenticatedCompanyContext(
                        new AuthenticatedActor(new AppUserId(new UUID(0, 99))), COMPANY_A),
                CommercialCatalogIdentity.PLUGIN_ID, permission, "request:catalog-query");
    }

    private static CatalogItem newItem(CompanyId companyId) {
        return CatalogItem.create(
                companyId, ITEM_ID, new CatalogItemCode("ITEM-1"),
                new CatalogItemName("Artículo"), "", CatalogItemType.PRODUCT,
                Set.of(CatalogItemScope.SALE), new UnitCode("UN"),
                new TaxProfileReference(new TaxProfileId(new UUID(0, 30)), 0));
    }

    private static PriceList newPriceList(CompanyId companyId) {
        return PriceList.create(
                companyId, PRICE_LIST_ID, new PriceListCode("MINORISTA"),
                new PriceListName("Minorista"), "PYG", CatalogTaxMode.TAX_INCLUDED,
                0, RoundingMode.HALF_UP);
    }

    private static CompanyId company(long suffix) {
        return new CompanyId(new UUID(0, suffix));
    }

    private static final class MemoryItems implements CatalogItemRepository {
        private final Map<String, CatalogItem> values = new HashMap<>();
        private int searches;

        @Override
        public Optional<CatalogItem> findById(CompanyId companyId, CatalogItemId itemId) {
            return Optional.ofNullable(values.get(companyId + ":" + itemId));
        }

        @Override
        public CatalogSearchPage search(CompanyId companyId, CatalogSearchCriteria criteria) {
            searches++;
            var references = values.values().stream()
                    .filter(item -> item.companyId().equals(companyId))
                    .filter(item -> criteria.states().isEmpty()
                            || criteria.states().contains(item.state()))
                    .map(CatalogItem::reference)
                    .toList();
            return new CatalogSearchPage(
                    references, references.size(), criteria.offset(), criteria.limit());
        }

        @Override
        public CatalogItem insert(CatalogItem item) {
            values.put(item.companyId() + ":" + item.id(), item);
            return item;
        }

        @Override
        public CatalogItem update(CatalogItem item, long expectedPersistedVersion) {
            return insert(item);
        }
    }

    private static final class MemoryPriceLists implements PriceListRepository {
        private final Map<String, PriceList> values = new HashMap<>();

        @Override
        public Optional<PriceList> findById(CompanyId companyId, PriceListId priceListId) {
            return Optional.ofNullable(values.get(companyId + ":" + priceListId));
        }

        @Override
        public PriceListSearchPage search(
                CompanyId companyId, PriceListSearchCriteria criteria) {
            var summaries = values.values().stream()
                    .filter(list -> list.companyId().equals(companyId))
                    .map(list -> new PriceListSummary(
                            list.id(), list.code(), list.name(), list.currency(), list.taxMode(),
                            list.state(), list.entries().size(),
                            list.entries().values().stream().filter(entry -> entry.active()).count(),
                            list.version()))
                    .toList();
            return new PriceListSearchPage(
                    summaries, summaries.size(), criteria.offset(), criteria.limit());
        }

        @Override
        public PriceList insert(PriceList priceList) {
            values.put(priceList.companyId() + ":" + priceList.id(), priceList);
            return priceList;
        }

        @Override
        public PriceList update(PriceList priceList, long expectedPersistedVersion) {
            return insert(priceList);
        }
    }
}
