package py.com.logixone.kernel.infrastructure.jakarta.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAccessAuditEvent;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityAccessAuditPort;
import py.com.logixone.kernel.infrastructure.jakarta.persistence.JpaTechnicalAuditStore;

/** Logs administrative access decisions without external identity or claims. */
@ApplicationScoped
public class StructuredSystemAuthorityAccessAudit implements SystemAuthorityAccessAuditPort {

    private static final Logger LOGGER =
            System.getLogger(StructuredSystemAuthorityAccessAudit.class.getName());

    @Inject
    JpaTechnicalAuditStore auditStore;

    @Override
    public void record(SystemAuthorityAccessAuditEvent event) {
        Objects.requireNonNull(event, "event");
        auditStore.record(event);
        LOGGER.log(Level.INFO, () -> String.join(" ",
                "event=system_authority_access",
                "outcome=" + event.outcome(),
                "actor_user_id=" + event.actorUserId().map(Object::toString).orElse("-"),
                "required_permission="
                        + event.requiredPermission().map(Object::toString).orElse("ANY"),
                "code=" + event.code().map(Enum::name).orElse("-"),
                "correlation_id=" + event.correlationId(),
                "occurred_at=" + event.occurredAt()));
    }
}
