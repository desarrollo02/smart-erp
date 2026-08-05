package py.com.logixone.plugins.commercialcatalog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CommercialCatalogDemoFixtureResourceTest {

    @Test
    void fixtureIsExplicitIdempotentAndConfinedToTheCatalogSchema() throws IOException {
        try (var stream = getClass().getResourceAsStream(
                "/demo/seed-commercial-catalog-demo.sql")) {
            assertNotNull(stream);
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(sql.contains("\\set ON_ERROR_STOP on"));
            assertTrue(sql.contains(":'company_id'::uuid"));
            assertTrue(sql.contains("plg_commercial_catalog.unit_definition"));
            assertTrue(sql.contains("plg_commercial_catalog.tax_profile_revision"));
            assertTrue(sql.contains("IVA_GENERAL_DEMO"));
            assertTrue(sql.contains("IVA_REDUCIDO_DEMO"));
            assertTrue(sql.contains("EXENTO_DEMO"));
            assertTrue(sql.contains("no representa una tasa o regla SIFEN certificada"));
            assertTrue(sql.contains("ON CONFLICT"));
            assertTrue(sql.contains("U&'"));
            assertTrue(sql.contains("IS DISTINCT FROM EXCLUDED.display_name"));
            assertFalse(sql.contains("core."));
            assertFalse(sql.toUpperCase().contains("DELETE"));
            assertFalse(sql.toUpperCase().contains("TRUNCATE"));
            assertFalse(sql.toUpperCase().contains("DROP "));
        }
    }
}
