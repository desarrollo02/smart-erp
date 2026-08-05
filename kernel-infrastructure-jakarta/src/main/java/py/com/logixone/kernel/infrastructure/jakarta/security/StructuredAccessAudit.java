package py.com.logixone.kernel.infrastructure.jakarta.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;
import py.com.logixone.kernel.application.security.audit.AccessAuditEvent;
import py.com.logixone.kernel.application.security.port.AccessAuditPort;
import py.com.logixone.kernel.infrastructure.jakarta.persistence.JpaTechnicalAuditStore;

/** Logs only local technical identifiers and a server-generated correlation id. */
@ApplicationScoped
public class StructuredAccessAudit implements AccessAuditPort {

    private static final Logger LOGGER = System.getLogger(StructuredAccessAudit.class.getName());

    @Inject
    JpaTechnicalAuditStore auditStore;

    @Override
    public void record(AccessAuditEvent event) {
        Objects.requireNonNull(event, "event");
        auditStore.record(event);
        LOGGER.log(Level.INFO, () -> String.join(" ",
                "event=trusted_access",
                "operation=" + event.operation(),
                "outcome=" + event.outcome(),
                "actor_user_id=" + event.actorUserId().map(Object::toString).orElse("-"),
                "company_id=" + event.companyId().map(Object::toString).orElse("-"),
                "plugin_id=" + event.pluginId().map(Object::toString).orElse("-"),
                "permission_id=" + event.permissionId().map(Object::toString).orElse("-"),
                "screen_id=" + event.screenId().map(Object::toString).orElse("-"),
                "code=" + event.code().map(Enum::name).orElse("-"),
                "correlation_id=" + event.correlationId(),
                "occurred_at=" + event.occurredAt()));
    }
}
