package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import org.postgresql.util.PSQLException;
import py.com.logixone.kernel.application.company.port.PersistenceConflictCode;
import py.com.logixone.kernel.application.company.port.PersistenceConflictException;

final class PostgreSqlConflictMapper {

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    private PostgreSqlConflictMapper() {
    }

    static PersistenceConflictException company(PersistenceException failure) {
        SQLException sqlFailure = findSqlFailure(failure);
        if (sqlFailure == null) {
            return conflict(PersistenceConflictCode.PERSISTENCE_CONFLICT, failure);
        }
        String constraint = constraintName(sqlFailure);
        if (UNIQUE_VIOLATION.equals(sqlFailure.getSQLState())) {
            if ("company_customization_plugin_id_uk".equals(constraint)) {
                return conflict(PersistenceConflictCode.CUSTOMIZATION_ALREADY_ASSIGNED, failure);
            }
            if ("company_pk".equals(constraint)) {
                return conflict(PersistenceConflictCode.COMPANY_ALREADY_EXISTS, failure);
            }
        }
        return conflict(PersistenceConflictCode.PERSISTENCE_CONFLICT, failure);
    }

    static PersistenceConflictException activation(PersistenceException failure) {
        SQLException sqlFailure = findSqlFailure(failure);
        if (sqlFailure == null) {
            return conflict(PersistenceConflictCode.PERSISTENCE_CONFLICT, failure);
        }
        String constraint = constraintName(sqlFailure);
        if (UNIQUE_VIOLATION.equals(sqlFailure.getSQLState())
                && "company_plugin_activation_pk".equals(constraint)) {
            return conflict(PersistenceConflictCode.ACTIVATION_ALREADY_EXISTS, failure);
        }
        if (FOREIGN_KEY_VIOLATION.equals(sqlFailure.getSQLState())
                && "company_plugin_activation_company_fk".equals(constraint)) {
            return conflict(PersistenceConflictCode.COMPANY_NOT_FOUND, failure);
        }
        return conflict(PersistenceConflictCode.PERSISTENCE_CONFLICT, failure);
    }

    private static SQLException findSqlFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLException sqlFailure) {
                return sqlFailure;
            }
            current = current.getCause();
        }
        return null;
    }

    private static String constraintName(SQLException failure) {
        if (failure instanceof PSQLException postgresFailure
                && postgresFailure.getServerErrorMessage() != null) {
            return postgresFailure.getServerErrorMessage().getConstraint();
        }
        return null;
    }

    private static PersistenceConflictException conflict(
            PersistenceConflictCode code,
            Throwable cause) {
        return new PersistenceConflictException(code, cause);
    }
}
