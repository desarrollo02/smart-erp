package py.com.logixone.kernel.application.security.system.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityAccessCode;

/** Audit of administrative access using only local technical identifiers. */
public record SystemAuthorityAccessAuditEvent(
        SystemAuthorityAccessAuditOutcome outcome,
        Optional<AppUserId> actorUserId,
        Optional<SystemPermission> requiredPermission,
        Optional<SystemAuthorityAccessCode> code,
        String correlationId,
        Instant occurredAt) {

    private static final Pattern VALID_CORRELATION =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    public SystemAuthorityAccessAuditEvent {
        Objects.requireNonNull(outcome, "outcome");
        actorUserId = Objects.requireNonNull(actorUserId, "actorUserId");
        requiredPermission = Objects.requireNonNull(requiredPermission, "requiredPermission");
        code = Objects.requireNonNull(code, "code");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (!VALID_CORRELATION.matcher(correlationId).matches()) {
            throw new IllegalArgumentException("correlationId has an invalid format");
        }
        if ((outcome == SystemAuthorityAccessAuditOutcome.ALLOWED) == code.isPresent()) {
            throw new IllegalArgumentException("allowed access has no code; denied access requires one");
        }
    }
}
