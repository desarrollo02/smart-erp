package py.com.logixone.plugins.commercialcatalog.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.domain.PriceList;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchPage;

public interface PriceListRepository {
    Optional<PriceList> findById(CompanyId companyId, PriceListId priceListId);
    PriceListSearchPage search(CompanyId companyId, PriceListSearchCriteria criteria);
    PriceList insert(PriceList priceList);
    PriceList update(PriceList priceList, long expectedPersistedVersion);
}
