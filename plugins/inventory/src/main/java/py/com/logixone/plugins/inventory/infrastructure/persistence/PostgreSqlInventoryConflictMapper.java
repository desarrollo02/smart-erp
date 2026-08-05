package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.persistence.OptimisticLockException;
import java.sql.SQLException;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceCode;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceException;

final class PostgreSqlInventoryConflictMapper {
    private PostgreSqlInventoryConflictMapper() {
    }

    static InventoryPersistenceException map(RuntimeException failure) {
        if (failure instanceof InventoryPersistenceException known) {
            return known;
        }
        if (failure instanceof OptimisticLockException) {
            return new InventoryPersistenceException(InventoryPersistenceCode.VERSION_CONFLICT, failure);
        }
        SQLException sql = findSqlException(failure);
        if (sql == null) {
            return new InventoryPersistenceException(InventoryPersistenceCode.STORAGE_FAILURE, failure);
        }
        return switch (sql.getSQLState()) {
            case "23505" -> new InventoryPersistenceException(InventoryPersistenceCode.DUPLICATE, failure);
            case "23503" -> new InventoryPersistenceException(InventoryPersistenceCode.REFERENCE_CONFLICT, failure);
            case "23P01" -> new InventoryPersistenceException(InventoryPersistenceCode.SCOPE_LOCKED, failure);
            case "23514" -> new InventoryPersistenceException(InventoryPersistenceCode.REFERENCE_CONFLICT, failure);
            default -> new InventoryPersistenceException(InventoryPersistenceCode.STORAGE_FAILURE, failure);
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
