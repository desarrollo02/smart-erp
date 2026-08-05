package py.com.logixone.kernel.application.health;

import java.util.List;

public final class HealthReport {

    private final HealthStatus status;
    private final List<HealthCheckResult> checks;

    private HealthReport(List<HealthCheckResult> checks, HealthStatus emptyStatus) {
        this.checks = List.copyOf(checks);
        this.status = this.checks.isEmpty()
                ? emptyStatus
                : this.checks.stream().allMatch(result -> result.status() == HealthStatus.UP)
                        ? HealthStatus.UP
                        : HealthStatus.DOWN;
    }

    public static HealthReport liveness() {
        return new HealthReport(List.of(HealthCheckResult.up("application")), HealthStatus.UP);
    }

    public static HealthReport readiness(List<HealthCheckResult> checks) {
        return new HealthReport(checks, HealthStatus.DOWN);
    }

    public HealthStatus status() {
        return status;
    }

    public List<HealthCheckResult> checks() {
        return checks;
    }
}
