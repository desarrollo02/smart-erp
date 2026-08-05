package py.com.logixone.kernel.application.security.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.application.security.access.TrustedAccessCode;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.ScreenId;

/** Access audit restricted to stable technical identifiers and a server correlation. */
public record AccessAuditEvent(
        AccessAuditOperation operation,
        AccessAuditOutcome outcome,
        Optional<AppUserId> actorUserId,
        Optional<CompanyId> companyId,
        Optional<PluginId> pluginId,
        Optional<ContributionId> permissionId,
        Optional<ScreenId> screenId,
        Optional<TrustedAccessCode> code,
        String correlationId,
        Instant occurredAt) {

    private static final Pattern VALID_CORRELATION =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    public AccessAuditEvent {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(outcome, "outcome");
        actorUserId = Objects.requireNonNull(actorUserId, "actorUserId");
        companyId = Objects.requireNonNull(companyId, "companyId");
        pluginId = Objects.requireNonNull(pluginId, "pluginId");
        permissionId = Objects.requireNonNull(permissionId, "permissionId");
        screenId = Objects.requireNonNull(screenId, "screenId");
        code = Objects.requireNonNull(code, "code");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (!VALID_CORRELATION.matcher(correlationId).matches()) {
            throw new IllegalArgumentException("correlationId has an invalid format");
        }
        if ((outcome == AccessAuditOutcome.ALLOWED) == code.isPresent()) {
            throw new IllegalArgumentException(
                    "allowed access has no code; denied/selection access requires one");
        }
        if (permissionId.isPresent() && pluginId.isEmpty()) {
            throw new IllegalArgumentException("permission audit requires a plugin id");
        }
        if (screenId.isPresent()
                && (pluginId.isEmpty()
                        || !screenId.orElseThrow().ownerPluginId().equals(pluginId.orElseThrow()))) {
            throw new IllegalArgumentException("screen audit requires its owning plugin id");
        }
    }
}
