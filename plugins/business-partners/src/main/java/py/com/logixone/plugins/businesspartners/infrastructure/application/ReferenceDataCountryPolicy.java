package py.com.logixone.plugins.businesspartners.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.application.port.CountryReferencePolicy;
import py.com.logixone.plugins.referencedata.api.CountryCode;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;

/** Resolves country validity exclusively through the public reference-data contract. */
@ApplicationScoped
public class ReferenceDataCountryPolicy implements CountryReferencePolicy {

    @Inject
    ReferenceDataDirectory directory;

    @Override
    public boolean isEnabled(CompanyId companyId, String alpha2Code) {
        return directory.findCountry(companyId, new CountryCode(alpha2Code))
                .filter(country -> country.enabled())
                .isPresent();
    }
}
