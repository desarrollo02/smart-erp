package py.com.logixone.plugins.businesspartners.api;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

/** Company-scoped synchronous lookup contract owned by business_partners. */
public interface BusinessPartnerDirectory {

    Optional<BusinessPartnerReference> findById(
            CompanyId companyId, BusinessPartnerId businessPartnerId);

    BusinessPartnerSearchPage search(
            CompanyId companyId, BusinessPartnerSearchQuery query);
}
