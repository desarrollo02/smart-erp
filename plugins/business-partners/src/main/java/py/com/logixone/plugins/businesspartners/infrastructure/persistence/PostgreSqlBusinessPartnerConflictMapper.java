package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerPersistenceCode;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerPersistenceException;

final class PostgreSqlBusinessPartnerConflictMapper {

    private PostgreSqlBusinessPartnerConflictMapper() {
    }

    static BusinessPartnerPersistenceException map(PersistenceException failure) {
        SQLException sql = findSqlException(failure);
        if (sql == null || !"23505".equals(sql.getSQLState())) {
            return new BusinessPartnerPersistenceException(
                    BusinessPartnerPersistenceCode.INVALID_PERSISTED_STATE, failure);
        }
        String message = sql.getMessage() == null ? "" : sql.getMessage();
        if (message.contains("pk_business_partner_definition")) {
            return new BusinessPartnerPersistenceException(
                    BusinessPartnerPersistenceCode.GENERAL_CODE_ALREADY_EXISTS, failure);
        }
        if (message.contains("uq_business_partner_code")) {
            return new BusinessPartnerPersistenceException(
                    BusinessPartnerPersistenceCode.GENERAL_CODE_ALREADY_EXISTS, failure);
        }
        if (message.contains("uq_business_partner_role_code")) {
            return new BusinessPartnerPersistenceException(
                    BusinessPartnerPersistenceCode.ROLE_CODE_ALREADY_EXISTS, failure);
        }
        if (message.contains("pk_business_partner")) {
            return new BusinessPartnerPersistenceException(
                    BusinessPartnerPersistenceCode.PARTNER_ALREADY_EXISTS, failure);
        }
        return new BusinessPartnerPersistenceException(
                BusinessPartnerPersistenceCode.INVALID_PERSISTED_STATE, failure);
    }

    private static SQLException findSqlException(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLException sql) {
                return sql;
            }
            current = current.getCause();
        }
        return null;
    }
}
