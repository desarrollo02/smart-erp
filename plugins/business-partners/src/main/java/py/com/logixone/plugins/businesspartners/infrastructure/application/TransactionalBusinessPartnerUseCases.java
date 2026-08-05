package py.com.logixone.plugins.businesspartners.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.time.Clock;
import java.util.List;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerCommandService;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerOperationContext;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerOperationResult;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerUseCases;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerQueryService;
import py.com.logixone.plugins.businesspartners.application.command.BusinessPartnerCommands;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerCodeSequenceRepository;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerDefinitionRepository;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerIdGenerator;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerRepository;
import py.com.logixone.plugins.businesspartners.application.port.CountryReferencePolicy;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchCriteria;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchPage;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerIdentificationKey;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerSnapshot;

/** JTA boundary; there is intentionally no REST or JSF exposure in this story. */
@ApplicationScoped
@Transactional(rollbackOn = RuntimeException.class)
public class TransactionalBusinessPartnerUseCases implements BusinessPartnerUseCases {

    @Inject
    BusinessPartnerRepository repository;

    @Inject
    BusinessPartnerDefinitionRepository definitions;

    @Inject
    BusinessPartnerCodeSequenceRepository sequences;

    @Inject
    BusinessPartnerIdGenerator idGenerator;

    @Inject
    CountryReferencePolicy countries;

    @Inject
    TechnicalAudit audit;

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> register(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.Register command) {
        return commands().register(context, command);
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> rename(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.Rename command) {
        return commands().rename(context, command);
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> changeCode(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.ChangeCode command) {
        return commands().changeCode(context, command);
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> addIdentification(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.AddIdentification command) {
        return commands().addIdentification(context, command);
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> addAddress(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.AddAddress command) {
        return commands().addAddress(context, command);
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> addChannel(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.AddChannel command) {
        return commands().addChannel(context, command);
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> addContact(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.AddContact command) {
        return commands().addContact(context, command);
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> assignRole(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.AssignRole command) {
        return commands().assignRole(context, command);
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> changeRoleState(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.ChangeRoleState command) {
        return commands().changeRoleState(context, command);
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> changeLifecycle(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.ChangeLifecycle command) {
        return commands().changeLifecycle(context, command);
    }

    @Transactional(TxType.SUPPORTS)
    public BusinessPartnerOperationResult<BusinessPartnerSearchPage> search(
            BusinessPartnerOperationContext context,
            BusinessPartnerSearchCriteria criteria) {
        return queries().search(context, criteria);
    }

    @Transactional(TxType.SUPPORTS)
    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> detail(
            BusinessPartnerOperationContext context,
            BusinessPartnerId id) {
        return queries().detail(context, id);
    }

    @Transactional(TxType.SUPPORTS)
    public BusinessPartnerOperationResult<List<BusinessPartnerId>> duplicateCandidates(
            BusinessPartnerOperationContext context,
            BusinessPartnerIdentificationKey candidate) {
        return queries().duplicateCandidates(context, candidate);
    }

    private BusinessPartnerCommandService commands() {
        return new BusinessPartnerCommandService(
                repository, definitions, sequences, idGenerator, countries, audit, Clock.systemUTC());
    }

    private BusinessPartnerQueryService queries() {
        return new BusinessPartnerQueryService(repository);
    }
}
