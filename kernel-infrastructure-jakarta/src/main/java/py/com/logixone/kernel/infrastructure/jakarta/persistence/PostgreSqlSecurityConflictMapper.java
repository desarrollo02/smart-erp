package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import org.postgresql.util.PSQLException;
import py.com.logixone.kernel.application.security.port.SecurityPersistenceCode;
import py.com.logixone.kernel.application.security.port.SecurityPersistenceException;

final class PostgreSqlSecurityConflictMapper {

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    private PostgreSqlSecurityConflictMapper() {
    }

    static SecurityPersistenceException user(PersistenceException failure) {
        String constraint = constraintName(failure);
        if (UNIQUE_VIOLATION.equals(sqlState(failure))) {
            if ("app_user_external_identity_uk".equals(constraint)) {
                return conflict(SecurityPersistenceCode.EXTERNAL_IDENTITY_ALREADY_EXISTS, failure);
            }
            if ("app_user_pk".equals(constraint)) {
                return conflict(SecurityPersistenceCode.USER_ALREADY_EXISTS, failure);
            }
        }
        return generic(failure);
    }

    static SecurityPersistenceException membership(PersistenceException failure) {
        String constraint = constraintName(failure);
        if (UNIQUE_VIOLATION.equals(sqlState(failure))
                && "company_membership_pk".equals(constraint)) {
            return conflict(SecurityPersistenceCode.MEMBERSHIP_ALREADY_EXISTS, failure);
        }
        if (FOREIGN_KEY_VIOLATION.equals(sqlState(failure))) {
            if ("company_membership_user_fk".equals(constraint)) {
                return conflict(SecurityPersistenceCode.USER_NOT_FOUND, failure);
            }
            if ("company_membership_company_fk".equals(constraint)) {
                return conflict(SecurityPersistenceCode.COMPANY_NOT_FOUND, failure);
            }
        }
        return generic(failure);
    }

    static SecurityPersistenceException role(PersistenceException failure) {
        String constraint = constraintName(failure);
        if (UNIQUE_VIOLATION.equals(sqlState(failure))) {
            if ("security_role_pk".equals(constraint)
                    || "security_role_company_role_id_uk".equals(constraint)) {
                return conflict(SecurityPersistenceCode.ROLE_ALREADY_EXISTS, failure);
            }
            if ("security_role_company_code_uk".equals(constraint)) {
                return conflict(SecurityPersistenceCode.ROLE_CODE_ALREADY_EXISTS, failure);
            }
        }
        if (FOREIGN_KEY_VIOLATION.equals(sqlState(failure))
                && "security_role_company_fk".equals(constraint)) {
            return conflict(SecurityPersistenceCode.COMPANY_NOT_FOUND, failure);
        }
        return generic(failure);
    }

    static SecurityPersistenceException assignment(PersistenceException failure) {
        String constraint = constraintName(failure);
        if (UNIQUE_VIOLATION.equals(sqlState(failure))
                && "membership_role_pk".equals(constraint)) {
            return conflict(SecurityPersistenceCode.ASSIGNMENT_ALREADY_EXISTS, failure);
        }
        if (FOREIGN_KEY_VIOLATION.equals(sqlState(failure))) {
            if ("membership_role_membership_fk".equals(constraint)) {
                return conflict(SecurityPersistenceCode.MEMBERSHIP_NOT_FOUND, failure);
            }
            if ("membership_role_role_fk".equals(constraint)) {
                return conflict(SecurityPersistenceCode.ROLE_COMPANY_MISMATCH, failure);
            }
        }
        return generic(failure);
    }

    static SecurityPersistenceException grant(PersistenceException failure) {
        String constraint = constraintName(failure);
        if (UNIQUE_VIOLATION.equals(sqlState(failure))
                && "role_permission_pk".equals(constraint)) {
            return conflict(SecurityPersistenceCode.PERMISSION_GRANT_ALREADY_EXISTS, failure);
        }
        if (FOREIGN_KEY_VIOLATION.equals(sqlState(failure))
                && "role_permission_role_fk".equals(constraint)) {
            return conflict(SecurityPersistenceCode.ROLE_COMPANY_MISMATCH, failure);
        }
        return generic(failure);
    }

    static SecurityPersistenceException systemRole(PersistenceException failure) {
        String constraint = constraintName(failure);
        if (UNIQUE_VIOLATION.equals(sqlState(failure))) {
            if ("system_role_pk".equals(constraint)) {
                return conflict(SecurityPersistenceCode.SYSTEM_ROLE_ALREADY_EXISTS, failure);
            }
            if ("system_role_code_uk".equals(constraint)) {
                return conflict(SecurityPersistenceCode.SYSTEM_ROLE_CODE_ALREADY_EXISTS, failure);
            }
        }
        return generic(failure);
    }

    static SecurityPersistenceException systemAssignment(PersistenceException failure) {
        String constraint = constraintName(failure);
        if (UNIQUE_VIOLATION.equals(sqlState(failure))
                && "app_user_system_role_pk".equals(constraint)) {
            return conflict(SecurityPersistenceCode.SYSTEM_ASSIGNMENT_ALREADY_EXISTS, failure);
        }
        if (FOREIGN_KEY_VIOLATION.equals(sqlState(failure))) {
            if ("app_user_system_role_user_fk".equals(constraint)) {
                return conflict(SecurityPersistenceCode.USER_NOT_FOUND, failure);
            }
            if ("app_user_system_role_role_fk".equals(constraint)) {
                return conflict(SecurityPersistenceCode.SYSTEM_ROLE_NOT_FOUND, failure);
            }
        }
        return generic(failure);
    }

    static SecurityPersistenceException systemGrant(PersistenceException failure) {
        String constraint = constraintName(failure);
        if (UNIQUE_VIOLATION.equals(sqlState(failure))
                && "system_role_permission_pk".equals(constraint)) {
            return conflict(SecurityPersistenceCode.SYSTEM_PERMISSION_GRANT_ALREADY_EXISTS, failure);
        }
        if (FOREIGN_KEY_VIOLATION.equals(sqlState(failure))
                && "system_role_permission_role_fk".equals(constraint)) {
            return conflict(SecurityPersistenceCode.SYSTEM_ROLE_NOT_FOUND, failure);
        }
        return generic(failure);
    }

    static SecurityPersistenceException generic(PersistenceException failure) {
        return conflict(SecurityPersistenceCode.PERSISTENCE_CONFLICT, failure);
    }

    private static String sqlState(Throwable failure) {
        SQLException sqlFailure = findSqlFailure(failure);
        return sqlFailure == null ? null : sqlFailure.getSQLState();
    }

    private static String constraintName(Throwable failure) {
        SQLException sqlFailure = findSqlFailure(failure);
        if (sqlFailure instanceof PSQLException postgresFailure
                && postgresFailure.getServerErrorMessage() != null) {
            return postgresFailure.getServerErrorMessage().getConstraint();
        }
        return null;
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

    private static SecurityPersistenceException conflict(
            SecurityPersistenceCode code,
            Throwable failure) {
        return new SecurityPersistenceException(code, failure);
    }
}
