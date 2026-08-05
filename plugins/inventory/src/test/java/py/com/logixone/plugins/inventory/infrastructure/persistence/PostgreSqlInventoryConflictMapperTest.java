package py.com.logixone.plugins.inventory.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceCode;

class PostgreSqlInventoryConflictMapperTest {

    @Test
    void mapsStableApplicationCodesWithoutLeakingSqlDetails() {
        assertEquals(InventoryPersistenceCode.VERSION_CONFLICT,
                PostgreSqlInventoryConflictMapper.map(new OptimisticLockException()).code());
        assertEquals(InventoryPersistenceCode.DUPLICATE,
                PostgreSqlInventoryConflictMapper.map(sql("23505")).code());
        assertEquals(InventoryPersistenceCode.REFERENCE_CONFLICT,
                PostgreSqlInventoryConflictMapper.map(sql("23503")).code());
        assertEquals(InventoryPersistenceCode.SCOPE_LOCKED,
                PostgreSqlInventoryConflictMapper.map(sql("23P01")).code());
    }

    private static PersistenceException sql(String state) {
        return new PersistenceException(new SQLException("database detail", state));
    }
}
