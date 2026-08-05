package py.com.logixone.kernel.application.security.system.access;

import java.util.Objects;
import java.util.Optional;

/** Generic allow/deny result that does not expose internal denial diagnostics. */
public record SystemAuthorityAccess(Optional<SystemAuthorityContext> context) {

    public SystemAuthorityAccess {
        context = Objects.requireNonNull(context, "context");
    }

    public static SystemAuthorityAccess allowed(SystemAuthorityContext context) {
        return new SystemAuthorityAccess(Optional.of(Objects.requireNonNull(context, "context")));
    }

    public static SystemAuthorityAccess denied() {
        return new SystemAuthorityAccess(Optional.empty());
    }

    public boolean authorized() {
        return context.isPresent();
    }
}
