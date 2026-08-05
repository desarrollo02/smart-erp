package py.com.logixone.kernel.application.security.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.application.security.SecurityOperationCode;
import py.com.logixone.kernel.domain.security.RoleId;
import py.com.logixone.plugin.api.ContributionId;

/** Audit event restricted to stable technical identifiers and operation codes. */
public record SecurityAuditEvent(
        SecurityAuditOperation operation,
        SecurityAuditOutcome outcome,
        Optional<AppUserId> subjectUserId,
        Optional<CompanyId> companyId,
        Optional<RoleId> roleId,
        Optional<ContributionId> permissionId,
        Optional<SecurityOperationCode> code,
        Optional<Long> previousVersion,
        Optional<Long> resultingVersion,
        Instant occurredAt,
        SecurityAuditActor actor) {

    public SecurityAuditEvent {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(outcome, "outcome");
        subjectUserId = Objects.requireNonNull(subjectUserId, "subjectUserId");
        companyId = Objects.requireNonNull(companyId, "companyId");
        roleId = Objects.requireNonNull(roleId, "roleId");
        permissionId = Objects.requireNonNull(permissionId, "permissionId");
        code = Objects.requireNonNull(code, "code");
        previousVersion = validVersion(previousVersion, "previousVersion");
        resultingVersion = validVersion(resultingVersion, "resultingVersion");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(actor, "actor");
        if ((outcome == SecurityAuditOutcome.REJECTED) != code.isPresent()) {
            throw new IllegalArgumentException("rejected events require exactly one stable code");
        }
    }

    private static Optional<Long> validVersion(Optional<Long> version, String name) {
        Objects.requireNonNull(version, name);
        version.ifPresent(value -> {
            if (value < 0) {
                throw new IllegalArgumentException(name + " must not be negative");
            }
        });
        return version;
    }
}
