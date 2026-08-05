package py.com.logixone.kernel.application.security.system.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.SecurityOperationCode;
import py.com.logixone.kernel.application.security.audit.SecurityAuditActor;
import py.com.logixone.kernel.application.security.audit.SecurityAuditOutcome;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;

/** Audit event containing only technical IDs for kernel-wide authority. */
public record SystemAuthorityAuditEvent(
        SystemAuthorityAuditOperation operation,
        SecurityAuditOutcome outcome,
        Optional<AppUserId> subjectUserId,
        Optional<SystemRoleId> systemRoleId,
        Optional<SystemPermission> permission,
        Optional<SecurityOperationCode> code,
        Optional<Long> previousVersion,
        Optional<Long> resultingVersion,
        Instant occurredAt,
        SecurityAuditActor actor) {

    public SystemAuthorityAuditEvent {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(outcome, "outcome");
        subjectUserId = Objects.requireNonNull(subjectUserId, "subjectUserId");
        systemRoleId = Objects.requireNonNull(systemRoleId, "systemRoleId");
        permission = Objects.requireNonNull(permission, "permission");
        code = Objects.requireNonNull(code, "code");
        previousVersion = Objects.requireNonNull(previousVersion, "previousVersion");
        resultingVersion = Objects.requireNonNull(resultingVersion, "resultingVersion");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(actor, "actor");
        if ((outcome == SecurityAuditOutcome.REJECTED) != code.isPresent()) {
            throw new IllegalArgumentException(
                    "rejected system-authority events require exactly one code");
        }
    }
}
