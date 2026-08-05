package py.com.logixone.plugins.inventory.application;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.audit.TechnicalAuditOutcome;
import py.com.logixone.plugin.api.ContributionId;

final class InventoryAuditRecorder {
    private final TechnicalAudit audit;
    private final Clock clock;

    InventoryAuditRecorder(TechnicalAudit audit, Clock clock) {
        this.audit = Objects.requireNonNull(audit, "audit");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    void changed(
            InventoryOperationContext context,
            ContributionId permission,
            String operation,
            String resourceType,
            String resourceId,
            Optional<Long> previousVersion,
            long resultingVersion) {
        record(context, permission, operation, resourceType, Optional.of(resourceId),
                TechnicalAuditOutcome.CHANGED, InventoryResultCode.SUCCESS,
                previousVersion, Optional.of(resultingVersion));
    }

    void unchanged(
            InventoryOperationContext context,
            ContributionId permission,
            String operation,
            String resourceType,
            String resourceId,
            long version) {
        record(context, permission, operation, resourceType, Optional.of(resourceId),
                TechnicalAuditOutcome.UNCHANGED, InventoryResultCode.SUCCESS,
                Optional.of(version), Optional.of(version));
    }

    <T> InventoryOperationResult<T> rejected(
            InventoryOperationContext context,
            ContributionId permission,
            String operation,
            String resourceType,
            Optional<String> resourceId,
            Optional<Long> previousVersion,
            InventoryResultCode code) {
        record(context, permission, operation, resourceType, resourceId,
                TechnicalAuditOutcome.REJECTED, code, previousVersion, Optional.empty());
        return InventoryOperationResult.failure(code);
    }

    private void record(
            InventoryOperationContext context,
            ContributionId permission,
            String operation,
            String resourceType,
            Optional<String> resourceId,
            TechnicalAuditOutcome outcome,
            InventoryResultCode code,
            Optional<Long> previousVersion,
            Optional<Long> resultingVersion) {
        audit.record(new TechnicalAuditEvent(
                operation,
                outcome,
                context.companyContext().actor().userId(),
                context.companyContext().companyId(),
                InventoryIdentity.PLUGIN_ID.value(),
                permission.value(),
                resourceType,
                resourceId,
                code.name(),
                previousVersion,
                resultingVersion,
                context.correlationId(),
                clock.instant()));
    }
}
