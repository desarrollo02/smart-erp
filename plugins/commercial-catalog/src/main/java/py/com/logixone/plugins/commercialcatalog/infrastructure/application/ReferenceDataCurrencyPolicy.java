package py.com.logixone.plugins.commercialcatalog.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.application.port.CurrencyReferencePolicy;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;

/** Resolves currency validity through the public reference-data contract only. */
@ApplicationScoped
public class ReferenceDataCurrencyPolicy implements CurrencyReferencePolicy {

    @Inject
    ReferenceDataDirectory directory;

    @Override
    public boolean isEnabled(CompanyId companyId, String alphabeticCode) {
        return directory.findCurrency(companyId, new CurrencyCode(alphabeticCode))
                .filter(currency -> currency.enabled())
                .isPresent();
    }
}
