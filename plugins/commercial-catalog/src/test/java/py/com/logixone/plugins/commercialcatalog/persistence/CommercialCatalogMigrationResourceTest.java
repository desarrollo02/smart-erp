package py.com.logixone.plugins.commercialcatalog.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CommercialCatalogMigrationResourceTest {

    @Test
    void v1DeclaresTwentyPrivateRelationalTablesAndConcurrencyGuards() throws IOException {
        String sql;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/commercial_catalog/V1__initialize_commercial_catalog_schema.sql")) {
            if (stream == null) {
                throw new IllegalStateException("Commercial catalog V1 resource is missing");
            }
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals(20, count(sql, "CREATE TABLE "));
        assertTrue(sql.contains("CREATE TABLE catalog_item ("));
        assertTrue(sql.contains("CREATE TABLE price_list ("));
        assertTrue(sql.contains("CREATE TABLE catalog_code_sequence ("));
        assertTrue(sql.contains("pg_advisory_xact_lock"));
        assertTrue(sql.contains("trg_price_entry_overlap"));
        assertTrue(sql.contains("trg_tax_profile_revision_overlap"));
        assertFalse(sql.contains("core."));
        assertFalse(sql.contains("business_partner"));
        assertFalse(sql.toUpperCase(java.util.Locale.ROOT).contains(" DOUBLE "));
        assertFalse(sql.toUpperCase(java.util.Locale.ROOT).contains(" FLOAT"));
    }

    @Test
    void v2AddsAppendOnlyHistoryForEverySimpleDefinition() throws IOException {
        String sql;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/commercial_catalog/V2__version_simple_catalog_definitions.sql")) {
            if (stream == null) {
                throw new IllegalStateException("Commercial catalog V2 resource is missing");
            }
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals(4, count(sql, "CREATE TABLE "));
        assertEquals(4, count(sql, "INSERT INTO "));
        assertTrue(sql.contains("CREATE TABLE unit_definition_revision ("));
        assertTrue(sql.contains("CREATE TABLE category_definition_revision ("));
        assertTrue(sql.contains("CREATE TABLE brand_definition_revision ("));
        assertTrue(sql.contains("CREATE TABLE tag_definition_revision ("));
        assertTrue(sql.contains("definition_version BIGINT NOT NULL"));
        assertFalse(sql.contains("core."));
        assertFalse(sql.contains("business_partner"));
    }

    @Test
    void v3LinksEachReplacedDefinitionWithoutCrossPluginAccess() throws IOException {
        String sql;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/commercial_catalog/"
                        + "V3__link_replaced_simple_catalog_definitions.sql")) {
            if (stream == null) {
                throw new IllegalStateException("Commercial catalog V3 resource is missing");
            }
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals(4, count(sql, "ALTER TABLE "));
        assertEquals(4, count(sql, "CREATE INDEX "));
        assertTrue(sql.contains("replacement_unit_code VARCHAR(16)"));
        assertTrue(sql.contains("replacement_category_id UUID"));
        assertTrue(sql.contains("replacement_brand_id UUID"));
        assertTrue(sql.contains("replacement_tag_id UUID"));
        assertEquals(4, count(sql, "FOREIGN KEY (company_id, replacement_"));
        assertEquals(4, count(sql, "OR state = 'INACTIVE'"));
        assertFalse(sql.contains("core."));
        assertFalse(sql.contains("business_partner"));
    }

    @Test
    void v4VersionsVariantFamilySnapshotsAndExistingAssignments() throws IOException {
        String sql;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/commercial_catalog/"
                        + "V4__version_variant_family_history.sql")) {
            if (stream == null) {
                throw new IllegalStateException("Commercial catalog V4 resource is missing");
            }
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals(2, count(sql, "CREATE TABLE "));
        assertTrue(sql.contains("CREATE TABLE variant_family_revision ("));
        assertTrue(sql.contains("CREATE TABLE variant_attribute_revision ("));
        assertEquals(2, count(sql, "INSERT INTO variant_"));
        assertEquals(2, count(sql, "ADD COLUMN variant_family_version BIGINT"));
        assertTrue(sql.contains("SET variant_family_version = family.version"));
        assertTrue(sql.contains("SET variant_family_version = assignment.variant_family_version"));
        assertTrue(sql.contains("DROP CONSTRAINT fk_catalog_item_variant_attribute_definition"));
        assertTrue(sql.contains("fk_catalog_item_variant_attribute_revision"));
        assertFalse(sql.contains("core."));
        assertFalse(sql.contains("business_partner"));
    }

    private static int count(String value, String token) {
        return value.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }
}
