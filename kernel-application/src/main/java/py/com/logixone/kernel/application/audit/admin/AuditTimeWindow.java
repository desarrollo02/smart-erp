package py.com.logixone.kernel.application.audit.admin;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Closed time windows; the browser cannot inject arbitrary temporal expressions. */
public enum AuditTimeWindow {
    LAST_24_HOURS(Duration.ofHours(24)),
    LAST_7_DAYS(Duration.ofDays(7)),
    LAST_30_DAYS(Duration.ofDays(30)),
    ALL(null);

    private final Duration duration;

    AuditTimeWindow(Duration duration) {
        this.duration = duration;
    }

    public Optional<Instant> lowerBound(Instant now) {
        Objects.requireNonNull(now, "now");
        return duration == null ? Optional.empty() : Optional.of(now.minus(duration));
    }
}
