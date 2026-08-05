package py.com.logixone.kernel.application.audit.admin;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;

/** Immutable technical projection safe for an authorized administrative adapter. */
public record AuditEventView(
        UUID auditEventId,
        AuditEventCategory category,
        String operation,
        AuditEventOutcome outcome,
        String actorKind,
        Optional<AppUserId> actorUserId,
        Optional<AppUserId> subjectUserId,
        Optional<CompanyId> companyId,
        Optional<String> roleId,
        Optional<String> systemRoleId,
        Optional<String> pluginId,
        Optional<String> permissionId,
        Optional<String> screenId,
        Optional<String> resourceType,
        Optional<String> resourceId,
        Optional<String> code,
        Optional<Long> previousVersion,
        Optional<Long> resultingVersion,
        Optional<String> correlationId,
        Instant occurredAt) {

    public AuditEventView {
        Objects.requireNonNull(auditEventId, "auditEventId");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(actorKind, "actorKind");
        actorUserId = Objects.requireNonNull(actorUserId, "actorUserId");
        subjectUserId = Objects.requireNonNull(subjectUserId, "subjectUserId");
        companyId = Objects.requireNonNull(companyId, "companyId");
        roleId = Objects.requireNonNull(roleId, "roleId");
        systemRoleId = Objects.requireNonNull(systemRoleId, "systemRoleId");
        pluginId = Objects.requireNonNull(pluginId, "pluginId");
        permissionId = Objects.requireNonNull(permissionId, "permissionId");
        screenId = Objects.requireNonNull(screenId, "screenId");
        resourceType = Objects.requireNonNull(resourceType, "resourceType");
        resourceId = Objects.requireNonNull(resourceId, "resourceId");
        code = Objects.requireNonNull(code, "code");
        previousVersion = Objects.requireNonNull(previousVersion, "previousVersion");
        resultingVersion = Objects.requireNonNull(resultingVersion, "resultingVersion");
        correlationId = Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (operation.isBlank() || actorKind.isBlank()) {
            throw new IllegalArgumentException("operation and actorKind must not be blank");
        }
    }
}
