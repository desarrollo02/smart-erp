package py.com.logixone.kernel.application.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationHealthServiceTest {

    @Test
    void livenessIsUpWithoutExecutingReadinessChecks() {
        ReadinessCheck exploding = check("database", () -> {
            throw new AssertionError("readiness check must not run");
        });

        HealthReport report = new ApplicationHealthService(List.of(exploding)).liveness();

        assertEquals(HealthStatus.UP, report.status());
        assertEquals(List.of(HealthCheckResult.up("application")), report.checks());
    }

    @Test
    void readinessIsDownWhenNoChecksAreRegistered() {
        HealthReport report = new ApplicationHealthService(List.of()).readiness();

        assertEquals(HealthStatus.DOWN, report.status());
        assertEquals(List.of(), report.checks());
    }

    @Test
    void readinessOrdersChecksAndIsUpWhenEveryCheckIsUp() {
        ApplicationHealthService service = new ApplicationHealthService(List.of(
                check("migrations", () -> HealthStatus.UP),
                check("catalog", () -> HealthStatus.UP),
                check("database", () -> HealthStatus.UP)));

        HealthReport report = service.readiness();

        assertEquals(HealthStatus.UP, report.status());
        assertEquals(
                List.of(
                        HealthCheckResult.up("catalog"),
                        HealthCheckResult.up("database"),
                        HealthCheckResult.up("migrations")),
                report.checks());
    }

    @Test
    void readinessIsDownWhenARegisteredCheckIsDown() {
        ApplicationHealthService service = new ApplicationHealthService(List.of(
                check("catalog", () -> HealthStatus.UP),
                check("database", () -> HealthStatus.DOWN)));

        HealthReport report = service.readiness();

        assertEquals(HealthStatus.DOWN, report.status());
        assertEquals(HealthCheckResult.down("database"), report.checks().get(1));
    }

    @Test
    void readinessConvertsRuntimeFailuresAndNullStatusesToDown() {
        ApplicationHealthService service = new ApplicationHealthService(List.of(
                check("database", () -> {
                    throw new IllegalStateException("sensitive diagnostic");
                }),
                check("migrations", () -> null)));

        HealthReport report = service.readiness();

        assertEquals(HealthStatus.DOWN, report.status());
        assertEquals(
                List.of(HealthCheckResult.down("database"), HealthCheckResult.down("migrations")),
                report.checks());
    }

    @Test
    void duplicateAndInvalidCheckNamesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ApplicationHealthService(List.of(
                        check("database", () -> HealthStatus.UP),
                        check("database", () -> HealthStatus.UP))));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ApplicationHealthService(List.of(check("Database URL", () -> HealthStatus.UP))));
    }

    private ReadinessCheck check(String name, StatusSupplier supplier) {
        return new ReadinessCheck() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public HealthStatus check() {
                return supplier.get();
            }
        };
    }

    @FunctionalInterface
    private interface StatusSupplier {
        HealthStatus get();
    }
}
