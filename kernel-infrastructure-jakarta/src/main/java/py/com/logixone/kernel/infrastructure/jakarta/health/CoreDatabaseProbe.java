package py.com.logixone.kernel.infrastructure.jakarta.health;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.sql.SQLException;
import py.com.logixone.kernel.application.health.HealthStatus;

@Dependent
public class CoreDatabaseProbe {

    private static final Logger LOGGER = System.getLogger(CoreDatabaseProbe.class.getName());

    private final JdbcHealthQueries queries;

    @Inject
    public CoreDatabaseProbe(ManagedDataSourceHealthQueries queries) {
        this.queries = queries;
    }

    CoreDatabaseProbe(JdbcHealthQueries queries) {
        this.queries = queries;
    }

    HealthStatus configurationStatus() {
        return execute("configuration", queries::configurationIsAvailable);
    }

    HealthStatus databaseStatus() {
        return execute("database", queries::databaseIsReachable);
    }

    HealthStatus migrationsStatus() {
        return execute("migrations", queries::coreMigrationsAreReady);
    }

    private HealthStatus execute(String check, CheckedProbe probe) {
        try {
            boolean up = probe.execute();
            if (!up) {
                LOGGER.log(Level.WARNING, "event=readiness_check_down check=" + check + " type=UnexpectedResult");
            }
            return up ? HealthStatus.UP : HealthStatus.DOWN;
        } catch (SQLException | RuntimeException failure) {
            LOGGER.log(
                    Level.WARNING,
                    "event=readiness_check_down check=" + check + " type=" + failure.getClass().getSimpleName());
            return HealthStatus.DOWN;
        }
    }

    @FunctionalInterface
    private interface CheckedProbe {
        boolean execute() throws SQLException;
    }
}
