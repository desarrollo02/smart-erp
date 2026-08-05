package py.com.logixone.kernel.infrastructure.jakarta.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.application.health.HealthStatus;

class CoreDatabaseProbeTest {

    @Test
    void migrationProbeRequiresCoreV6WithoutRunningMigrations() {
        assertEquals("6", ManagedDataSourceHealthQueries.EXPECTED_CORE_SCHEMA_VERSION);
        assertTrue(ManagedDataSourceHealthQueries.MIGRATIONS_QUERY.contains("version = '6'"));
        assertFalse(ManagedDataSourceHealthQueries.MIGRATIONS_QUERY.contains("version = '1'"));
        assertFalse(ManagedDataSourceHealthQueries.MIGRATIONS_QUERY.contains("version = '2'"));
        assertFalse(ManagedDataSourceHealthQueries.MIGRATIONS_QUERY.contains("version = '3'"));
        assertFalse(ManagedDataSourceHealthQueries.MIGRATIONS_QUERY.contains("version = '4'"));
        assertFalse(ManagedDataSourceHealthQueries.MIGRATIONS_QUERY.contains("version = '5'"));
        assertFalse(ManagedDataSourceHealthQueries.MIGRATIONS_QUERY.toLowerCase().contains("insert "));
        assertFalse(ManagedDataSourceHealthQueries.MIGRATIONS_QUERY.toLowerCase().contains("update "));
        assertFalse(ManagedDataSourceHealthQueries.MIGRATIONS_QUERY.toLowerCase().contains("delete "));
    }

    @Test
    void configurationCheckDoesNotOpenADatabaseConnection() {
        JdbcHealthQueries forbiddenQueries = new JdbcHealthQueries() {
            @Override
            public boolean configurationIsAvailable() {
                return true;
            }

            @Override
            public boolean databaseIsReachable() {
                throw new AssertionError("database query must not run");
            }

            @Override
            public boolean coreMigrationsAreReady() {
                throw new AssertionError("migration query must not run");
            }
        };

        CoreDatabaseProbe probe = new CoreDatabaseProbe(forbiddenQueries);

        assertEquals(HealthStatus.UP, probe.configurationStatus());
    }

    @Test
    void databaseAndMigrationChecksReturnTheirControlledResults() {
        JdbcHealthQueries queries = new StubQueries(true, true, false);
        CoreDatabaseProbe probe = new CoreDatabaseProbe(queries);

        assertEquals(HealthStatus.UP, probe.databaseStatus());
        assertEquals(HealthStatus.DOWN, probe.migrationsStatus());
    }

    @Test
    void missingConfigurationAndSqlFailuresBecomeDownWithoutEscaping() {
        CoreDatabaseProbe invalidConfiguration = new CoreDatabaseProbe(new StubQueries(false, true, true));
        CoreDatabaseProbe sqlFailure = new CoreDatabaseProbe(
                new JdbcHealthQueries() {
                    @Override
                    public boolean configurationIsAvailable() {
                        return true;
                    }

                    @Override
                    public boolean databaseIsReachable() throws SQLException {
                        throw new SQLException("sensitive database diagnostic");
                    }

                    @Override
                    public boolean coreMigrationsAreReady() throws SQLException {
                        throw new SQLException("sensitive migration diagnostic");
                    }
                });

        assertEquals(HealthStatus.DOWN, invalidConfiguration.configurationStatus());
        assertEquals(HealthStatus.DOWN, sqlFailure.databaseStatus());
        assertEquals(HealthStatus.DOWN, sqlFailure.migrationsStatus());
    }

    private record StubQueries(
            boolean configurationResult,
            boolean databaseResult,
            boolean migrationsResult) implements JdbcHealthQueries {

        @Override
        public boolean configurationIsAvailable() {
            return configurationResult;
        }

        @Override
        public boolean databaseIsReachable() {
            return databaseResult;
        }

        @Override
        public boolean coreMigrationsAreReady() {
            return migrationsResult;
        }
    }
}
