package py.com.logixone.plugins.purchasing.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class PurchasingMigrationResourceTest {
    @Test
    void v1DeclaresNinePrivateTablesAndDocumentInvariants() throws IOException {
        String sql;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/purchasing/V1__initialize_purchasing_schema.sql")) {
            if (stream == null) {
                throw new IllegalStateException("Purchasing V1 resource is missing");
            }
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals(9, count(sql, "CREATE TABLE "));
        assertTrue(sql.contains("CREATE TABLE purchase_request ("));
        assertTrue(sql.contains("CREATE TABLE purchase_order_allocation ("));
        assertTrue(sql.contains("CREATE TABLE goods_receipt ("));
        assertTrue(sql.contains("CREATE TABLE supplier_return_line ("));
        assertTrue(sql.contains("trg_purchase_request_line_immutable"));
        assertTrue(sql.contains("trg_purchase_order_allocation_limits"));
        assertTrue(sql.contains("trg_goods_receipt_confirmation"));
        assertTrue(sql.contains("trg_supplier_return_immutable"));
        assertTrue(sql.contains("NUMERIC(30, 6)"));
        assertFalse(sql.contains("core."));
        assertFalse(sql.contains("plg_business_partners."));
        assertFalse(sql.contains("plg_commercial_catalog."));
        assertFalse(sql.contains("plg_reference_data."));
        assertFalse(sql.contains("plg_inventory."));
        assertFalse(sql.toUpperCase(Locale.ROOT).contains(" DOUBLE "));
        assertFalse(sql.toUpperCase(Locale.ROOT).contains(" FLOAT"));
        assertFalse(sql.toUpperCase(Locale.ROOT).contains(" JSON"));
    }

    @Test
    void v2DeclaresImmutableOperationAndImportLedgers() throws IOException {
        String sql;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/purchasing/V2__add_operation_and_import_ledgers.sql")) {
            if (stream == null) {
                throw new IllegalStateException("Purchasing V2 resource is missing");
            }
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(2, count(sql, "CREATE TABLE "));
        assertTrue(sql.contains("PRIMARY KEY (company_id, idempotency_key)"));
        assertTrue(sql.contains("PRIMARY KEY (company_id, source_system, source_record_key)"));
        assertTrue(sql.contains("request_fingerprint"));
    }

    private static int count(String value, String token) {
        return value.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }
}
