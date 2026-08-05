package py.com.logixone.kernel.infrastructure.jakarta.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import py.com.logixone.kernel.application.health.HealthStatus;
import py.com.logixone.kernel.application.health.ReadinessCheck;

@ApplicationScoped
public class DatabaseReadinessCheck implements ReadinessCheck {

    private final CoreDatabaseProbe probe;

    @Inject
    public DatabaseReadinessCheck(CoreDatabaseProbe probe) {
        this.probe = probe;
    }

    @Override
    public String name() {
        return "database";
    }

    @Override
    public HealthStatus check() {
        return probe.databaseStatus();
    }
}
