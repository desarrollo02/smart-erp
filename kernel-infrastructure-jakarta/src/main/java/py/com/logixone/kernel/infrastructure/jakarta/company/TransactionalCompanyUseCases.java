package py.com.logixone.kernel.infrastructure.jakarta.company;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.time.Clock;
import java.util.Optional;
import py.com.logixone.kernel.application.company.CompanyAdministrationService;
import py.com.logixone.kernel.application.company.CompanyOperationResult;
import py.com.logixone.kernel.application.company.PluginActivationService;
import py.com.logixone.kernel.application.company.admin.CompanyAdministrationActionResult;
import py.com.logixone.kernel.application.company.admin.CompanyAdministrationQueryService;
import py.com.logixone.kernel.application.company.admin.CompanyAdministrationSnapshot;
import py.com.logixone.kernel.application.company.admin.CompanyPluginAdministrationView;
import py.com.logixone.kernel.application.company.audit.CompanyAuditContext;
import py.com.logixone.kernel.application.company.command.ChangeCompanyStatusCommand;
import py.com.logixone.kernel.application.company.command.ChangePluginActivationCommand;
import py.com.logixone.kernel.application.company.command.RegisterCompanyCommand;
import py.com.logixone.kernel.application.company.command.ReplaceCustomizationCommand;
import py.com.logixone.kernel.application.company.port.CompanyAuditPort;
import py.com.logixone.kernel.application.company.port.CompanyAdministrationPort;
import py.com.logixone.kernel.application.company.port.CompanyIdGenerator;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.company.port.PluginActivationRepository;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyPluginResolver;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.kernel.domain.company.PluginActivationPolicy;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.infrastructure.jakarta.plugin.CdiPluginCatalog;

/** Authorized-adapter boundary for neutral use cases; intentionally has no REST exposure. */
@ApplicationScoped
public class TransactionalCompanyUseCases implements CompanyAdministrationPort {

    @Inject
    CompanyRepository companyRepository;

    @Inject
    PluginActivationRepository activationRepository;

    @Inject
    CompanyIdGenerator idGenerator;

    @Inject
    CompanyAuditPort auditPort;

    @Inject
    CdiPluginCatalog pluginCatalog;

    @Transactional
    public CompanyOperationResult<Company> register(RegisterCompanyCommand command) {
        return administration().register(command);
    }

    @Transactional
    public CompanyOperationResult<Company> changeStatus(ChangeCompanyStatusCommand command) {
        return administration().changeStatus(command);
    }

    @Transactional
    public CompanyOperationResult<Company> replaceCustomization(ReplaceCustomizationCommand command) {
        return administration().replaceCustomization(command);
    }

    @Transactional
    public CompanyOperationResult<PluginActivationDecision> changeActivation(
            ChangePluginActivationCommand command) {
        return activation().change(command);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public CompanyAdministrationSnapshot snapshot() {
        return queries().snapshot();
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public Optional<CompanyPluginAdministrationView> findCompany(CompanyId companyId) {
        return queries().findCompany(companyId);
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public CompanyAdministrationActionResult register(
            RegisterCompanyCommand command,
            CompanyAuditContext auditContext) {
        return CompanyAdministrationActionResult.from(administration(auditContext).register(command));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public CompanyAdministrationActionResult changeStatus(
            ChangeCompanyStatusCommand command,
            CompanyAuditContext auditContext) {
        return CompanyAdministrationActionResult.from(administration(auditContext).changeStatus(command));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public CompanyAdministrationActionResult replaceCustomization(
            ReplaceCustomizationCommand command,
            CompanyAuditContext auditContext) {
        return CompanyAdministrationActionResult.from(
                administration(auditContext).replaceCustomization(command));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public CompanyAdministrationActionResult changeActivation(
            ChangePluginActivationCommand command,
            CompanyAuditContext auditContext) {
        return CompanyAdministrationActionResult.from(activation(auditContext).change(command));
    }

    private CompanyAdministrationService administration() {
        return administration(CompanyAuditContext.SYSTEM);
    }

    private CompanyAdministrationService administration(CompanyAuditContext auditContext) {
        return new CompanyAdministrationService(
                companyRepository,
                activationRepository,
                idGenerator,
                pluginCatalog.registry(),
                new CompanyPluginResolver(),
                auditPort,
                Clock.systemUTC(),
                auditContext);
    }

    private PluginActivationService activation() {
        return activation(CompanyAuditContext.SYSTEM);
    }

    private PluginActivationService activation(CompanyAuditContext auditContext) {
        return new PluginActivationService(
                companyRepository,
                activationRepository,
                pluginCatalog.registry(),
                new PluginActivationPolicy(),
                auditPort,
                Clock.systemUTC(),
                auditContext);
    }

    private CompanyAdministrationQueryService queries() {
        return new CompanyAdministrationQueryService(
                companyRepository,
                activationRepository,
                pluginCatalog.registry(),
                new CompanyPluginResolver());
    }
}
