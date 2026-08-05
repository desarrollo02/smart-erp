package py.com.logixone.plugins.businesspartners.application.port;

import java.util.List;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchCriteria;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchPage;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartner;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerIdentificationKey;

public interface BusinessPartnerRepository {

    Optional<BusinessPartner> findById(CompanyId companyId, BusinessPartnerId id);

    BusinessPartner insert(BusinessPartner partner);

    BusinessPartner update(BusinessPartner partner, long expectedPersistedVersion);

    List<BusinessPartnerId> findIdentificationCandidates(
            CompanyId companyId, BusinessPartnerIdentificationKey candidate);

    BusinessPartnerSearchPage search(
            CompanyId companyId, BusinessPartnerSearchCriteria criteria);
}
