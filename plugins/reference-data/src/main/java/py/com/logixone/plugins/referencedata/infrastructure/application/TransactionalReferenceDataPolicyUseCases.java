package py.com.logixone.plugins.referencedata.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.time.Clock;
import java.util.List;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.security.AuthorizedCompanyOperation;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;
import py.com.logixone.plugins.referencedata.application.policy.ChangeReferenceDataPolicy;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicy;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicyRepository;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicyResult;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicyRevision;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicyService;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicyUseCases;

/** JTA boundary that keeps the current override, history and audit in one operation. */
@ApplicationScoped
@Transactional(rollbackOn = RuntimeException.class)
public class TransactionalReferenceDataPolicyUseCases implements ReferenceDataPolicyUseCases {

    @Inject
    ReferenceDataPolicyRepository repository;

    @Inject
    TechnicalAudit audit;

    @Override
    @Transactional(TxType.SUPPORTS)
    public ReferenceDataPolicyResult<ReferenceDataPolicy> current(
            AuthorizedCompanyOperation authorization,
            ReferenceDataCatalog catalog,
            String code) {
        return service().current(authorization, catalog, code);
    }

    @Override
    public ReferenceDataPolicyResult<ReferenceDataPolicy> change(
            AuthorizedCompanyOperation authorization, ChangeReferenceDataPolicy command) {
        return service().change(authorization, command);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public ReferenceDataPolicyResult<List<ReferenceDataPolicyRevision>> history(
            AuthorizedCompanyOperation authorization,
            ReferenceDataCatalog catalog,
            String code) {
        return service().history(authorization, catalog, code);
    }

    private ReferenceDataPolicyService service() {
        return new ReferenceDataPolicyService(repository, audit, Clock.systemUTC());
    }
}
