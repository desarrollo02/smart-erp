package py.com.logixone.kernel.infrastructure.jakarta.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;
import py.com.logixone.kernel.application.security.audit.SecurityAuditEvent;
import py.com.logixone.kernel.application.security.port.SecurityAuditPort;
import py.com.logixone.kernel.infrastructure.jakarta.persistence.JpaTechnicalAuditStore;

/** Emits stable technical identifiers only; never issuer, subject, claims or credentials. */
@ApplicationScoped
public class StructuredSecurityAudit implements SecurityAuditPort {

    private static final Logger LOGGER = System.getLogger(StructuredSecurityAudit.class.getName());

    @Inject
    JpaTechnicalAuditStore auditStore;

    @Override
    public void record(SecurityAuditEvent event) {
        Objects.requireNonNull(event, "event");
        auditStore.record(event);
        LOGGER.log(Level.INFO, () -> String.join(" ",
                "event=security_operation",
                "operation=" + event.operation(),
                "outcome=" + event.outcome(),
                "subject_user_id=" + event.subjectUserId().map(Object::toString).orElse("-"),
                "company_id=" + event.companyId().map(Object::toString).orElse("-"),
                "role_id=" + event.roleId().map(Object::toString).orElse("-"),
                "permission_id=" + event.permissionId().map(Object::toString).orElse("-"),
                "code=" + event.code().map(Enum::name).orElse("-"),
                "previous_version=" + event.previousVersion().map(Object::toString).orElse("-"),
                "resulting_version=" + event.resultingVersion().map(Object::toString).orElse("-"),
                "occurred_at=" + event.occurredAt(),
                "actor=" + event.actor().userId().map(Object::toString).orElse("SYSTEM"),
                "correlation_id=" + event.actor().correlationId().orElse("-")));
    }
}
