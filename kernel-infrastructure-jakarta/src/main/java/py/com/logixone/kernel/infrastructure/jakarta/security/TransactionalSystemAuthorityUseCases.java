package py.com.logixone.kernel.infrastructure.jakarta.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.SecurityOperationResult;
import py.com.logixone.kernel.application.security.admin.SecurityAdministrationActionResult;
import py.com.logixone.kernel.application.security.admin.SystemAuthorityAdministrationQueryService;
import py.com.logixone.kernel.application.security.admin.SystemAuthorityAdministrationSnapshot;
import py.com.logixone.kernel.application.security.audit.SecurityAuditActor;
import py.com.logixone.kernel.application.security.command.ChangeAppUserStatusCommand;
import py.com.logixone.kernel.application.security.port.AppUserIdGenerator;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.system.SystemAuthorityAdministrationService;
import py.com.logixone.kernel.application.security.system.SystemAuthorityBootstrapService;
import py.com.logixone.kernel.application.security.system.SystemAuthorityBootstrapState;
import py.com.logixone.kernel.application.security.system.SystemAuthorityQueryService;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityAccess;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityAccessService;
import py.com.logixone.kernel.application.security.system.command.AssignSystemRoleCommand;
import py.com.logixone.kernel.application.security.system.command.BootstrapSystemAuthorityCommand;
import py.com.logixone.kernel.application.security.system.command.ChangeSystemRoleStatusCommand;
import py.com.logixone.kernel.application.security.system.command.GrantSystemPermissionCommand;
import py.com.logixone.kernel.application.security.system.command.RegisterSystemRoleCommand;
import py.com.logixone.kernel.application.security.system.command.RevokeSystemPermissionCommand;
import py.com.logixone.kernel.application.security.system.command.UnassignSystemRoleCommand;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityAuditPort;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityAdministrationPort;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityAccessAuditPort;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityAccessPort;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityBootstrapPort;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityRepository;
import py.com.logixone.kernel.application.security.system.port.SystemRoleIdGenerator;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.system.AppUserSystemRoleAssignment;
import py.com.logixone.kernel.domain.security.system.EffectiveSystemPermissionResolution;
import py.com.logixone.kernel.domain.security.system.SystemRole;
import py.com.logixone.kernel.domain.security.system.SystemRolePermissionGrant;

/** Internal JTA boundary; no anonymous or public endpoint delegates here. */
@ApplicationScoped
public class TransactionalSystemAuthorityUseCases
        implements SystemAuthorityBootstrapPort, SystemAuthorityAccessPort,
        SystemAuthorityAdministrationPort {

    @Inject
    AppUserRepository userRepository;

    @Inject
    SystemAuthorityRepository authorityRepository;

    @Inject
    AppUserIdGenerator userIdGenerator;

    @Inject
    SystemRoleIdGenerator roleIdGenerator;

    @Inject
    SystemAuthorityAuditPort auditPort;

    @Inject
    SystemAuthorityAccessAuditPort accessAuditPort;

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<SystemAuthorityBootstrapState> bootstrap(
            BootstrapSystemAuthorityCommand command) {
        return new SystemAuthorityBootstrapService(
                userRepository,
                authorityRepository,
                userIdGenerator,
                roleIdGenerator,
                auditPort,
                Clock.systemUTC())
                .bootstrap(command);
    }

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<SystemRole> registerRole(
            RegisterSystemRoleCommand command, SecurityAuditActor actor) {
        return administration(actor).registerRole(command);
    }

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<AppUser> changeUserStatus(
            ChangeAppUserStatusCommand command, SecurityAuditActor actor) {
        return administration(actor).changeUserStatus(command);
    }

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<SystemRole> changeRoleStatus(
            ChangeSystemRoleStatusCommand command, SecurityAuditActor actor) {
        return administration(actor).changeRoleStatus(command);
    }

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<AppUserSystemRoleAssignment> assignRole(
            AssignSystemRoleCommand command, SecurityAuditActor actor) {
        return administration(actor).assignRole(command);
    }

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<AppUserSystemRoleAssignment> unassignRole(
            UnassignSystemRoleCommand command, SecurityAuditActor actor) {
        return administration(actor).unassignRole(command);
    }

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<SystemRolePermissionGrant> grantPermission(
            GrantSystemPermissionCommand command, SecurityAuditActor actor) {
        return administration(actor).grantPermission(command);
    }

    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityOperationResult<SystemRolePermissionGrant> revokePermission(
            RevokeSystemPermissionCommand command, SecurityAuditActor actor) {
        return administration(actor).revokePermission(command);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public SystemAuthorityAdministrationSnapshot authoritySnapshot() {
        return new SystemAuthorityAdministrationQueryService(userRepository, authorityRepository)
                .snapshot();
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult registerSystemRole(
            RegisterSystemRoleCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(registerRole(command, actor));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult changeSystemRoleStatus(
            ChangeSystemRoleStatusCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(changeRoleStatus(command, actor));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult assignSystemRole(
            AssignSystemRoleCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(assignRole(command, actor));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult unassignSystemRole(
            UnassignSystemRoleCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(unassignRole(command, actor));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult grantSystemPermission(
            GrantSystemPermissionCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(grantPermission(command, actor));
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SecurityAdministrationActionResult revokeSystemPermission(
            RevokeSystemPermissionCommand command, SecurityAuditActor actor) {
        return SecurityAdministrationActionResult.from(revokePermission(command, actor));
    }

    @Transactional(TxType.SUPPORTS)
    public Optional<EffectiveSystemPermissionResolution> resolvePermissions(AppUserId userId) {
        return new SystemAuthorityQueryService(userRepository, authorityRepository)
                .resolvePermissions(userId);
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SystemAuthorityAccess authorizeAny(
            ExternalIdentity externalIdentity,
            String correlationId) {
        return access().authorizeAny(externalIdentity, correlationId);
    }

    @Override
    @Transactional(rollbackOn = RuntimeException.class)
    public SystemAuthorityAccess authorize(
            ExternalIdentity externalIdentity,
            SystemPermission requiredPermission,
            String correlationId) {
        return access().authorize(externalIdentity, requiredPermission, correlationId);
    }

    private SystemAuthorityAdministrationService administration(SecurityAuditActor actor) {
        return new SystemAuthorityAdministrationService(
                userRepository,
                authorityRepository,
                roleIdGenerator,
                auditPort,
                Clock.systemUTC(),
                Objects.requireNonNull(actor, "actor"));
    }

    private SystemAuthorityAccessService access() {
        return new SystemAuthorityAccessService(
                userRepository,
                authorityRepository,
                accessAuditPort,
                Clock.systemUTC());
    }
}
