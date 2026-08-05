package py.com.logixone.plugins.businesspartners.application;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerDirectory;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerReference;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerRepository;

/** Public synchronous projection; callers must already hold their own authorized use case. */
public final class RepositoryBusinessPartnerDirectory implements BusinessPartnerDirectory {

    private final BusinessPartnerRepository repository;

    public RepositoryBusinessPartnerDirectory(BusinessPartnerRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public Optional<BusinessPartnerReference> findById(
            CompanyId companyId, BusinessPartnerId partnerId) {
        return repository.findById(
                        Objects.requireNonNull(companyId, "companyId"),
                        Objects.requireNonNull(partnerId, "partnerId"))
                .map(value -> value.toReference());
    }
}
