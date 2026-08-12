package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugins.purchasing.application.port.PurchasingPersistenceCode;

class PostgreSqlPurchasingConflictMapperTest {
    @Test
    void translatesStablePostgreSqlStatesWithoutLeakingDriverDetails() {
        assertEquals(PurchasingPersistenceCode.DUPLICATE, map("23505"));
        assertEquals(PurchasingPersistenceCode.REFERENCE_CONFLICT, map("23503"));
        assertEquals(PurchasingPersistenceCode.REFERENCE_CONFLICT, map("23514"));
        assertEquals(PurchasingPersistenceCode.IMMUTABLE_DOCUMENT, map("P2001"));
        assertEquals(PurchasingPersistenceCode.STORAGE_FAILURE, map("08006"));
    }

    private static PurchasingPersistenceCode map(String sqlState) {
        return PostgreSqlPurchasingConflictMapper.map(
                new IllegalStateException(new SQLException("test", sqlState))).code();
    }
}
