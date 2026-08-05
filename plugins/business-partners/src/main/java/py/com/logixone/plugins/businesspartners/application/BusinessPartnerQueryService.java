package py.com.logixone.plugins.businesspartners.application;

import java.util.List;
import java.util.Objects;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerRepository;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchCriteria;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchPage;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerIdentificationKey;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerSnapshot;

/** Company-scoped read use cases. Trusted access is rechecked at the boundary. */
public final class BusinessPartnerQueryService {

    private final BusinessPartnerRepository repository;

    public BusinessPartnerQueryService(BusinessPartnerRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public BusinessPartnerOperationResult<BusinessPartnerSearchPage> search(
            BusinessPartnerOperationContext context,
            BusinessPartnerSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");
        if (!authorized(context)) {
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.ACCESS_DENIED);
        }
        return BusinessPartnerOperationResult.success(repository.search(
                context.companyContext().companyId(), criteria), List.of());
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> detail(
            BusinessPartnerOperationContext context,
            BusinessPartnerId id) {
        Objects.requireNonNull(id, "id");
        if (!authorized(context)) {
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.ACCESS_DENIED);
        }
        return repository.findById(context.companyContext().companyId(), id)
                .map(value -> BusinessPartnerOperationResult.success(value.snapshot(), List.of()))
                .orElseGet(() -> BusinessPartnerOperationResult.failure(
                        BusinessPartnerResultCode.NOT_FOUND));
    }

    public BusinessPartnerOperationResult<List<BusinessPartnerId>> duplicateCandidates(
            BusinessPartnerOperationContext context,
            BusinessPartnerIdentificationKey candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (!authorized(context)) {
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.ACCESS_DENIED);
        }
        return BusinessPartnerOperationResult.success(repository.findIdentificationCandidates(
                context.companyContext().companyId(), candidate), List.of());
    }

    private static boolean authorized(BusinessPartnerOperationContext context) {
        return Objects.requireNonNull(context, "context").authorizes(BusinessPartnerPermissions.VIEW);
    }
}
