package py.com.logixone.plugins.purchasing.application;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.audit.TechnicalAuditOutcome;
import py.com.logixone.plugin.api.ContributionId;

final class PurchasingAuditRecorder {
    private final TechnicalAudit audit;
    private final Clock clock;

    PurchasingAuditRecorder(TechnicalAudit audit, Clock clock) {
        this.audit = Objects.requireNonNull(audit, "audit");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    void changed(PurchasingOperationContext context, ContributionId permission,
            String operation, String resourceType, String resourceId,
            Optional<Long> previousVersion, long resultingVersion) {
        record(context, permission, operation, resourceType, Optional.of(resourceId),
                TechnicalAuditOutcome.CHANGED, PurchasingResultCode.SUCCESS,
                previousVersion, Optional.of(resultingVersion));
    }

    void unchanged(PurchasingOperationContext context, ContributionId permission,
            String operation, String resourceType, String resourceId, long version) {
        record(context, permission, operation, resourceType, Optional.of(resourceId),
                TechnicalAuditOutcome.UNCHANGED, PurchasingResultCode.SUCCESS,
                Optional.of(version), Optional.of(version));
    }

    <T> PurchasingOperationResult<T> rejected(PurchasingOperationContext context,
            ContributionId permission, String operation, String resourceType,
            Optional<String> resourceId, Optional<Long> previousVersion,
            PurchasingResultCode code) {
        record(context, permission, operation, resourceType, resourceId,
                TechnicalAuditOutcome.REJECTED, code, previousVersion, Optional.empty());
        return PurchasingOperationResult.failure(code);
    }

    private void record(PurchasingOperationContext context, ContributionId permission,
            String operation, String resourceType, Optional<String> resourceId,
            TechnicalAuditOutcome outcome, PurchasingResultCode code,
            Optional<Long> previousVersion, Optional<Long> resultingVersion) {
        audit.record(new TechnicalAuditEvent(
                operation, outcome, context.companyContext().actor().userId(),
                context.companyContext().companyId(), PurchasingIdentity.PLUGIN_ID.value(),
                permission.value(), resourceType, resourceId, code.name(),
                previousVersion, resultingVersion, context.correlationId(), clock.instant()));
    }
}
