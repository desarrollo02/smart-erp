package py.com.logixone.web.shell;

import jakarta.enterprise.context.SessionScoped;
import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Bounded, expiring and one-use store for native administration return contexts. */
@SessionScoped
public class NativeSelectorReturnContextStore implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int MAX_CONTEXTS = 4;
    private static final long MAX_AGE_MILLIS = 10 * 60 * 1000L;

    private final Map<String, StoredContext> contexts = new LinkedHashMap<>();

    synchronized String remember(NativeSelectorReturnContext context) {
        Objects.requireNonNull(context, "context");
        purgeExpired();
        while (contexts.size() >= MAX_CONTEXTS) {
            Iterator<String> oldest = contexts.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
        String token = UUID.randomUUID().toString();
        contexts.put(token, new StoredContext(context, nowMillis()));
        return token;
    }

    synchronized Optional<NativeSelectorReturnContext> findForTarget(
            String token, String userId, long sessionRevision, String targetRoute) {
        purgeExpired();
        return findBound(token, userId, sessionRevision)
                .filter(context -> context.targetRoute().equals(targetRoute));
    }

    synchronized Optional<NativeSelectorReturnContext> findForOrigin(
            String token, String userId, long sessionRevision, String originRoute) {
        purgeExpired();
        return findBound(token, userId, sessionRevision)
                .filter(context -> context.originRoute().equals(originRoute));
    }

    synchronized Optional<NativeSelectorReturnContext> consumeForOrigin(
            String token, String userId, long sessionRevision, String originRoute) {
        Optional<NativeSelectorReturnContext> found = findForOrigin(
                token, userId, sessionRevision, originRoute);
        found.ifPresent(ignored -> contexts.remove(token));
        return found;
    }

    public synchronized void clear() {
        contexts.clear();
    }

    long nowMillis() {
        return System.currentTimeMillis();
    }

    private Optional<NativeSelectorReturnContext> findBound(
            String token, String userId, long sessionRevision) {
        if (!validToken(token)) {
            return Optional.empty();
        }
        StoredContext stored = contexts.get(token);
        if (stored == null || !stored.context().belongsTo(userId, sessionRevision)) {
            return Optional.empty();
        }
        return Optional.of(stored.context());
    }

    private void purgeExpired() {
        long threshold = nowMillis() - MAX_AGE_MILLIS;
        contexts.values().removeIf(stored -> stored.createdAtMillis() < threshold);
    }

    private static boolean validToken(String token) {
        if (token == null || token.length() != 36) {
            return false;
        }
        try {
            return UUID.fromString(token).toString().equals(token);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private record StoredContext(
            NativeSelectorReturnContext context,
            long createdAtMillis) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }
}
