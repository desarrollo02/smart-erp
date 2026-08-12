package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.persistence.OptimisticLockException;
import java.sql.SQLException;
import py.com.logixone.plugins.purchasing.application.port.PurchasingPersistenceCode;
import py.com.logixone.plugins.purchasing.application.port.PurchasingPersistenceException;

final class PostgreSqlPurchasingConflictMapper {
    private PostgreSqlPurchasingConflictMapper() {
    }

    static PurchasingPersistenceException map(RuntimeException failure) {
        if (failure instanceof PurchasingPersistenceException known) {
            return known;
        }
        if (failure instanceof OptimisticLockException) {
            return new PurchasingPersistenceException(
                    PurchasingPersistenceCode.VERSION_CONFLICT, failure);
        }
        SQLException sql = findSqlException(failure);
        if (sql == null) {
            return new PurchasingPersistenceException(
                    PurchasingPersistenceCode.STORAGE_FAILURE, failure);
        }
        return switch (sql.getSQLState()) {
            case "23505" -> new PurchasingPersistenceException(
                    PurchasingPersistenceCode.DUPLICATE, failure);
            case "23503", "23514" -> new PurchasingPersistenceException(
                    PurchasingPersistenceCode.REFERENCE_CONFLICT, failure);
            case "P2001" -> new PurchasingPersistenceException(
                    PurchasingPersistenceCode.IMMUTABLE_DOCUMENT, failure);
            default -> new PurchasingPersistenceException(
                    PurchasingPersistenceCode.STORAGE_FAILURE, failure);
        };
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
