package py.com.logixone.kernel.infrastructure.jakarta.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAuditEvent;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityAuditPort;
import py.com.logixone.kernel.infrastructure.jakarta.persistence.JpaTechnicalAuditStore;

/** Emits only local technical IDs for kernel-wide authority operations. */
@ApplicationScoped
public class StructuredSystemAuthorityAudit implements SystemAuthorityAuditPort {

    private static final Logger LOGGER =
            System.getLogger(StructuredSystemAuthorityAudit.class.getName());

    @Inject
    JpaTechnicalAuditStore auditStore;

    @Override
    public void record(SystemAuthorityAuditEvent event) {
        Objects.requireNonNull(event, "event");
        auditStore.record(event);
        LOGGER.log(Level.INFO, () -> String.join(" ",
                "event=system_authority_operation",
                "operation=" + event.operation(),
                "outcome=" + event.outcome(),
                "subject_user_id=" + event.subjectUserId().map(Object::toString).orElse("-"),
                "system_role_id=" + event.systemRoleId().map(Object::toString).orElse("-"),
                "permission=" + event.permission().map(Object::toString).orElse("-"),
                "code=" + event.code().map(Enum::name).orElse("-"),
                "previous_version=" + event.previousVersion().map(Object::toString).orElse("-"),
                "resulting_version=" + event.resultingVersion().map(Object::toString).orElse("-"),
                "occurred_at=" + event.occurredAt(),
                "actor=" + event.actor().userId().map(Object::toString).orElse("SYSTEM"),
                "correlation_id=" + event.actor().correlationId().orElse("-")));
    }
}
