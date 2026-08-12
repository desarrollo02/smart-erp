package py.com.logixone.plugins.purchasing.application;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.purchasing.application.port.PurchasingOperationRecord;
import py.com.logixone.plugins.purchasing.application.port.PurchasingOperationRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchasingPersistenceException;
import py.com.logixone.plugins.purchasing.domain.ConcurrentPurchasingChangeException;

abstract class PurchasingApplicationService {
    protected final PurchasingOperationRepository operations;
    protected final PurchasingAuditRecorder audit;
    protected final Clock clock;

    PurchasingApplicationService(
            PurchasingOperationRepository operations,
            py.com.logixone.kernel.api.audit.TechnicalAudit technicalAudit,
            Clock clock) {
        this.operations = Objects.requireNonNull(operations, "operations");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.audit = new PurchasingAuditRecorder(technicalAudit, clock);
    }

    protected <T> Optional<PurchasingOperationResult<T>> replay(
            PurchasingOperationContext context,
            ContributionId permission,
            String operationType,
            String resourceType,
            String idempotencyKey,
            Object command,
            Function<UUID, Optional<T>> loader,
            ToLongFunction<T> version) {
        var stored = operations.find(context.companyContext().companyId(), idempotencyKey);
        if (stored.isEmpty()) {
            return Optional.empty();
        }
        var record = stored.orElseThrow();
        String fingerprint = PurchasingFingerprint.of(operationType, command);
        if (!record.operationType().equals(operationType)
                || !record.resourceType().equals(resourceType)
                || !record.requestFingerprint().equals(fingerprint)) {
            return Optional.of(audit.rejected(
                    context, permission, operationType, resourceType,
                    Optional.of(record.resourceId().toString()),
                    Optional.of(record.resultingVersion()),
                    PurchasingResultCode.IDEMPOTENCY_CONFLICT));
        }
        Optional<T> current = loader.apply(record.resourceId());
        if (current.isEmpty()) {
            return Optional.of(audit.rejected(
                    context, permission, operationType, resourceType,
                    Optional.of(record.resourceId().toString()),
                    Optional.of(record.resultingVersion()),
                    PurchasingResultCode.STORAGE_FAILURE));
        }
        T value = current.orElseThrow();
        long currentVersion = version.applyAsLong(value);
        audit.unchanged(context, permission, operationType, resourceType,
                record.resourceId().toString(), currentVersion);
        return Optional.of(PurchasingOperationResult.success(value));
    }

    protected void remember(PurchasingOperationContext context, String idempotencyKey,
            String operationType, Object command, String resourceType,
            UUID resourceId, long resultingVersion) {
        operations.append(new PurchasingOperationRecord(
                context.companyContext().companyId(), idempotencyKey, operationType,
                PurchasingFingerprint.of(operationType, command), resourceType,
                resourceId, resultingVersion, clock.instant()));
    }

    protected <T> PurchasingOperationResult<T> failure(
            PurchasingOperationContext context, ContributionId permission,
            String operation, String resourceType, Optional<String> resourceId,
            Optional<Long> previousVersion, RuntimeException failure) {
        PurchasingResultCode code = switch (failure) {
            case PurchasingInventoryFailure ignored -> PurchasingResultCode.INVENTORY_FAILURE;
            case PurchasingReferenceResolver.ReferenceFailure ignored ->
                    PurchasingResultCode.REFERENCE_CONFLICT;
            case ConcurrentPurchasingChangeException ignored ->
                    PurchasingResultCode.VERSION_CONFLICT;
            case PurchasingPersistenceException persistence ->
                    PurchasingApplicationSupport.map(persistence.code());
            case IllegalArgumentException ignored -> PurchasingResultCode.INVALID_OPERATION;
            case IllegalStateException ignored -> PurchasingResultCode.INVALID_OPERATION;
            default -> PurchasingResultCode.STORAGE_FAILURE;
        };
        return audit.rejected(context, permission, operation, resourceType,
                resourceId, previousVersion, code);
    }
}
