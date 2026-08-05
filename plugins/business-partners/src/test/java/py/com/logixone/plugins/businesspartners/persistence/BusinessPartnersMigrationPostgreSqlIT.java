package py.com.logixone.plugins.businesspartners.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class BusinessPartnersMigrationPostgreSqlIT {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse(
                    "postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("logixone_business_partners_test")
            .withUsername("logixone_test")
            .withPassword("test-only-password");

    @BeforeAll
    static void migrateSchema() {
        MigrateResult first = flyway().migrate();
        MigrateResult second = flyway().migrate();
        assertEquals(4, first.migrationsExecuted);
        assertEquals(0, second.migrationsExecuted);
        flyway().validate();
    }

    @Test
    void createsTenPrivateTablesAndOneIndependentFlywayHistory() throws SQLException {
        assertEquals(10, queryInt("""
                SELECT count(*)
                FROM information_schema.tables
                WHERE table_schema = 'plg_business_partners'
                  AND table_name <> 'flyway_schema_history'
                """));
        assertEquals(4, queryInt("""
                SELECT count(*)
                FROM plg_business_partners.flyway_schema_history
                WHERE success AND version IN ('1', '2', '3', '4')
                """));
    }

    @Test
    void acceptsTheFourGovernedDefinitionKindsAndRejectsUnknownKinds()
            throws SQLException {
        UUID company = UUID.randomUUID();

        insertDefinition(company, "CHANNEL_KIND", "telegram", "Telegram");
        insertDefinition(company, "IDENTIFICATION_TYPE", "passport", "Pasaporte");
        insertDefinition(company, "ADDRESS_TYPE", "postal", "Postal");
        insertDefinition(company, "ADDRESS_PURPOSE", "billing", "Facturación");

        assertEquals(4, queryInt("""
                SELECT count(*)
                FROM plg_business_partners.business_partner_definition
                WHERE company_id = '%s'
                """.formatted(company)));
        assertSqlState("23514", () -> insertDefinition(
                company, "UNMANAGED_KIND", "invalid", "Inválida"));
    }

    @Test
    void keepsDefinitionCodesUniqueInsideKindAndCompany() throws SQLException {
        UUID companyA = UUID.randomUUID();
        UUID companyB = UUID.randomUUID();

        insertDefinition(companyA, "email", "Correo electrónico");
        insertDefinition(companyB, "email", "Correo electrónico");
        assertSqlState("23505", () -> insertDefinition(companyA, "email", "Duplicado"));

        assertEquals(1, queryInt("""
                SELECT count(*)
                FROM plg_business_partners.business_partner_definition
                WHERE company_id = '%s' AND definition_kind = 'CHANNEL_KIND'
                """.formatted(companyA)));
    }

    @Test
    void keepsDefinitionRevisionsAppendOnlyAndInsideTheirOwningCompany()
            throws SQLException {
        UUID company = UUID.randomUUID();
        UUID otherCompany = UUID.randomUUID();
        insertDefinition(company, "email_history", "Correo histórico");
        insertDefinitionRevision(company, "email_history", 0, "Correo histórico");

        assertSqlState("23505", () -> insertDefinitionRevision(
                company, "email_history", 0, "Duplicado"));
        assertSqlState("23503", () -> insertDefinitionRevision(
                otherCompany, "email_history", 0, "Sin definición"));
        assertEquals(1, queryInt("""
                SELECT count(*)
                FROM plg_business_partners.business_partner_definition_revision
                WHERE company_id = '%s'
                  AND definition_kind = 'CHANNEL_KIND'
                  AND code = 'email_history'
                """.formatted(company)));
    }

    @Test
    void scopesGeneralAndRoleCodesByCompanyAndKeepsIdentificationAsWarningOnly()
            throws SQLException {
        UUID companyA = UUID.randomUUID();
        UUID companyB = UUID.randomUUID();
        UUID partnerA = insertPartner(companyA, "BP-SHARED");
        UUID partnerB = insertPartner(companyB, "BP-SHARED");

        assertSqlState("23505", () -> insertPartner(companyA, "BP-SHARED"));
        insertRole(companyA, partnerA, "CLIENT", "CUSTOMER-1");
        insertRole(companyB, partnerB, "CLIENT", "CUSTOMER-1");
        UUID anotherA = insertPartner(companyA, "BP-SECOND");
        assertSqlState("23505", () -> insertRole(companyA, anotherA, "CLIENT", "CUSTOMER-1"));

        insertIdentification(companyA, partnerA, UUID.randomUUID(), "80001234-5", "800012345");
        insertIdentification(companyA, anotherA, UUID.randomUUID(), "80001234-5", "800012345");
        assertEquals(2, queryInt("""
                SELECT count(*)
                FROM plg_business_partners.business_partner_identification
                WHERE company_id = '%s' AND normalized_value = '800012345'
                """.formatted(companyA)));
    }

    @Test
    void rejectsCrossCompanyChildrenAndMultipleActivePrimaryValues() throws SQLException {
        UUID companyA = UUID.randomUUID();
        UUID companyB = UUID.randomUUID();
        UUID partner = insertPartner(companyA, "BP-OWNER");

        assertSqlState("23503", () -> insertIdentification(
                companyB, partner, UUID.randomUUID(), "123", "123"));

        insertAddress(companyA, partner, UUID.randomUUID(), true);
        assertSqlState("23505", () -> insertAddress(
                companyA, partner, UUID.randomUUID(), true));
        insertAddress(companyA, partner, UUID.randomUUID(), false);
        assertEquals(2, queryInt("""
                SELECT count(*)
                FROM plg_business_partners.business_partner_address
                WHERE company_id = '%s' AND business_partner_id = '%s'
                """.formatted(companyA, partner)));
    }

    @Test
    void inactivationPreservesRootAndAllDetails() throws SQLException {
        UUID company = UUID.randomUUID();
        UUID partner = insertPartner(company, "BP-RETAIN");
        insertIdentification(company, partner, UUID.randomUUID(), "ABC-123", "ABC123");

        execute("""
                UPDATE plg_business_partners.business_partner
                SET state = 'INACTIVE', version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE company_id = '%s' AND business_partner_id = '%s'
                """.formatted(company, partner));

        assertEquals("INACTIVE", queryString("""
                SELECT state FROM plg_business_partners.business_partner
                WHERE company_id = '%s' AND business_partner_id = '%s'
                """.formatted(company, partner)));
        assertEquals(1, queryInt("""
                SELECT count(*) FROM plg_business_partners.business_partner_identification
                WHERE company_id = '%s' AND business_partner_id = '%s'
                """.formatted(company, partner)));
    }

    private static Flyway flyway() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("plg_business_partners")
                .defaultSchema("plg_business_partners")
                .table("flyway_schema_history")
                .locations("classpath:db/migration/business_partners")
                .createSchemas(true)
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .validateOnMigrate(true)
                .failOnMissingLocations(true)
                .outOfOrder(false)
                .load();
    }

    private static UUID insertPartner(UUID companyId, String code) throws SQLException {
        UUID id = UUID.randomUUID();
        execute("""
                INSERT INTO plg_business_partners.business_partner
                    (company_id, business_partner_id, code, kind, display_name, state)
                VALUES ('%s', '%s', '%s', 'ORGANIZATION', 'Test partner', 'ACTIVE')
                """.formatted(companyId, id, code));
        return id;
    }

    private static void insertRole(UUID companyId, UUID partnerId, String role, String code)
            throws SQLException {
        execute("""
                INSERT INTO plg_business_partners.business_partner_role
                    (company_id, business_partner_id, role_type, state, role_code)
                VALUES ('%s', '%s', '%s', 'ACTIVE', '%s')
                """.formatted(companyId, partnerId, role, code));
    }

    private static void insertIdentification(
            UUID companyId, UUID partnerId, UUID identificationId, String presented, String normalized)
            throws SQLException {
        execute("""
                INSERT INTO plg_business_partners.business_partner_identification
                    (company_id, business_partner_id, identification_id, type_code,
                     presented_value, normalized_value)
                VALUES ('%s', '%s', '%s', 'tax_id', '%s', '%s')
                """.formatted(companyId, partnerId, identificationId, presented, normalized));
    }

    private static void insertAddress(UUID companyId, UUID partnerId, UUID addressId, boolean primary)
            throws SQLException {
        execute("""
                INSERT INTO plg_business_partners.business_partner_address
                    (company_id, business_partner_id, address_id, type_code, purpose_code,
                     address_line, active, is_primary)
                VALUES ('%s', '%s', '%s', 'postal', 'billing', 'Demo address', true, %s)
                """.formatted(companyId, partnerId, addressId, primary));
    }

    private static void insertDefinition(UUID companyId, String code, String name)
            throws SQLException {
        insertDefinition(companyId, "CHANNEL_KIND", code, name);
    }

    private static void insertDefinition(
            UUID companyId, String kind, String code, String name)
            throws SQLException {
        execute("""
                INSERT INTO plg_business_partners.business_partner_definition
                    (company_id, definition_kind, code, display_name, state)
                VALUES ('%s', '%s', '%s', '%s', 'ACTIVE')
                """.formatted(companyId, kind, code, name));
    }

    private static void insertDefinitionRevision(
            UUID companyId, String code, long version, String name) throws SQLException {
        execute("""
                INSERT INTO plg_business_partners.business_partner_definition_revision
                    (company_id, definition_kind, code, version, display_name, state)
                VALUES ('%s', 'CHANNEL_KIND', '%s', %d, '%s', 'ACTIVE')
                """.formatted(companyId, code, version, name));
    }

    private static void assertSqlState(String expected, SqlOperation operation) {
        SQLException failure = assertThrows(SQLException.class, operation::execute);
        assertEquals(expected, failure.getSQLState());
    }

    private static void execute(String sql) throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static int queryInt(String sql) throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static String queryString(String sql) throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    @FunctionalInterface
    private interface SqlOperation {
        void execute() throws SQLException;
    }
}
