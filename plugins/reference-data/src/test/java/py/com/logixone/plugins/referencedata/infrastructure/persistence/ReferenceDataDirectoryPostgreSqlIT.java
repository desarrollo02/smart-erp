package py.com.logixone.plugins.referencedata.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.referencedata.api.CatalogCompleteness;
import py.com.logixone.plugins.referencedata.api.CountryCode;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;

@Testcontainers
class ReferenceDataDirectoryPostgreSqlIT {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse(
                    "postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("logixone_reference_data_test")
            .withUsername("logixone_test")
            .withPassword("test-only-password");

    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    static void migrateAndOpenPersistenceUnit() {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(ReferenceDataPersistenceNames.SCHEMA)
                .defaultSchema(ReferenceDataPersistenceNames.SCHEMA)
                .table("flyway_schema_history")
                .locations("classpath:db/migration/reference_data")
                .createSchemas(true)
                .cleanDisabled(true)
                .load();
        assertEquals(1, flyway.migrate().migrationsExecuted);
        assertEquals(0, flyway.migrate().migrationsExecuted);
        flyway.validate();

        entityManagerFactory = Persistence.createEntityManagerFactory(
                ReferenceDataPersistenceNames.TEST_UNIT_NAME,
                Map.of(
                        "jakarta.persistence.jdbc.url", POSTGRES.getJdbcUrl(),
                        "jakarta.persistence.jdbc.user", POSTGRES.getUsername(),
                        "jakarta.persistence.jdbc.password", POSTGRES.getPassword(),
                        "jakarta.persistence.jdbc.driver", "org.postgresql.Driver"));
    }

    @AfterAll
    static void closePersistenceUnit() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @Test
    void createsFivePrivateTablesAndSeededProvenance() throws SQLException {
        assertEquals(5, queryInt("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'plg_reference_data'
                  AND table_name <> 'flyway_schema_history'
                """));
        assertEquals(1, queryInt("""
                SELECT count(*) FROM plg_reference_data.flyway_schema_history
                WHERE success AND version = '1'
                """));

        var entityManager = entityManagerFactory.createEntityManager();
        try {
            var directory = new JpaReferenceDataDirectory(entityManager);
            CompanyId company = new CompanyId(UUID.randomUUID());
            var countries = directory.currentRelease(company, ReferenceDataCatalog.COUNTRY);
            var currencies = directory.currentRelease(company, ReferenceDataCatalog.CURRENCY);

            assertEquals(CatalogCompleteness.BOOTSTRAP_SUBSET, countries.completeness());
            assertEquals(1, countries.entryCount());
            assertEquals(
                    "748f6ff7380c8a50ea9448f068b79e3a1ee31be63207249e8cc89bf1eb969d11",
                    countries.sourceSha256());
            assertEquals(CatalogCompleteness.BOOTSTRAP_SUBSET, currencies.completeness());
            assertEquals(2, currencies.entryCount());
            assertEquals(
                    "838dfb991648cf36df939edd5fe3811737962b75a32252847d239cedd1e291c9",
                    currencies.sourceSha256());
            assertEquals("PRY", directory.findCountry(company, new CountryCode("PY"))
                    .orElseThrow().alpha3Code());
            assertEquals(0, directory.findCurrency(company, new CurrencyCode("PYG"))
                    .orElseThrow().minorUnit());
            assertEquals(2, directory.findCurrency(company, new CurrencyCode("USD"))
                    .orElseThrow().minorUnit());
        } finally {
            entityManager.close();
        }
    }

    @Test
    void appliesCompanyOverridesWithoutChangingAnotherCompany() throws SQLException {
        CompanyId restricted = new CompanyId(UUID.randomUUID());
        CompanyId unaffected = new CompanyId(UUID.randomUUID());
        execute("""
                INSERT INTO plg_reference_data.company_country_policy
                    (company_id, alpha2_code, enabled)
                VALUES ('%s', 'PY', false);
                INSERT INTO plg_reference_data.company_currency_policy
                    (company_id, alphabetic_code, enabled)
                VALUES ('%s', 'USD', false)
                """.formatted(restricted.value(), restricted.value()));

        var entityManager = entityManagerFactory.createEntityManager();
        try {
            var directory = new JpaReferenceDataDirectory(entityManager);
            assertFalse(directory.findCountry(restricted, new CountryCode("PY"))
                    .orElseThrow().enabled());
            assertFalse(directory.findCurrency(restricted, new CurrencyCode("USD"))
                    .orElseThrow().enabled());
            assertTrue(directory.findCountry(unaffected, new CountryCode("PY"))
                    .orElseThrow().enabled());
            assertTrue(directory.findCurrency(unaffected, new CurrencyCode("USD"))
                    .orElseThrow().enabled());
        } finally {
            entityManager.close();
        }
    }

    private static int queryInt(String sql) throws SQLException {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement();
                var result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static void execute(String sql) throws SQLException {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
