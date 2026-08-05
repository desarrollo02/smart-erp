package py.com.logixone.kernel.application.company;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.audit.CompanyAuditActor;
import py.com.logixone.kernel.application.company.audit.CompanyAuditContext;
import py.com.logixone.kernel.application.company.audit.CompanyAuditEvent;
import py.com.logixone.kernel.application.company.audit.CompanyAuditOperation;
import py.com.logixone.kernel.application.company.audit.CompanyAuditOutcome;
import py.com.logixone.kernel.application.company.port.CompanyAuditPort;
import py.com.logixone.plugin.api.PluginId;

final class CompanyAuditRecorder {

    private final CompanyAuditPort auditPort;
    private final Clock clock;
    private final CompanyAuditContext context;

    CompanyAuditRecorder(CompanyAuditPort auditPort, Clock clock, CompanyAuditActor actor) {
        this(auditPort, clock, CompanyAuditContext.legacy(actor));
    }

    CompanyAuditRecorder(CompanyAuditPort auditPort, Clock clock, CompanyAuditContext context) {
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.context = Objects.requireNonNull(context, "context");
    }

    void record(
            CompanyId companyId,
            CompanyAuditOperation operation,
            CompanyAuditOutcome outcome,
            PluginId pluginId,
            CompanyOperationCode code,
            Long previousVersion,
            Long resultingVersion) {
        auditPort.record(new CompanyAuditEvent(
                companyId,
                operation,
                outcome,
                Optional.ofNullable(pluginId),
                Optional.ofNullable(code),
                Optional.ofNullable(previousVersion),
                Optional.ofNullable(resultingVersion),
                clock.instant(),
                context.actor(),
                context.actorUserId(),
                context.correlationId()));
    }
}
