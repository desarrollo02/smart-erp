package py.com.logixone.plugins.businesspartners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class BusinessPartnersMigrationResourceTest {

    private static final String RESOURCE =
            "/db/migration/business_partners/V1__initialize_business_partners_schema.sql";
    private static final String DEFINITIONS_RESOURCE =
            "/db/migration/business_partners/V2__add_company_owned_definitions.sql";
    private static final String DEFINITION_HISTORY_RESOURCE =
            "/db/migration/business_partners/V3__add_definition_revision_history.sql";
    private static final String CONTACT_DEFINITIONS_RESOURCE =
            "/db/migration/business_partners/V4__add_identification_and_address_definitions.sql";

    @Test
    void ownsEightRelationalTablesWithoutCrossSchemaReferencesOrDestructiveStatements()
            throws IOException {
        String sql = resourceText();
        List<String> tables = List.of(
                "business_partner",
                "business_partner_role",
                "business_partner_identification",
                "business_partner_address",
                "business_partner_channel",
                "business_partner_contact",
                "business_partner_contact_channel",
                "business_partner_code_sequence");

        assertEquals(8, count(sql, "CREATE TABLE "));
        tables.forEach(table -> assertTrue(sql.contains("CREATE TABLE " + table + " ("), table));
        assertTrue(sql.contains("uq_business_partner_code"));
        assertTrue(sql.contains("ix_business_partner_identification_candidate"));
        assertTrue(sql.contains("WHERE active AND is_primary"));
        assertFalse(sql.toLowerCase().contains("core."));
        assertFalse(sql.toUpperCase().contains("DROP "));
        assertFalse(sql.toUpperCase().contains("DELETE "));
        assertFalse(sql.toUpperCase().contains("TRUNCATE "));
    }

    @Test
    void addsACompanyScopedDefinitionTableAndSeedsOnlyFromPluginOwnedCompanies()
            throws IOException {
        String sql = resourceText(DEFINITIONS_RESOURCE);

        assertTrue(sql.contains("CREATE TABLE business_partner_definition ("));
        assertTrue(sql.contains("PRIMARY KEY (company_id, definition_kind, code)"));
        assertTrue(sql.contains("'CHANNEL_KIND'"));
        assertTrue(sql.contains("'email', 'Correo electrónico'"));
        assertTrue(sql.contains("FROM business_partner partner"));
        assertFalse(sql.toLowerCase().contains("core."));
        assertFalse(sql.toUpperCase().contains("DROP "));
        assertFalse(sql.toUpperCase().contains("DELETE "));
        assertFalse(sql.toUpperCase().contains("TRUNCATE "));
    }

    @Test
    void addsAppendOnlyDefinitionHistoryAndBackfillsTheCurrentRevision()
            throws IOException {
        String sql = resourceText(DEFINITION_HISTORY_RESOURCE);

        assertTrue(sql.contains("CREATE TABLE business_partner_definition_revision ("));
        assertTrue(sql.contains(
                "PRIMARY KEY (company_id, definition_kind, code, version)"));
        assertTrue(sql.contains(
                "REFERENCES business_partner_definition (company_id, definition_kind, code)"));
        assertTrue(sql.contains("INSERT INTO business_partner_definition_revision"));
        assertTrue(sql.contains("FROM business_partner_definition"));
        assertFalse(sql.toLowerCase().contains("core."));
        assertFalse(sql.toUpperCase().contains("DROP "));
        assertFalse(sql.toUpperCase().contains("DELETE "));
        assertFalse(sql.toUpperCase().contains("TRUNCATE "));
    }

    @Test
    void extendsDefinitionsWithoutDeletingOperationalOrHistoricalValues()
            throws IOException {
        String sql = resourceText(CONTACT_DEFINITIONS_RESOURCE);

        assertTrue(sql.contains("'IDENTIFICATION_TYPE'"));
        assertTrue(sql.contains("'ADDRESS_TYPE'"));
        assertTrue(sql.contains("'ADDRESS_PURPOSE'"));
        assertTrue(sql.contains("FROM business_partner_identification identification"));
        assertTrue(sql.contains("FROM business_partner_address address"));
        assertTrue(sql.contains("INSERT INTO business_partner_definition_revision"));
        assertTrue(sql.contains("ON CONFLICT (company_id, definition_kind, code) DO NOTHING"));
        assertFalse(sql.toLowerCase().contains("core."));
        assertFalse(sql.toUpperCase().contains("DELETE "));
        assertFalse(sql.toUpperCase().contains("TRUNCATE "));
    }

    private static String resourceText() throws IOException {
        return resourceText(RESOURCE);
    }

    private static String resourceText(String resource) throws IOException {
        try (InputStream input = BusinessPartnersMigrationResourceTest.class.getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int count(String text, String token) {
        int matches = 0;
        int position = 0;
        while ((position = text.indexOf(token, position)) >= 0) {
            matches++;
            position += token.length();
        }
        return matches;
    }
}
