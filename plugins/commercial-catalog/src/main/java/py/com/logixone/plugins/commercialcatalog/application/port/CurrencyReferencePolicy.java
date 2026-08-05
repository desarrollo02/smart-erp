package py.com.logixone.plugins.commercialcatalog.application.port;

import py.com.logixone.kernel.api.company.CompanyId;

/** Consumer-owned boundary for validating company-enabled ISO 4217 currencies. */
@FunctionalInterface
public interface CurrencyReferencePolicy {

    boolean isEnabled(CompanyId companyId, String alphabeticCode);
}
