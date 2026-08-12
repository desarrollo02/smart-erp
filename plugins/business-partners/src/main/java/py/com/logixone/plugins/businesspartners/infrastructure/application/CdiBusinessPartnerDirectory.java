package py.com.logixone.plugins.businesspartners.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerDirectory;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerReference;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerSearchPage;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerSearchQuery;
import py.com.logixone.plugins.businesspartners.application.RepositoryBusinessPartnerDirectory;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerRepository;

@ApplicationScoped
public class CdiBusinessPartnerDirectory implements BusinessPartnerDirectory {

    @Inject
    BusinessPartnerRepository repository;

    @Override
    @Transactional(TxType.SUPPORTS)
    public Optional<BusinessPartnerReference> findById(
            CompanyId companyId, BusinessPartnerId partnerId) {
        return new RepositoryBusinessPartnerDirectory(repository).findById(companyId, partnerId);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public BusinessPartnerSearchPage search(
            CompanyId companyId, BusinessPartnerSearchQuery query) {
        return new RepositoryBusinessPartnerDirectory(repository).search(companyId, query);
    }
}
