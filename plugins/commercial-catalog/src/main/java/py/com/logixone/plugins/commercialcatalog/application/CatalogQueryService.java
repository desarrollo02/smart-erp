package py.com.logixone.plugins.commercialcatalog.application;

import java.util.Objects;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPriceQuote;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPriceQuoteRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchPage;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionResult;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogItemRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.PriceListRepository;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchPage;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemSnapshot;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListSnapshot;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;

/** Company-scoped read use cases with authorization rechecked at the boundary. */
public final class CatalogQueryService {

    private final CatalogItemRepository items;
    private final PriceListRepository priceLists;

    public CatalogQueryService(
            CatalogItemRepository items, PriceListRepository priceLists) {
        this.items = Objects.requireNonNull(items, "items");
        this.priceLists = Objects.requireNonNull(priceLists, "priceLists");
    }

    public CatalogOperationResult<CatalogSearchPage> search(
            CatalogOperationContext context, CatalogSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");
        if (!authorized(context)) {
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        return CatalogOperationResult.success(items.search(
                context.companyContext().companyId(), criteria));
    }

    public CatalogOperationResult<CatalogItemSnapshot> detail(
            CatalogOperationContext context, CatalogItemId itemId) {
        Objects.requireNonNull(itemId, "itemId");
        if (!authorized(context)) {
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        return items.findById(context.companyContext().companyId(), itemId)
                .map(item -> CatalogOperationResult.success(item.snapshot()))
                .orElseGet(() -> CatalogOperationResult.failure(CatalogResultCode.NOT_FOUND));
    }

    public CatalogOperationResult<CatalogUnitConversionResult> convert(
            CatalogOperationContext context, CatalogUnitConversionRequest request) {
        Objects.requireNonNull(request, "request");
        if (!authorized(context)) {
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        return items.findById(context.companyContext().companyId(), request.itemId())
                .map(item -> {
                    try {
                        return CatalogOperationResult.success(item.convert(
                                new UnitCode(request.sourceUnitCode()),
                                new UnitCode(request.targetUnitCode()), request.quantity()));
                    } catch (IllegalArgumentException failure) {
                        return CatalogOperationResult.<CatalogUnitConversionResult>failure(
                                CatalogResultCode.INVALID_OPERATION);
                    }
                })
                .orElseGet(() -> CatalogOperationResult.failure(CatalogResultCode.NOT_FOUND));
    }

    public CatalogOperationResult<CatalogPriceQuote> quote(
            CatalogOperationContext context, CatalogPriceQuoteRequest request) {
        Objects.requireNonNull(request, "request");
        if (!authorized(context)) {
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        return priceLists.findById(
                        context.companyContext().companyId(), request.priceListId())
                .flatMap(list -> list.quote(request))
                .map(CatalogOperationResult::success)
                .orElseGet(() -> CatalogOperationResult.failure(CatalogResultCode.NOT_FOUND));
    }

    public CatalogOperationResult<PriceListSearchPage> priceLists(
            CatalogOperationContext context, PriceListSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");
        if (!authorized(context)) {
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        return CatalogOperationResult.success(priceLists.search(
                context.companyContext().companyId(), criteria));
    }

    public CatalogOperationResult<PriceListSnapshot> priceListDetail(
            CatalogOperationContext context, PriceListId priceListId) {
        Objects.requireNonNull(priceListId, "priceListId");
        if (!authorized(context)) {
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        return priceLists.findById(context.companyContext().companyId(), priceListId)
                .map(list -> CatalogOperationResult.success(list.snapshot()))
                .orElseGet(() -> CatalogOperationResult.failure(CatalogResultCode.NOT_FOUND));
    }

    private static boolean authorized(CatalogOperationContext context) {
        return Objects.requireNonNull(context, "context")
                .authorizes(CommercialCatalogPermissions.VIEW);
    }
}
