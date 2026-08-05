package py.com.logixone.kernel.infrastructure.jakarta.health;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import javax.sql.DataSource;
import py.com.logixone.kernel.infrastructure.jakarta.persistence.CorePersistenceNames;

@ApplicationScoped
public class ManagedDataSourceHealthQueries implements JdbcHealthQueries {

    private static final int PROBE_TIMEOUT_SECONDS = 1;
    private static final String DATABASE_QUERY = "SELECT 1";
    static final String EXPECTED_CORE_SCHEMA_VERSION = "6";
    static final String MIGRATIONS_QUERY = """
            SELECT
                EXISTS (
                    SELECT 1
                    FROM core.flyway_schema_history
                    WHERE version = '%s' AND success
                )
                AND EXISTS (
                    SELECT 1
                    FROM core.system_metadata
                    WHERE property_key = 'schema_owner' AND property_value = 'core'
                )
            """.formatted(EXPECTED_CORE_SCHEMA_VERSION);

    @Resource(lookup = CorePersistenceNames.DATA_SOURCE_JNDI)
    private DataSource dataSource;

    public ManagedDataSourceHealthQueries() {
    }

    ManagedDataSourceHealthQueries(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public boolean configurationIsAvailable() {
        return dataSource != null;
    }

    @Override
    public boolean databaseIsReachable() throws SQLException {
        return query(DATABASE_QUERY, result -> result.getInt(1) == 1);
    }

    @Override
    public boolean coreMigrationsAreReady() throws SQLException {
        return query(MIGRATIONS_QUERY, result -> result.getBoolean(1));
    }

    private boolean query(String sql, ResultCheck check) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(PROBE_TIMEOUT_SECONDS);
            try (ResultSet result = statement.executeQuery(sql)) {
                return result.next() && check.isReady(result);
            }
        }
    }

    @FunctionalInterface
    private interface ResultCheck {
        boolean isReady(ResultSet result) throws SQLException;
    }
}
