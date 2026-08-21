package py.com.logixone.plugins.sales.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class SalesMigrationResourceTest {
    @Test void v1DeclaresEightPrivateTablesAndRequiredInvariants() throws Exception {
        String sql;
        try(var stream=getClass().getResourceAsStream("/db/migration/sales/V1__initialize_sales_schema.sql")){
            assertNotNull(stream); sql=new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
        assertEquals(8,count(sql,"CREATE TABLE "));
        assertTrue(sql.contains("CREATE TABLE sales_quote ("));
        assertTrue(sql.contains("CREATE TABLE sales_order_reservation ("));
        assertTrue(sql.contains("uq_sales_order_source_quote"));
        assertTrue(sql.contains("trg_sales_operation_immutable"));
        assertTrue(sql.contains("NUMERIC(30,6)"));
        assertFalse(sql.contains("core.")); assertFalse(sql.contains("plg_inventory."));
        assertFalse(sql.toUpperCase(Locale.ROOT).contains(" JSON"));
    }
    private static int count(String value,String token){return value.split(java.util.regex.Pattern.quote(token),-1).length-1;}
}
