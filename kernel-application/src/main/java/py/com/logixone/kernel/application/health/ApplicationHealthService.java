package py.com.logixone.kernel.application.health;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ApplicationHealthService {

    private static final Logger LOGGER = System.getLogger(ApplicationHealthService.class.getName());

    private final List<ReadinessCheck> readinessChecks;

    public ApplicationHealthService(Collection<? extends ReadinessCheck> readinessChecks) {
        Objects.requireNonNull(readinessChecks, "readinessChecks must not be null");
        List<ReadinessCheck> ordered = new ArrayList<>(readinessChecks.size());
        Set<String> names = new HashSet<>();
        for (ReadinessCheck readinessCheck : readinessChecks) {
            ReadinessCheck required = Objects.requireNonNull(readinessCheck, "readinessCheck must not be null");
            String name = new HealthCheckResult(required.name(), HealthStatus.UP).name();
            if (!names.add(name)) {
                throw new IllegalArgumentException("duplicate health check name");
            }
            ordered.add(required);
        }
        ordered.sort(Comparator.comparing(ReadinessCheck::name));
        this.readinessChecks = List.copyOf(ordered);
    }

    public HealthReport liveness() {
        return HealthReport.liveness();
    }

    public HealthReport readiness() {
        List<HealthCheckResult> results = readinessChecks.stream()
                .map(this::executeSafely)
                .toList();
        return HealthReport.readiness(results);
    }

    private HealthCheckResult executeSafely(ReadinessCheck readinessCheck) {
        String name = readinessCheck.name();
        try {
            HealthStatus status = Objects.requireNonNull(readinessCheck.check(), "health status must not be null");
            return new HealthCheckResult(name, status);
        } catch (RuntimeException failure) {
            LOGGER.log(
                    Level.WARNING,
                    "event=readiness_check_failed check=" + name + " type=" + failure.getClass().getSimpleName());
            return HealthCheckResult.down(name);
        }
    }
}
