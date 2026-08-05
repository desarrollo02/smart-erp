package py.com.logixone.kernel.infrastructure.jakarta.company;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;
import py.com.logixone.kernel.application.company.audit.CompanyAuditEvent;
import py.com.logixone.kernel.application.company.port.CompanyAuditPort;
import py.com.logixone.kernel.infrastructure.jakarta.persistence.JpaTechnicalAuditStore;

/** Emits only technical identifiers and stable codes; no commercial data or credentials. */
@ApplicationScoped
public class StructuredCompanyAudit implements CompanyAuditPort {

    private static final Logger LOGGER = System.getLogger(StructuredCompanyAudit.class.getName());

    @Inject
    JpaTechnicalAuditStore auditStore;

    @Override
    public void record(CompanyAuditEvent event) {
        Objects.requireNonNull(event, "event");
        auditStore.record(event);
        LOGGER.log(Level.INFO, () -> String.join(" ",
                "event=company_operation",
                "company_id=" + event.companyId(),
                "operation=" + event.operation(),
                "outcome=" + event.outcome(),
                "plugin_id=" + event.pluginId().map(Object::toString).orElse("-"),
                "code=" + event.code().map(Enum::name).orElse("-"),
                "previous_version=" + event.previousVersion().map(Object::toString).orElse("-"),
                "resulting_version=" + event.resultingVersion().map(Object::toString).orElse("-"),
                "occurred_at=" + event.occurredAt(),
                "actor=" + event.actor(),
                "actor_user_id=" + event.actorUserId().map(Object::toString).orElse("-"),
                "correlation_id=" + event.correlationId().orElse("-")));
    }
}
