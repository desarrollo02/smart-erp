package py.com.logixone.kernel.api.security;

import java.util.Objects;

/** Minimal authenticated actor; provider claims remain inside the trusted adapter. */
public record AuthenticatedActor(AppUserId userId) {

    public AuthenticatedActor {
        Objects.requireNonNull(userId, "userId");
    }
}
