package py.com.logixone.kernel.infrastructure.jakarta.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.time.Clock;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.CompanyPluginQueryService;
import py.com.logixone.kernel.application.company.contribution.CompanyContributionService;
import py.com.logixone.kernel.application.company.port.PluginActivationRepository;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.security.SecurityAdministrationService;
import py.com.logixone.kernel.application.security.SecurityBootstrapService;
import py.com.logixone.kernel.application.security.SecurityBootstrapState;
import py.com.logixone.kernel.application.security.SecurityOperationResult;
import py.com.logixone.kernel.application.security.admin.BusinessSecurityAdministrationQueryService;
import py.com.logixone.kernel.application.security.admin.BusinessSecuritySnapshot;
import py.com.logixone.kernel.application.security.admin.CompanySecurityAdministrationView;
import py.com.logixone.kernel.application.security.admin.SecurityAdministrationActionResult;
import py.com.logixone.kernel.application.security.audit.SecurityAuditActor;
import py.com.logixone.kernel.application.security.command.AssignRoleCommand;
import py.com.logixone.kernel.application.security.command.BootstrapSecurityCommand;
import py.com.logixone.kernel.application.security.command.ChangeAppUserStatusCommand;
import py.com.logixone.kernel.application.security.command.ChangeMembershipStatusCommand;
import py.com.logixone.kernel.application.security.command.ChangeRoleStatusCommand;
import py.com.logixone.kernel.application.security.command.GrantPermissionCommand;
import py.com.logixone.kernel.application.security.command.RegisterAppUserCommand;
import py.com.logixone.kernel.application.security.command.RegisterMembershipCommand;
import py.com.logixone.kernel.application.security.command.RegisterRoleCommand;
import py.com.logixone.kernel.application.security.command.RevokePermissionCommand;
import py.com.logixone.kernel.application.security.command.UnassignRoleCommand;
import py.com.logixone.kernel.application.security.port.AppUserIdGenerator;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.port.CompanyAuthorizationRepository;
import py.com.logixone.kernel.application.security.port.CompanyMembershipRepository;
import py.com.logixone.kernel.application.security.port.RoleIdGenerator;
import py.com.logixone.kernel.application.security.port.SecurityAuditPort;
import py.com.logixone.kernel.application.security.port.SecurityBootstrapPort;
import py.com.logixone.kernel.application.security.port.BusinessSecurityAdministrationPort;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.CompanyMembership;
import py.com.logixone.kernel.domain.security.CompanyRole;
import py.com.logixone.kernel.domain.security.MembershipRoleAssignment;
import py.com.logixone.kernel.domain.security.RolePermissionGrant;
import py.com.logixone.kernel.domain.company.CompanyPluginResolver;
import py.com.logixone.kernel.infrastructure.jakarta.plugin.CdiPluginCatalog;

/** Internal JTA boundary. No REST, Faces or anonymous bootstrap endpoint delegates here. */
@ApplicationScoped
public class TransactionalSecurityUseCases
        implements SecurityBootstrapPort, BusinessSecurityAdministrationPort {

    @Inject
    AppUserRepository userRepository;

    @Inject
    CompanyMembershipRepository membershipRepository;

    @Inject
    CompanyAuthorizationRepository authorizationRepository;

    @Inject
    CompanyRepository companyRepository;

    @Inject
    PluginActivationRepository activationRepository;

    @Inject
    CdiPluginCatalog pluginCatalog;

    @Inject
    AppUserIdGenerator userIdGenerator;

    @Inject
    RoleIdGenerator roleIdGenerator;

    @Inject
    SecurityAuditPort auditPort;

    @Inject
    TransactionalSystemAuthorityUseCases systemAuthorityUseCases;

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<AppUser> registerUser(RegisterAppUserCommand command) {
        return administration().registerUser(command);
    }

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<AppUser> changeUserStatus(ChangeAppUserStatusCommand command) {
        return systemAuthorityUseCases.changeUserStatus(command, SecurityAuditActor.SYSTEM);
    }

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<CompanyMembership> registerMembership(
            RegisterMembershipCommand command) {
        return administration().registerMembership(command);
    }

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<CompanyMembership> changeMembershipStatus(
            ChangeMembershipStatusCommand command) {
        return administration().changeMembershipStatus(command);
    }

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<CompanyRole> registerRole(RegisterRoleCommand command) {
        return administration().registerRole(command);
    }

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<CompanyRole> changeRoleStatus(ChangeRoleStatusCommand command) {
        return administration().changeRoleStatus(command);
    }

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<MembershipRoleAssignment> assignRole(AssignRoleCommand command) {
        return administration().assignRole(command);
    }

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<RolePermissionGrant> grantPermission(
            GrantPermissionCommand command) {
        return administration().grantPermission(command);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public BusinessSecuritySnapshot administrationSnapshot() {
        return queries().snapshot();
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public Optional<CompanySecurityAdministrationView> findCompanySecurity(CompanyId companyId) {
        return queries().findCompany(companyId);
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult registerUser(
            RegisterAppUserCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(administration(actor).registerUser(command));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult changeUserStatus(
            ChangeAppUserStatusCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(
                systemAuthorityUseCases.changeUserStatus(command, actor));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult registerMembership(
            RegisterMembershipCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(
                administration(actor).registerMembership(command));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult changeMembershipStatus(
            ChangeMembershipStatusCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(
                administration(actor).changeMembershipStatus(command));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult registerCompanyRole(
            RegisterRoleCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(administration(actor).registerRole(command));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult changeCompanyRoleStatus(
            ChangeRoleStatusCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(
                administration(actor).changeRoleStatus(command));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult assignCompanyRole(
            AssignRoleCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(administration(actor).assignRole(command));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult unassignCompanyRole(
            UnassignRoleCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(administration(actor).unassignRole(command));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult grantCompanyPermission(
            GrantPermissionCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(administration(actor).grantPermission(
                command, contributionService().compose(command.companyId()).permissions()));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult revokeCompanyPermission(
            RevokePermissionCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(
                administration(actor).revokePermission(command));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<SecurityBootstrapState> bootstrap(
            BootstrapSecurityCommand command) {
        return new SecurityBootstrapService(
                companyRepository,
                userRepository,
                membershipRepository,
                authorizationRepository,
                userIdGenerator,
                roleIdGenerator,
                auditPort,
                Clock.systemUTC())
                .bootstrap(command);
    }

    private SecurityAdministrationService administration() {
        return administration(SecurityAuditActor.SYSTEM);
    }

    private SecurityAdministrationService administration(SecurityAuditActor actor) {
        return new SecurityAdministrationService(
                userRepository,
                membershipRepository,
                authorizationRepository,
                companyRepository,
                userIdGenerator,
                roleIdGenerator,
                auditPort,
                Clock.systemUTC(),
                actor);
    }

    private BusinessSecurityAdministrationQueryService queries() {
        return new BusinessSecurityAdministrationQueryService(
                userRepository,
                membershipRepository,
                authorizationRepository,
                companyRepository,
                contributionService());
    }

    private CompanyContributionService contributionService() {
        return new CompanyContributionService(new CompanyPluginQueryService(
                companyRepository,
                activationRepository,
                pluginCatalog.registry(),
                new CompanyPluginResolver()));
    }
}
