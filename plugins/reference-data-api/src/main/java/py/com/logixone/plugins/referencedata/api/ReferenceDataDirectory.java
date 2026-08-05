package py.com.logixone.plugins.referencedata.api;

import java.util.List;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

/** Public read boundary; callers never receive persistence objects. */
public interface ReferenceDataDirectory {

    ReferenceDataRelease currentRelease(CompanyId companyId, ReferenceDataCatalog catalog);

    List<CountryReference> countries(CompanyId companyId);

    default ReferenceDataPage<CountryReference> searchCountries(
            CompanyId companyId, ReferenceDataQuery query) {
        java.util.Objects.requireNonNull(companyId, "companyId");
        java.util.Objects.requireNonNull(query, "query");
        List<CountryReference> matches = countries(companyId).stream()
                .filter(value -> !query.enabledOnly() || value.enabled())
                .filter(value -> query.matches(
                        value.code().value(),
                        value.alpha3Code(),
                        value.numericCode(),
                        value.displayName()))
                .toList();
        return page(matches, query);
    }

    Optional<CountryReference> findCountry(CompanyId companyId, CountryCode code);

    List<CurrencyReference> currencies(CompanyId companyId);

    default ReferenceDataPage<CurrencyReference> searchCurrencies(
            CompanyId companyId, ReferenceDataQuery query) {
        java.util.Objects.requireNonNull(companyId, "companyId");
        java.util.Objects.requireNonNull(query, "query");
        List<CurrencyReference> matches = currencies(companyId).stream()
                .filter(value -> !query.enabledOnly() || value.enabled())
                .filter(value -> query.matches(
                        value.code().value(), value.numericCode(), value.displayName()))
                .toList();
        return page(matches, query);
    }

    Optional<CurrencyReference> findCurrency(CompanyId companyId, CurrencyCode code);

    private static <T> ReferenceDataPage<T> page(
            List<T> matches, ReferenceDataQuery query) {
        int from = Math.min(query.offset(), matches.size());
        int to = Math.min(from + query.limit(), matches.size());
        return new ReferenceDataPage<>(
                matches.subList(from, to), matches.size(), from, query.limit());
    }
}
