package py.com.logixone.kernel.application.security.system.access;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.system.SystemAuthorityQueryService;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAccessAuditEvent;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAccessAuditOutcome;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityAccessAuditPort;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityRepository;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.system.EffectiveSystemPermissionResolution;
import py.com.logixone.kernel.domain.security.system.SystemSecurityDiagnosticCode;

/** Resolves global authority from current local state and audits every decision. */
public final class SystemAuthorityAccessService {

    private final AppUserRepository userRepository;
    private final SystemAuthorityRepository authorityRepository;
    private final SystemAuthorityAccessAuditPort auditPort;
    private final Clock clock;

    public SystemAuthorityAccessService(
            AppUserRepository userRepository,
            SystemAuthorityRepository authorityRepository,
            SystemAuthorityAccessAuditPort auditPort,
            Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.authorityRepository = Objects.requireNonNull(authorityRepository, "authorityRepository");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SystemAuthorityAccess authorizeAny(
            ExternalIdentity externalIdentity,
            String correlationId) {
        return authorize(externalIdentity, Optional.empty(), correlationId);
    }

    public SystemAuthorityAccess authorize(
            ExternalIdentity externalIdentity,
            SystemPermission requiredPermission,
            String correlationId) {
        return authorize(
                externalIdentity,
                Optional.of(Objects.requireNonNull(requiredPermission, "requiredPermission")),
                correlationId);
    }

    private SystemAuthorityAccess authorize(
            ExternalIdentity externalIdentity,
            Optional<SystemPermission> requiredPermission,
            String correlationId) {
        Objects.requireNonNull(externalIdentity, "externalIdentity");
        requiredPermission = Objects.requireNonNull(requiredPermission, "requiredPermission");

        if (requiredPermission.isPresent()
                && !SystemPermission.knownPermissions().contains(requiredPermission.orElseThrow())) {
            return denied(null, requiredPermission, SystemAuthorityAccessCode.PERMISSION_UNKNOWN,
                    correlationId);
        }

        AppUser user = userRepository.findByExternalIdentity(externalIdentity).orElse(null);
        if (user == null) {
            return denied(null, requiredPermission, SystemAuthorityAccessCode.IDENTITY_NOT_FOUND,
                    correlationId);
        }
        if (!user.isActive()) {
            return denied(user, requiredPermission, SystemAuthorityAccessCode.USER_INACTIVE,
                    correlationId);
        }

        EffectiveSystemPermissionResolution resolution =
                new SystemAuthorityQueryService(userRepository, authorityRepository)
                        .resolvePermissions(user.id())
                        .orElse(null);
        if (resolution == null) {
            return denied(user, requiredPermission, SystemAuthorityAccessCode.CONTEXT_INVALID,
                    correlationId);
        }
        if (!resolution.authorized()) {
            SystemAuthorityAccessCode code = resolution.failure().orElseThrow()
                    == SystemSecurityDiagnosticCode.USER_INACTIVE
                    ? SystemAuthorityAccessCode.USER_INACTIVE
                    : SystemAuthorityAccessCode.CONTEXT_INVALID;
            return denied(user, requiredPermission, code, correlationId);
        }

        Set<SystemPermission> permissions = resolution.permissions();
        boolean allowed = requiredPermission
                .map(permissions::contains)
                .orElseGet(() -> !permissions.isEmpty());
        if (!allowed) {
            return denied(user, requiredPermission, SystemAuthorityAccessCode.PERMISSION_DENIED,
                    correlationId);
        }

        SystemAuthorityContext context = new SystemAuthorityContext(user.id(), permissions);
        auditPort.record(new SystemAuthorityAccessAuditEvent(
                SystemAuthorityAccessAuditOutcome.ALLOWED,
                Optional.of(user.id()),
                requiredPermission,
                Optional.empty(),
                correlationId,
                clock.instant()));
        return SystemAuthorityAccess.allowed(context);
    }

    private SystemAuthorityAccess denied(
            AppUser user,
            Optional<SystemPermission> requiredPermission,
            SystemAuthorityAccessCode code,
            String correlationId) {
        auditPort.record(new SystemAuthorityAccessAuditEvent(
                SystemAuthorityAccessAuditOutcome.DENIED,
                Optional.ofNullable(user).map(AppUser::id),
                requiredPermission,
                Optional.of(code),
                correlationId,
                clock.instant()));
        return SystemAuthorityAccess.denied();
    }
}
