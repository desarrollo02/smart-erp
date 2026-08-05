package py.com.logixone.kernel.application.security;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.application.security.audit.SecurityAuditActor;
import py.com.logixone.kernel.application.security.audit.SecurityAuditEvent;
import py.com.logixone.kernel.application.security.audit.SecurityAuditOperation;
import py.com.logixone.kernel.application.security.audit.SecurityAuditOutcome;
import py.com.logixone.kernel.application.security.port.SecurityAuditPort;
import py.com.logixone.kernel.domain.security.RoleId;
import py.com.logixone.plugin.api.ContributionId;

final class SecurityAuditRecorder {

    private final SecurityAuditPort port;
    private final Clock clock;
    private final SecurityAuditActor actor;

    SecurityAuditRecorder(SecurityAuditPort port, Clock clock, SecurityAuditActor actor) {
        this.port = Objects.requireNonNull(port, "port");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.actor = Objects.requireNonNull(actor, "actor");
    }

    void record(
            SecurityAuditOperation operation,
            SecurityAuditOutcome outcome,
            AppUserId subjectUserId,
            CompanyId companyId,
            RoleId roleId,
            ContributionId permissionId,
            SecurityOperationCode code,
            Long previousVersion,
            Long resultingVersion) {
        port.record(new SecurityAuditEvent(
                operation,
                outcome,
                Optional.ofNullable(subjectUserId),
                Optional.ofNullable(companyId),
                Optional.ofNullable(roleId),
                Optional.ofNullable(permissionId),
                Optional.ofNullable(code),
                Optional.ofNullable(previousVersion),
                Optional.ofNullable(resultingVersion),
                clock.instant(),
                actor));
    }
}
