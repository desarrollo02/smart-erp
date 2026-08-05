package py.com.logixone.plugins.inventory.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class InventoryMigrationResourceTest {

    @Test
    void v1DeclaresNinePrivateTablesAndDatabaseInvariants() throws IOException {
        String sql;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/inventory/V1__initialize_inventory_schema.sql")) {
            if (stream == null) {
                throw new IllegalStateException("Inventory V1 resource is missing");
            }
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals(9, count(sql, "CREATE TABLE "));
        assertTrue(sql.contains("CREATE TABLE warehouse ("));
        assertTrue(sql.contains("CREATE TABLE inventory_balance ("));
        assertTrue(sql.contains("CREATE TABLE stock_movement ("));
        assertTrue(sql.contains("CREATE TABLE stock_reservation ("));
        assertTrue(sql.contains("CREATE TABLE stock_count ("));
        assertTrue(sql.contains("UNIQUE NULLS NOT DISTINCT"));
        assertTrue(sql.contains("uq_stock_movement_single_reversal"));
        assertTrue(sql.contains("uq_stock_movement_idempotency"));
        assertTrue(sql.contains("pg_advisory_xact_lock"));
        assertTrue(sql.contains("trg_stock_count_scope_lock"));
        assertFalse(sql.contains("plg_commercial_catalog."));
        assertFalse(sql.contains("core."));
        assertFalse(sql.toUpperCase(Locale.ROOT).contains(" DOUBLE "));
        assertFalse(sql.toUpperCase(Locale.ROOT).contains(" FLOAT"));
    }

    @Test
    void v2AddsAnImmutableReservationOperationLedger() throws IOException {
        String sql;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/inventory/V2__add_reservation_operation_ledger.sql")) {
            if (stream == null) {
                throw new IllegalStateException("Inventory V2 resource is missing");
            }
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals(1, count(sql, "CREATE TABLE "));
        assertTrue(sql.contains("CREATE TABLE stock_reservation_operation ("));
        assertTrue(sql.contains("PRIMARY KEY (company_id, idempotency_key)"));
        assertTrue(sql.contains("fk_stock_reservation_operation_owner"));
        assertFalse(sql.contains("plg_commercial_catalog."));
        assertFalse(sql.contains("core."));
        assertFalse(sql.toUpperCase(Locale.ROOT).contains(" DOUBLE "));
        assertFalse(sql.toUpperCase(Locale.ROOT).contains(" FLOAT"));
    }

    private static int count(String value, String token) {
        return value.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }
}
