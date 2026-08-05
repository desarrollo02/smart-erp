package py.com.logixone.kernel.application.company.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.application.company.CompanyOperationCode;
import py.com.logixone.plugin.api.PluginId;

/** Technical audit event without commercial data, SQL, credentials or user invention. */
public record CompanyAuditEvent(
        CompanyId companyId,
        CompanyAuditOperation operation,
        CompanyAuditOutcome outcome,
        Optional<PluginId> pluginId,
        Optional<CompanyOperationCode> code,
        Optional<Long> previousVersion,
        Optional<Long> resultingVersion,
        Instant occurredAt,
        CompanyAuditActor actor,
        Optional<AppUserId> actorUserId,
        Optional<String> correlationId) {

    public CompanyAuditEvent {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(outcome, "outcome");
        pluginId = Objects.requireNonNull(pluginId, "pluginId");
        code = Objects.requireNonNull(code, "code");
        previousVersion = validVersion(previousVersion, "previousVersion");
        resultingVersion = validVersion(resultingVersion, "resultingVersion");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(actor, "actor");
        actorUserId = Objects.requireNonNull(actorUserId, "actorUserId");
        correlationId = Objects.requireNonNull(correlationId, "correlationId");
        if (actor == CompanyAuditActor.AUTHENTICATED_USER
                && (actorUserId.isEmpty() || correlationId.isEmpty())) {
            throw new IllegalArgumentException(
                    "authenticated audit events require actorUserId and correlationId");
        }
        boolean denied = outcome == CompanyAuditOutcome.REJECTED
                || outcome == CompanyAuditOutcome.DENIED;
        if (denied != code.isPresent()) {
            throw new IllegalArgumentException("rejected or denied events require exactly one code");
        }
    }

    public CompanyAuditEvent(
            CompanyId companyId,
            CompanyAuditOperation operation,
            CompanyAuditOutcome outcome,
            Optional<PluginId> pluginId,
            Optional<CompanyOperationCode> code,
            Optional<Long> previousVersion,
            Optional<Long> resultingVersion,
            Instant occurredAt,
            CompanyAuditActor actor) {
        this(
                companyId,
                operation,
                outcome,
                pluginId,
                code,
                previousVersion,
                resultingVersion,
                occurredAt,
                actor,
                Optional.empty(),
                Optional.empty());
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
