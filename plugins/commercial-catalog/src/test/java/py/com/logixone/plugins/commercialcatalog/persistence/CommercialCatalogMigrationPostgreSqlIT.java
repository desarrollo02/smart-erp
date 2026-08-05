package py.com.logixone.plugins.commercialcatalog.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class CommercialCatalogMigrationPostgreSqlIT {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse(
                    "postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("logixone_commercial_catalog_test")
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
    void createsTwentySixPrivateTablesAndIndependentHistory() throws SQLException {
        assertEquals(26, queryInt("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'plg_commercial_catalog'
                  AND table_name <> 'flyway_schema_history'
                """));
        assertEquals(4, queryInt("""
                SELECT count(*) FROM plg_commercial_catalog.flyway_schema_history
                WHERE success AND version IN ('1', '2', '3', '4')
                """));
    }

    @Test
    void preservesVariantAssignmentsAgainstTheExactAppendOnlyFamilyRevision()
            throws SQLException {
        UUID company = UUID.randomUUID();
        seedDefinitions(company);
        UUID item = insertItem(company, "SKU-VARIANT-HISTORY");
        UUID family = UUID.randomUUID();
        execute("""
                INSERT INTO plg_commercial_catalog.variant_family
                    (company_id, variant_family_id, code, display_name, state, version)
                VALUES ('%s', '%s', 'CLOTHING', 'Clothing', 'ACTIVE', 0)
                """.formatted(company, family));
        execute("""
                INSERT INTO plg_commercial_catalog.variant_attribute_definition
                    (company_id, variant_family_id, attribute_code, display_name,
                     value_type, required, position)
                VALUES ('%s', '%s', 'COLOR', 'Color', 'TEXT', true, 0)
                """.formatted(company, family));
        execute("""
                INSERT INTO plg_commercial_catalog.variant_family_revision
                    (company_id, variant_family_id, family_version, display_name, state)
                VALUES ('%s', '%s', 0, 'Clothing', 'ACTIVE')
                """.formatted(company, family));
        execute("""
                INSERT INTO plg_commercial_catalog.variant_attribute_revision
                    (company_id, variant_family_id, family_version, attribute_code,
                     display_name, value_type, required, position)
                VALUES ('%s', '%s', 0, 'COLOR', 'Color', 'TEXT', true, 0)
                """.formatted(company, family));
        execute("""
                INSERT INTO plg_commercial_catalog.catalog_item_variant
                    (company_id, catalog_item_id, variant_family_id, variant_family_version)
                VALUES ('%s', '%s', '%s', 0)
                """.formatted(company, item, family));
        execute("""
                INSERT INTO plg_commercial_catalog.catalog_item_variant_attribute
                    (company_id, catalog_item_id, variant_family_id, variant_family_version,
                     attribute_code, value_type, attribute_value)
                VALUES ('%s', '%s', '%s', 0, 'COLOR', 'TEXT', 'Blue')
                """.formatted(company, item, family));

        execute("""
                UPDATE plg_commercial_catalog.variant_family
                SET display_name = 'Clothing by size', version = 1
                WHERE company_id = '%s' AND variant_family_id = '%s'
                """.formatted(company, family));
        execute("""
                INSERT INTO plg_commercial_catalog.variant_family_revision
                    (company_id, variant_family_id, family_version, display_name, state)
                VALUES ('%s', '%s', 1, 'Clothing by size', 'ACTIVE')
                """.formatted(company, family));
        execute("""
                INSERT INTO plg_commercial_catalog.variant_attribute_revision
                    (company_id, variant_family_id, family_version, attribute_code,
                     display_name, value_type, required, position)
                VALUES ('%s', '%s', 1, 'SIZE', 'Size', 'NUMBER', true, 0)
                """.formatted(company, family));

        assertEquals(1, queryInt("""
                SELECT count(*)
                FROM plg_commercial_catalog.catalog_item_variant assignment
                JOIN plg_commercial_catalog.variant_family_revision revision
                  ON revision.company_id = assignment.company_id
                 AND revision.variant_family_id = assignment.variant_family_id
                 AND revision.family_version = assignment.variant_family_version
                JOIN plg_commercial_catalog.catalog_item_variant_attribute value
                  ON value.company_id = assignment.company_id
                 AND value.catalog_item_id = assignment.catalog_item_id
                WHERE assignment.company_id = '%s'
                  AND assignment.catalog_item_id = '%s'
                  AND assignment.variant_family_version = 0
                  AND revision.display_name = 'Clothing'
                  AND value.attribute_code = 'COLOR'
                """.formatted(company, item)));
        assertSqlState("23503", () -> execute("""
                UPDATE plg_commercial_catalog.catalog_item_variant
                SET variant_family_version = 2
                WHERE company_id = '%s' AND catalog_item_id = '%s'
                """.formatted(company, item)));
    }

    @Test
    void backfillsAV3FamilyAndItsAssignmentWhenV4IsApplied() throws SQLException {
        String database = "logixone_variant_backfill";
        execute("CREATE DATABASE " + database);
        String jdbcUrl = POSTGRES.getJdbcUrl().replace(
                "logixone_commercial_catalog_test", database);
        Flyway.configure()
                .dataSource(jdbcUrl, POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("plg_commercial_catalog")
                .defaultSchema("plg_commercial_catalog")
                .table("flyway_schema_history")
                .locations("classpath:db/migration/commercial_catalog")
                .createSchemas(true)
                .cleanDisabled(true)
                .target(MigrationVersion.fromVersion("3"))
                .load()
                .migrate();
        UUID company = UUID.randomUUID();
        UUID profile = UUID.randomUUID();
        UUID family = UUID.randomUUID();
        UUID item = UUID.randomUUID();
        execute(jdbcUrl, """
                INSERT INTO plg_commercial_catalog.unit_definition
                    (company_id, unit_code, display_name, decimal_scale, state)
                VALUES ('%s', 'EA', 'Each', 0, 'ACTIVE');
                INSERT INTO plg_commercial_catalog.tax_profile
                    (company_id, tax_profile_id, code, display_name, state)
                VALUES ('%s', '%s', 'GENERAL', 'General tax', 'ACTIVE');
                INSERT INTO plg_commercial_catalog.tax_profile_revision
                    (company_id, tax_profile_id, profile_version, internal_kind_code,
                     description, valid_from, active)
                VALUES ('%s', '%s', 0, 'GENERAL', 'General',
                        TIMESTAMPTZ '2026-01-01 00:00:00Z', true);
                INSERT INTO plg_commercial_catalog.variant_family
                    (company_id, variant_family_id, code, display_name, state, version)
                VALUES ('%s', '%s', 'APPAREL', 'Apparel', 'ACTIVE', 5);
                INSERT INTO plg_commercial_catalog.variant_attribute_definition
                    (company_id, variant_family_id, attribute_code, display_name,
                     value_type, required, position)
                VALUES ('%s', '%s', 'COLOR', 'Color', 'TEXT', true, 0);
                INSERT INTO plg_commercial_catalog.catalog_item
                    (company_id, catalog_item_id, code, display_name, description,
                     item_type, state, base_unit_code, tax_profile_id, tax_profile_version)
                VALUES ('%s', '%s', 'SKU-BACKFILL', 'Backfill', '', 'PRODUCT',
                        'ACTIVE', 'EA', '%s', 0);
                INSERT INTO plg_commercial_catalog.catalog_item_variant
                    (company_id, catalog_item_id, variant_family_id)
                VALUES ('%s', '%s', '%s');
                INSERT INTO plg_commercial_catalog.catalog_item_variant_attribute
                    (company_id, catalog_item_id, variant_family_id,
                     attribute_code, value_type, attribute_value)
                VALUES ('%s', '%s', '%s', 'COLOR', 'TEXT', 'Blue')
                """.formatted(
                        company,
                        company, profile,
                        company, profile,
                        company, family,
                        company, family,
                        company, item, profile,
                        company, item, family,
                        company, item, family));

        MigrateResult migration = flyway(jdbcUrl).migrate();
        MigrateResult repeated = flyway(jdbcUrl).migrate();

        assertEquals(1, migration.migrationsExecuted);
        assertEquals(0, repeated.migrationsExecuted);
        assertEquals(5, queryInt(jdbcUrl, """
                SELECT variant_family_version
                FROM plg_commercial_catalog.catalog_item_variant
                WHERE company_id = '%s' AND catalog_item_id = '%s'
                """.formatted(company, item)));
        assertEquals(1, queryInt(jdbcUrl, """
                SELECT count(*)
                FROM plg_commercial_catalog.variant_family_revision
                WHERE company_id = '%s' AND variant_family_id = '%s'
                  AND family_version = 5 AND display_name = 'Apparel'
                """.formatted(company, family)));
        assertEquals(1, queryInt(jdbcUrl, """
                SELECT count(*)
                FROM plg_commercial_catalog.variant_attribute_revision
                WHERE company_id = '%s' AND variant_family_id = '%s'
                  AND family_version = 5 AND attribute_code = 'COLOR'
                """.formatted(company, family)));
    }

    @Test
    void linksReplacementsInsideTheCompanyAndPreservesExistingReferences()
            throws SQLException {
        UUID companyA = UUID.randomUUID();
        UUID companyB = UUID.randomUUID();
        seedDefinitions(companyA);
        seedDefinitions(companyB);
        UUID item = insertItem(companyA, "SKU-REPLACEMENT");
        execute("""
                INSERT INTO plg_commercial_catalog.unit_definition
                    (company_id, unit_code, display_name, decimal_scale, state)
                VALUES ('%s', 'EA2', 'Replacement each', 0, 'ACTIVE')
                """.formatted(companyA));
        execute("""
                UPDATE plg_commercial_catalog.unit_definition
                SET state = 'INACTIVE', replacement_unit_code = 'EA2', version = version + 1
                WHERE company_id = '%s' AND unit_code = 'EA'
                """.formatted(companyA));

        assertEquals(1, queryInt("""
                SELECT count(*) FROM plg_commercial_catalog.catalog_item
                WHERE company_id = '%s' AND catalog_item_id = '%s' AND base_unit_code = 'EA'
                """.formatted(companyA, item)));
        assertEquals(1, queryInt("""
                SELECT count(*) FROM plg_commercial_catalog.unit_definition
                WHERE company_id = '%s' AND unit_code = 'EA'
                  AND state = 'INACTIVE' AND replacement_unit_code = 'EA2'
                """.formatted(companyA)));
        assertSqlState("23514", () -> execute("""
                UPDATE plg_commercial_catalog.unit_definition
                SET state = 'ACTIVE'
                WHERE company_id = '%s' AND unit_code = 'EA'
                """.formatted(companyA)));

        execute("""
                INSERT INTO plg_commercial_catalog.unit_definition
                    (company_id, unit_code, display_name, decimal_scale, state)
                VALUES ('%s', 'SELF', 'Self', 0, 'ACTIVE'),
                       ('%s', 'ONLYB', 'Only B', 0, 'ACTIVE')
                """.formatted(companyA, companyB));
        assertSqlState("23514", () -> execute("""
                UPDATE plg_commercial_catalog.unit_definition
                SET state = 'INACTIVE', replacement_unit_code = 'SELF'
                WHERE company_id = '%s' AND unit_code = 'SELF'
                """.formatted(companyA)));
        assertSqlState("23503", () -> execute("""
                UPDATE plg_commercial_catalog.unit_definition
                SET state = 'INACTIVE', replacement_unit_code = 'ONLYB'
                WHERE company_id = '%s' AND unit_code = 'SELF'
                """.formatted(companyA)));
    }

    @Test
    void scopesItemCodesAndActiveIdentifiersByCompany() throws SQLException {
        UUID companyA = UUID.randomUUID();
        UUID companyB = UUID.randomUUID();
        seedDefinitions(companyA);
        seedDefinitions(companyB);
        UUID itemA = insertItem(companyA, "SKU-SHARED");
        insertItem(companyB, "SKU-SHARED");

        assertSqlState("23505", () -> insertItem(companyA, "SKU-SHARED"));
        insertIdentifier(companyA, itemA, "EAN", "7840001", true);
        UUID itemA2 = insertItem(companyA, "SKU-SECOND");
        assertSqlState("23505", () -> insertIdentifier(companyA, itemA2, "EAN", "7840001", true));
        insertIdentifier(companyA, itemA2, "EAN", "7840001", false);

        assertSqlState("23503", () -> execute("""
                INSERT INTO plg_commercial_catalog.catalog_item_category
                    (company_id, catalog_item_id, category_id, is_primary)
                VALUES ('%s', '%s', '%s', true)
                """.formatted(companyB, itemA, definitionId(companyA, "category"))));
    }

    @Test
    void permitsOnlyOneDefaultUnitForEachItemPurpose() throws SQLException {
        UUID company = UUID.randomUUID();
        seedDefinitions(company);
        UUID item = insertItem(company, "SKU-UNIT");
        insertConversion(company, item, "BOX", "12");
        insertConversion(company, item, "PACK", "6");
        insertPurpose(company, item, "BOX", "SALE", true);

        assertSqlState("23505", () -> insertPurpose(company, item, "PACK", "SALE", true));
        insertPurpose(company, item, "PACK", "PURCHASE", true);
    }

    @Test
    void rejectsOverlappingTaxRevisionsAndAllowsAdjacentPeriods() throws SQLException {
        UUID company = UUID.randomUUID();
        UUID profile = UUID.randomUUID();
        execute("""
                INSERT INTO plg_commercial_catalog.tax_profile
                    (company_id, tax_profile_id, code, display_name, state)
                VALUES ('%s', '%s', 'VAT', 'Internal VAT', 'ACTIVE')
                """.formatted(company, profile));
        insertTaxRevision(company, profile, 0, "2026-01-01T00:00:00Z", "2026-02-01T00:00:00Z");

        assertSqlState("23505", () -> insertTaxRevision(
                company, profile, 1, "2026-01-15T00:00:00Z", "2026-03-01T00:00:00Z"));
        insertTaxRevision(company, profile, 2, "2026-02-01T00:00:00Z", "2026-03-01T00:00:00Z");
    }

    @Test
    void rejectsAmbiguousPriceValidityAndPreservesRowsOnInactivation() throws SQLException {
        UUID company = UUID.randomUUID();
        seedDefinitions(company);
        UUID item = insertItem(company, "SKU-PRICE");
        UUID list = insertPriceList(company, "RETAIL");
        insertPrice(company, list, item, UUID.randomUUID(), "1", "100", JANUARY, FEBRUARY, true);

        assertSqlState("23505", () -> insertPrice(
                company, list, item, UUID.randomUUID(), "1", "110",
                "2026-01-15T00:00:00Z", "2026-03-01T00:00:00Z", true));
        insertPrice(company, list, item, UUID.randomUUID(), "1", "120", FEBRUARY, MARCH, true);
        insertPrice(company, list, item, UUID.randomUUID(), "10", "90", JANUARY, MARCH, true);
        assertSqlState("23514", () -> insertPrice(
                company, list, item, UUID.randomUUID(), "1", "-1", MARCH, null, true));

        execute("""
                UPDATE plg_commercial_catalog.price_list SET state = 'INACTIVE', version = version + 1
                WHERE company_id = '%s' AND price_list_id = '%s'
                """.formatted(company, list));
        assertEquals(3, queryInt("""
                SELECT count(*) FROM plg_commercial_catalog.price_entry
                WHERE company_id = '%s' AND price_list_id = '%s'
                """.formatted(company, list)));
    }

    private static final String JANUARY = "2026-01-01T00:00:00Z";
    private static final String FEBRUARY = "2026-02-01T00:00:00Z";
    private static final String MARCH = "2026-03-01T00:00:00Z";

    private static Flyway flyway() {
        return flyway(POSTGRES.getJdbcUrl());
    }

    private static Flyway flyway(String jdbcUrl) {
        return Flyway.configure()
                .dataSource(jdbcUrl, POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("plg_commercial_catalog")
                .defaultSchema("plg_commercial_catalog")
                .table("flyway_schema_history")
                .locations("classpath:db/migration/commercial_catalog")
                .createSchemas(true)
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .validateOnMigrate(true)
                .failOnMissingLocations(true)
                .outOfOrder(false)
                .load();
    }

    private static void seedDefinitions(UUID company) throws SQLException {
        UUID category = definitionId(company, "category");
        UUID taxProfile = definitionId(company, "tax");
        execute("""
                INSERT INTO plg_commercial_catalog.unit_definition
                    (company_id, unit_code, display_name, decimal_scale, state)
                VALUES ('%1$s', 'EA', 'Each', 0, 'ACTIVE'),
                       ('%1$s', 'BOX', 'Box', 0, 'ACTIVE'),
                       ('%1$s', 'PACK', 'Pack', 0, 'ACTIVE')
                """.formatted(company));
        execute("""
                INSERT INTO plg_commercial_catalog.category_definition
                    (company_id, category_id, code, display_name, state)
                VALUES ('%s', '%s', 'GENERAL', 'General', 'ACTIVE')
                """.formatted(company, category));
        execute("""
                INSERT INTO plg_commercial_catalog.tax_profile
                    (company_id, tax_profile_id, code, display_name, state)
                VALUES ('%s', '%s', 'GENERAL', 'General tax', 'ACTIVE')
                """.formatted(company, taxProfile));
        insertTaxRevision(company, taxProfile, 0, "2020-01-01T00:00:00Z", null);
    }

    private static UUID insertItem(UUID company, String code) throws SQLException {
        UUID item = UUID.randomUUID();
        execute("""
                INSERT INTO plg_commercial_catalog.catalog_item
                    (company_id, catalog_item_id, code, display_name, description,
                     item_type, state, base_unit_code, tax_profile_id, tax_profile_version)
                VALUES ('%s', '%s', '%s', 'Test item', '', 'PRODUCT', 'ACTIVE',
                        'EA', '%s', 0)
                """.formatted(company, item, code, definitionId(company, "tax")));
        execute("""
                INSERT INTO plg_commercial_catalog.catalog_item_scope
                    (company_id, catalog_item_id, scope_code)
                VALUES ('%s', '%s', 'SALE')
                """.formatted(company, item));
        return item;
    }

    private static void insertIdentifier(
            UUID company, UUID item, String type, String normalized, boolean active) throws SQLException {
        execute("""
                INSERT INTO plg_commercial_catalog.catalog_item_identifier
                    (company_id, catalog_item_id, identifier_id, type_code,
                     presented_value, normalized_value, active)
                VALUES ('%s', '%s', '%s', '%s', '%s', '%s', %s)
                """.formatted(company, item, UUID.randomUUID(), type, normalized, normalized, active));
    }

    private static void insertConversion(UUID company, UUID item, String unit, String factor)
            throws SQLException {
        execute("""
                INSERT INTO plg_commercial_catalog.catalog_item_unit_conversion
                    (company_id, catalog_item_id, unit_code, to_base_factor, active)
                VALUES ('%s', '%s', '%s', %s, true)
                """.formatted(company, item, unit, factor));
    }

    private static void insertPurpose(
            UUID company, UUID item, String unit, String purpose, boolean isDefault) throws SQLException {
        execute("""
                INSERT INTO plg_commercial_catalog.catalog_item_unit_purpose
                    (company_id, catalog_item_id, unit_code, purpose_code, is_default)
                VALUES ('%s', '%s', '%s', '%s', %s)
                """.formatted(company, item, unit, purpose, isDefault));
    }

    private static void insertTaxRevision(
            UUID company, UUID profile, long version, String from, String until) throws SQLException {
        String untilValue = until == null ? "NULL" : "'%s'".formatted(Instant.parse(until));
        execute("""
                INSERT INTO plg_commercial_catalog.tax_profile_revision
                    (company_id, tax_profile_id, profile_version, internal_kind_code,
                     description, valid_from, valid_until, active)
                VALUES ('%s', '%s', %d, 'GENERAL', 'Internal profile', '%s', %s, true)
                """.formatted(company, profile, version, Instant.parse(from), untilValue));
    }

    private static UUID insertPriceList(UUID company, String code) throws SQLException {
        UUID list = UUID.randomUUID();
        execute("""
                INSERT INTO plg_commercial_catalog.price_list
                    (company_id, price_list_id, code, display_name, currency_code,
                     tax_mode, amount_scale, rounding_mode, state)
                VALUES ('%s', '%s', '%s', 'Retail', 'PYG', 'TAX_INCLUDED', 0, 'HALF_UP', 'ACTIVE')
                """.formatted(company, list, code));
        return list;
    }

    private static void insertPrice(
            UUID company, UUID list, UUID item, UUID entry, String minimum, String amount,
            String from, String until, boolean active) throws SQLException {
        String untilValue = until == null ? "NULL" : "'%s'".formatted(Instant.parse(until));
        execute("""
                INSERT INTO plg_commercial_catalog.price_entry
                    (company_id, price_list_id, price_entry_id, catalog_item_id, unit_code,
                     minimum_quantity, amount, valid_from, valid_until, active)
                VALUES ('%s', '%s', '%s', '%s', 'EA', %s, %s, '%s', %s, %s)
                """.formatted(company, list, entry, item, minimum, amount,
                        Instant.parse(from), untilValue, active));
    }

    private static UUID definitionId(UUID company, String kind) {
        return UUID.nameUUIDFromBytes((company + ":" + kind).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static void assertSqlState(String expected, SqlOperation operation) {
        SQLException failure = assertThrows(SQLException.class, operation::execute);
        assertEquals(expected, failure.getSQLState());
    }

    private static void execute(String sql) throws SQLException {
        execute(POSTGRES.getJdbcUrl(), sql);
    }

    private static void execute(String jdbcUrl, String sql) throws SQLException {
        try (Connection connection = connection(jdbcUrl);
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static int queryInt(String sql) throws SQLException {
        return queryInt(POSTGRES.getJdbcUrl(), sql);
    }

    private static int queryInt(String jdbcUrl, String sql) throws SQLException {
        try (Connection connection = connection(jdbcUrl);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static Connection connection() throws SQLException {
        return connection(POSTGRES.getJdbcUrl());
    }

    private static Connection connection(String jdbcUrl) throws SQLException {
        return DriverManager.getConnection(
                jdbcUrl, POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    @FunctionalInterface
    private interface SqlOperation {
        void execute() throws SQLException;
    }
}
