package py.com.logixone.kernel.application.health;

import java.util.Objects;
import java.util.regex.Pattern;

public record HealthCheckResult(String name, HealthStatus status) {

    private static final Pattern VALID_NAME = Pattern.compile("[a-z][a-z0-9-]{0,63}");

    public HealthCheckResult {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (!VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid health check name");
        }
    }

    public static HealthCheckResult up(String name) {
        return new HealthCheckResult(name, HealthStatus.UP);
    }

    public static HealthCheckResult down(String name) {
        return new HealthCheckResult(name, HealthStatus.DOWN);
    }
}
