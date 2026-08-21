package py.com.logixone.plugins.sales.application;

import java.time.Clock;
import java.util.Optional;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.audit.TechnicalAuditOutcome;
import py.com.logixone.plugin.api.ContributionId;

final class SalesAuditRecorder {
    private final TechnicalAudit audit; private final Clock clock;
    SalesAuditRecorder(TechnicalAudit audit, Clock clock) { this.audit=audit; this.clock=clock; }
    void changed(SalesOperationContext context, ContributionId permission, String operation,
            String type, String id, Optional<Long> previous, long resulting) {
        record(context, permission, operation, type, Optional.of(id),
                TechnicalAuditOutcome.CHANGED, SalesResultCode.SUCCESS, previous,
                Optional.of(resulting));
    }
    void unchanged(SalesOperationContext context, ContributionId permission, String operation,
            String type, String id, long version) {
        record(context, permission, operation, type, Optional.of(id),
                TechnicalAuditOutcome.UNCHANGED, SalesResultCode.SUCCESS,
                Optional.of(version), Optional.of(version));
    }
    <T> SalesOperationResult<T> rejected(SalesOperationContext context,
            ContributionId permission, String operation, String type,
            Optional<String> id, Optional<Long> previous, SalesResultCode code) {
        record(context, permission, operation, type, id, TechnicalAuditOutcome.REJECTED,
                code, previous, Optional.empty());
        return SalesOperationResult.failure(code);
    }
    private void record(SalesOperationContext context, ContributionId permission,
            String operation, String type, Optional<String> id,
            TechnicalAuditOutcome outcome, SalesResultCode code,
            Optional<Long> previous, Optional<Long> resulting) {
        audit.record(new TechnicalAuditEvent(operation, outcome,
                context.companyContext().actor().userId(), context.companyContext().companyId(),
                SalesIdentity.PLUGIN_ID.value(), permission.value(), type, id, code.name(),
                previous, resulting, context.correlationId(), clock.instant()));
    }
}
