package py.com.logixone.plugins.businesspartners.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.time.Clock;
import java.util.List;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerDefinitionService;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerDefinitionUseCases;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerOperationContext;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerOperationResult;
import py.com.logixone.plugins.businesspartners.application.command.ChangeBusinessPartnerDefinitionState;
import py.com.logixone.plugins.businesspartners.application.command.RegisterBusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.application.command.ReviseBusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerDefinitionRepository;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionRevision;

/** JTA boundary for company-owned definitions. */
@ApplicationScoped
@Transactional(rollbackOn = RuntimeException.class)
public class TransactionalBusinessPartnerDefinitionUseCases
        implements BusinessPartnerDefinitionUseCases {

    @Inject
    BusinessPartnerDefinitionRepository repository;

    @Inject
    TechnicalAudit audit;

    @Override
    @Transactional(TxType.SUPPORTS)
    public BusinessPartnerOperationResult<List<BusinessPartnerDefinition>> definitions(
            BusinessPartnerOperationContext context, BusinessPartnerDefinitionKind kind) {
        return service().definitions(context, kind);
    }

    @Override
    public BusinessPartnerOperationResult<BusinessPartnerDefinition> registerDefinition(
            BusinessPartnerOperationContext context, RegisterBusinessPartnerDefinition command) {
        return service().registerDefinition(context, command);
    }

    @Override
    public BusinessPartnerOperationResult<BusinessPartnerDefinition> reviseDefinition(
            BusinessPartnerOperationContext context, ReviseBusinessPartnerDefinition command) {
        return service().reviseDefinition(context, command);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public BusinessPartnerOperationResult<List<BusinessPartnerDefinitionRevision>> definitionHistory(
            BusinessPartnerOperationContext context,
            BusinessPartnerDefinitionKind kind,
            py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode code) {
        return service().definitionHistory(context, kind, code);
    }

    @Override
    public BusinessPartnerOperationResult<BusinessPartnerDefinition> changeDefinitionState(
            BusinessPartnerOperationContext context,
            ChangeBusinessPartnerDefinitionState command) {
        return service().changeDefinitionState(context, command);
    }

    private BusinessPartnerDefinitionService service() {
        return new BusinessPartnerDefinitionService(repository, audit, Clock.systemUTC());
    }
}
