package py.com.logixone.kernel.application.security.audit;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.security.AppUserId;

/** Technical actor reference; provider claims and presentation data are never retained. */
public record SecurityAuditActor(
        Optional<AppUserId> userId,
        Optional<String> correlationId) {

    public static final SecurityAuditActor SYSTEM = new SecurityAuditActor(
            Optional.empty(), Optional.empty());

    public SecurityAuditActor {
        userId = Objects.requireNonNull(userId, "userId");
        correlationId = Objects.requireNonNull(correlationId, "correlationId")
                .map(String::trim)
                .filter(value -> !value.isEmpty());
        if (userId.isEmpty() && correlationId.isPresent()) {
            throw new IllegalArgumentException("system actor cannot contain correlationId");
        }
    }

    public SecurityAuditActor(Optional<AppUserId> userId) {
        this(userId, Optional.empty());
    }

    public static SecurityAuditActor authenticated(AppUserId userId) {
        return new SecurityAuditActor(
                Optional.of(Objects.requireNonNull(userId, "userId")), Optional.empty());
    }

    public static SecurityAuditActor authenticated(AppUserId userId, String correlationId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(correlationId, "correlationId");
        return new SecurityAuditActor(Optional.of(userId), Optional.of(correlationId));
    }
}
