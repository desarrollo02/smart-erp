package py.com.logixone.kernel.application.company.audit;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.security.AppUserId;

/** Technical actor and server-generated correlation for one company operation. */
public record CompanyAuditContext(
        CompanyAuditActor actor,
        Optional<AppUserId> actorUserId,
        Optional<String> correlationId) {

    public static final CompanyAuditContext SYSTEM = new CompanyAuditContext(
            CompanyAuditActor.SYSTEM, Optional.empty(), Optional.empty());

    public CompanyAuditContext {
        Objects.requireNonNull(actor, "actor");
        actorUserId = Objects.requireNonNull(actorUserId, "actorUserId");
        correlationId = Objects.requireNonNull(correlationId, "correlationId")
                .map(String::trim)
                .filter(value -> !value.isEmpty());
        if (actor == CompanyAuditActor.AUTHENTICATED_USER
                && (actorUserId.isEmpty() || correlationId.isEmpty())) {
            throw new IllegalArgumentException(
                    "authenticated company operations require actorUserId and correlationId");
        }
        if (actor != CompanyAuditActor.AUTHENTICATED_USER && actorUserId.isPresent()) {
            throw new IllegalArgumentException("only authenticated operations contain actorUserId");
        }
    }

    public static CompanyAuditContext authenticated(AppUserId userId, String correlationId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(correlationId, "correlationId");
        return new CompanyAuditContext(
                CompanyAuditActor.AUTHENTICATED_USER,
                Optional.of(userId),
                Optional.of(correlationId));
    }

    public static CompanyAuditContext legacy(CompanyAuditActor actor) {
        Objects.requireNonNull(actor, "actor");
        if (actor == CompanyAuditActor.AUTHENTICATED_USER) {
            throw new IllegalArgumentException("authenticated actor requires explicit context");
        }
        return new CompanyAuditContext(actor, Optional.empty(), Optional.empty());
    }
}
