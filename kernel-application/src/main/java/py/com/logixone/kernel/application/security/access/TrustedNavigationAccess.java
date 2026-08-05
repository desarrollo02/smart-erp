package py.com.logixone.kernel.application.security.access;

import java.util.Objects;
import java.util.Optional;

/** Current navigation projection or a closed internal denial. */
public record TrustedNavigationAccess(
        Optional<TrustedNavigationView> view,
        Optional<TrustedAccessCode> failure) {

    public TrustedNavigationAccess {
        view = Objects.requireNonNull(view, "view");
        failure = Objects.requireNonNull(failure, "failure");
        if (view.isPresent() == failure.isPresent()) {
            throw new IllegalArgumentException("navigation has exactly one result");
        }
    }

    public static TrustedNavigationAccess allowed(TrustedNavigationView view) {
        return new TrustedNavigationAccess(
                Optional.of(Objects.requireNonNull(view, "view")), Optional.empty());
    }

    public static TrustedNavigationAccess forbidden(TrustedAccessCode failure) {
        return new TrustedNavigationAccess(
                Optional.empty(), Optional.of(Objects.requireNonNull(failure, "failure")));
    }

    public boolean allowed() {
        return view.isPresent();
    }
}
