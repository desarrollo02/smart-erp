package py.com.logixone.kernel.application.security.access;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.kernel.application.company.screen.ComposedScreen;
import py.com.logixone.plugin.api.ScreenId;

/** One fully composed screen or a closed access/composition denial. */
public record TrustedScreenAccess(
        ScreenId requestedScreenId,
        Optional<AuthenticatedCompanyContext> context,
        Optional<ComposedScreen> screen,
        Optional<TrustedAccessCode> failure) {

    public TrustedScreenAccess {
        Objects.requireNonNull(requestedScreenId, "requestedScreenId");
        context = Objects.requireNonNull(context, "context");
        screen = Objects.requireNonNull(screen, "screen");
        failure = Objects.requireNonNull(failure, "failure");
        if (screen.isPresent()) {
            if (context.isEmpty()
                    || failure.isPresent()
                    || !screen.orElseThrow().id().equals(requestedScreenId)) {
                throw new IllegalArgumentException(
                        "allowed screen requires matching screen, context and no failure");
            }
        } else if (failure.isEmpty()) {
            throw new IllegalArgumentException("denied screen requires a failure");
        }
    }

    public static TrustedScreenAccess allowed(
            AuthenticatedCompanyContext context,
            ComposedScreen screen) {
        Objects.requireNonNull(screen, "screen");
        return new TrustedScreenAccess(
                screen.id(),
                Optional.of(Objects.requireNonNull(context, "context")),
                Optional.of(screen),
                Optional.empty());
    }

    public static TrustedScreenAccess forbidden(
            ScreenId requestedScreenId,
            Optional<AuthenticatedCompanyContext> context,
            TrustedAccessCode failure) {
        return new TrustedScreenAccess(
                requestedScreenId,
                Objects.requireNonNull(context, "context"),
                Optional.empty(),
                Optional.of(Objects.requireNonNull(failure, "failure")));
    }

    public boolean allowed() {
        return screen.isPresent();
    }
}
