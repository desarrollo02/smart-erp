package py.com.logixone.plugins.referencedata.api;

import java.util.List;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

/** Public read boundary; callers never receive persistence objects. */
public interface ReferenceDataDirectory {

    ReferenceDataRelease currentRelease(CompanyId companyId, ReferenceDataCatalog catalog);

    List<CountryReference> countries(CompanyId companyId);

    Optional<CountryReference> findCountry(CompanyId companyId, CountryCode code);

    List<CurrencyReference> currencies(CompanyId companyId);

    Optional<CurrencyReference> findCurrency(CompanyId companyId, CurrencyCode code);
}
