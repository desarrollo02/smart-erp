package py.com.logixone.plugins.sales.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;

public record SalesTransitionRecord(CompanyId companyId, UUID transitionId,
        String aggregateType, UUID aggregateId, String fromState, String toState,
        AppUserId actorId, Optional<String> reason, Instant occurredAt,
        String idempotencyKey) { }
