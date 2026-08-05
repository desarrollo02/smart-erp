package py.com.logixone.plugins.referencedata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ReferenceDataMigrationContractTest {

    @Test
    void migrationMarksTheVerifiedSeedAsAnIncompleteImmutableSubset() throws IOException {
        String sql;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/reference_data/V1__initialize_reference_data_schema.sql")) {
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(sql.contains("BOOTSTRAP_SUBSET"));
        assertTrue(sql.contains("748f6ff7380c8a50ea9448f068b79e3a1ee31be63207249e8cc89bf1eb969d11"));
        assertTrue(sql.contains("838dfb991648cf36df939edd5fe3811737962b75a32252847d239cedd1e291c9"));
        assertEquals(1, occurrences(sql, "'PY', 'PRY', '600'"));
        assertEquals(1, occurrences(sql, "'PYG', '600', 0"));
        assertEquals(1, occurrences(sql, "'USD', '840', 2"));
        assertTrue(sql.contains("CREATE UNIQUE INDEX uq_catalog_release_current"));
    }

    @Test
    void secondMigrationAddsConstrainedAppendOnlyPolicyHistory() throws IOException {
        String sql;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/reference_data/V2__add_company_policy_history.sql")) {
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(sql.contains("CREATE TABLE company_reference_policy_history"));
        assertTrue(sql.contains("PRIMARY KEY (company_id, catalog_kind, reference_code, version)"));
        assertTrue(sql.contains("catalog_kind IN ('COUNTRY', 'CURRENCY')"));
        assertTrue(sql.contains("version > 0"));
        assertTrue(sql.contains("actor_user_id UUID NOT NULL"));
        assertTrue(sql.contains("correlation_id VARCHAR(128) NOT NULL"));
        assertTrue(sql.contains("changed_at TIMESTAMPTZ NOT NULL"));
        assertFalse(sql.toUpperCase(java.util.Locale.ROOT).contains("UPDATE "));
        assertFalse(sql.toUpperCase(java.util.Locale.ROOT).contains("DELETE "));
    }

    @Test
    void thirdMigrationRepresentsNotApplicableMinorUnitAsSqlNull() throws IOException {
        String sql;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/reference_data/V3__support_not_applicable_minor_unit.sql")) {
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(sql.contains("ALTER COLUMN minor_unit DROP NOT NULL"));
        assertTrue(sql.contains("minor_unit IS NULL OR minor_unit BETWEEN 0 AND 9"));
        assertFalse(sql.contains("DEFAULT"));
        assertFalse(sql.contains("-1"));
    }

    @Test
    void fourthMigrationPublishesTheCompleteVerifiedCatalogs() throws IOException {
        String sql;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/reference_data/V4__publish_full_reference_data.sql")) {
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        String countryEntries = sql.substring(
                sql.indexOf("INSERT INTO country_entry"),
                sql.indexOf("INSERT INTO currency_entry"));
        String currencyEntries = sql.substring(sql.indexOf("INSERT INTO currency_entry"));

        assertTrue(sql.contains("UPDATE catalog_release SET current_release = FALSE"));
        assertTrue(sql.contains("'un-m49-2026-08-04-full'"));
        assertTrue(sql.contains("'six-list-one-2026-01-01-full'"));
        assertTrue(sql.contains("DATE '2026-08-04', 'FULL', 248, TRUE"));
        assertTrue(sql.contains("DATE '2026-08-04', 'FULL', 178, TRUE"));
        assertTrue(sql.contains("748f6ff7380c8a50ea9448f068b79e3a1ee31be63207249e8cc89bf1eb969d11"));
        assertTrue(sql.contains("838dfb991648cf36df939edd5fe3811737962b75a32252847d239cedd1e291c9"));
        assertEquals(248, occurrences(countryEntries, "    ('COUNTRY'"));
        assertEquals(178, occurrences(currencyEntries, "    ('CURRENCY'"));
        assertEquals(13, occurrences(currencyEntries, ", NULL,"));
        assertEquals(1, occurrences(countryEntries, "'PY', 'PRY', '600', 'Paraguay'"));
        assertEquals(1, occurrences(currencyEntries, "'PYG', '600', 0, 'Guarani'"));
        assertEquals(1, occurrences(currencyEntries, "'XDR', '960', NULL"));
    }

    private static int occurrences(String value, String token) {
        return value.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }
}
