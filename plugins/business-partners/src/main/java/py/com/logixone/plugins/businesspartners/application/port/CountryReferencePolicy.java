package py.com.logixone.plugins.businesspartners.application.port;

import py.com.logixone.kernel.api.company.CompanyId;

/** Consumer-owned port that prevents business partners from depending on reference-data internals. */
@FunctionalInterface
public interface CountryReferencePolicy {

    boolean isEnabled(CompanyId companyId, String alpha2Code);
}
