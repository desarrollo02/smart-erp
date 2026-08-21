package py.com.logixone.plugins.sales.application;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.sales.application.port.*;
import py.com.logixone.plugins.sales.domain.ConcurrentSalesChangeException;

abstract class SalesApplicationService {
    protected final SalesOperationRepository operations; protected final SalesAuditRecorder audit;
    protected final Clock clock;
    SalesApplicationService(SalesOperationRepository operations,
            py.com.logixone.kernel.api.audit.TechnicalAudit audit, Clock clock) {
        this.operations=operations; this.audit=new SalesAuditRecorder(audit,clock); this.clock=clock;
    }
    protected <T> Optional<SalesOperationResult<T>> replay(SalesOperationContext context,
            ContributionId permission,String operation,String type,String key,Object command,
            Function<UUID,Optional<T>> loader,ToLongFunction<T> version) {
        var existing=operations.find(context.companyContext().companyId(),key);
        if(existing.isEmpty()) return Optional.empty();
        var record=existing.orElseThrow();
        if(!record.operationType().equals(operation)||!record.aggregateType().equals(type)
                ||!record.fingerprint().equals(SalesFingerprint.of(operation,command))) {
            return Optional.of(audit.rejected(context,permission,operation,type,
                    Optional.of(record.aggregateId().toString()),Optional.empty(),SalesResultCode.IDEMPOTENCY_CONFLICT));
        }
        var value=loader.apply(record.aggregateId());
        if(value.isEmpty()) return Optional.of(audit.rejected(context,permission,operation,type,
                Optional.of(record.aggregateId().toString()),Optional.empty(),SalesResultCode.STORAGE_FAILURE));
        T result=value.orElseThrow(); audit.unchanged(context,permission,operation,type,
                record.aggregateId().toString(),version.applyAsLong(result));
        return Optional.of(SalesOperationResult.success(result));
    }
    protected void remember(SalesOperationContext context,String key,String operation,
            Object command,String type,UUID id) {
        operations.append(new SalesOperationRecord(context.companyContext().companyId(),key,
                operation,SalesFingerprint.of(operation,command),type,id,clock.instant()));
    }
    protected <T> SalesOperationResult<T> failure(SalesOperationContext context,
            ContributionId permission,String operation,String type,Optional<String> id,
            Optional<Long> version,RuntimeException failure) {
        SalesResultCode code=switch(failure) {
            case SalesReferenceResolver.ReferenceFailure ignored -> SalesResultCode.REFERENCE_CONFLICT;
            case ConcurrentSalesChangeException ignored -> SalesResultCode.VERSION_CONFLICT;
            case SalesPersistenceException value -> SalesApplicationSupport.map(value.code());
            case IllegalArgumentException ignored -> SalesResultCode.INVALID_OPERATION;
            case IllegalStateException ignored -> SalesResultCode.INVALID_OPERATION;
            default -> SalesResultCode.STORAGE_FAILURE;
        };
        return audit.rejected(context,permission,operation,type,id,version,code);
    }
}
