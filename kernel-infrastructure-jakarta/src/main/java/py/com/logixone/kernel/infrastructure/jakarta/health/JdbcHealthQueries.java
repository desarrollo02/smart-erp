package py.com.logixone.kernel.infrastructure.jakarta.health;

import java.sql.SQLException;

interface JdbcHealthQueries {

    boolean configurationIsAvailable();

    boolean databaseIsReachable() throws SQLException;

    boolean coreMigrationsAreReady() throws SQLException;
}
