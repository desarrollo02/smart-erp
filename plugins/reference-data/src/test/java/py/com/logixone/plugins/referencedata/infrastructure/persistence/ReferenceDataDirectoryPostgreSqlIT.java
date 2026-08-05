package py.com.logixone.plugins.referencedata.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
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
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.referencedata.api.CatalogCompleteness;
import py.com.logixone.plugins.referencedata.api.CountryCode;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;
import py.com.logixone.plugins.referencedata.api.ReferenceDataQuery;
import py.com.logixone.plugins.referencedata.application.policy.ChangeReferenceDataPolicy;

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
        assertEquals(4, flyway.migrate().migrationsExecuted);
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
    void createsSixPrivateTablesAndSeededProvenance() throws SQLException {
        assertEquals(6, queryInt("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'plg_reference_data'
                  AND table_name <> 'flyway_schema_history'
                """));
        assertEquals(1, queryInt("""
                SELECT count(*) FROM plg_reference_data.flyway_schema_history
                WHERE success AND version = '1'
                """));
        assertEquals(1, queryInt("""
                SELECT count(*) FROM plg_reference_data.flyway_schema_history
                WHERE success AND version = '2'
                """));
        assertEquals(1, queryInt("""
                SELECT count(*) FROM plg_reference_data.flyway_schema_history
                WHERE success AND version = '3'
                """));
        assertEquals(1, queryInt("""
                SELECT count(*) FROM plg_reference_data.flyway_schema_history
                WHERE success AND version = '4'
                """));
        assertEquals(4, queryInt("""
                SELECT count(*) FROM plg_reference_data.catalog_release
                """));
        assertEquals(2, queryInt("""
                SELECT count(*) FROM plg_reference_data.catalog_release
                WHERE current_release
                """));

        var entityManager = entityManagerFactory.createEntityManager();
        try {
            var directory = new JpaReferenceDataDirectory(entityManager);
            CompanyId company = new CompanyId(UUID.randomUUID());
            var countries = directory.currentRelease(company, ReferenceDataCatalog.COUNTRY);
            var currencies = directory.currentRelease(company, ReferenceDataCatalog.CURRENCY);

            assertEquals(CatalogCompleteness.FULL, countries.completeness());
            assertEquals(248, countries.entryCount());
            assertEquals(
                    "748f6ff7380c8a50ea9448f068b79e3a1ee31be63207249e8cc89bf1eb969d11",
                    countries.sourceSha256());
            assertEquals(CatalogCompleteness.FULL, currencies.completeness());
            assertEquals(178, currencies.entryCount());
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

    @Test
    void changesPoliciesOptimisticallyAndKeepsAppendOnlyCompanyHistory() {
        CompanyId company = new CompanyId(UUID.randomUUID());
        AppUserId actor = new AppUserId(UUID.randomUUID());
        Instant changedAt = Instant.parse("2026-08-05T15:00:00Z");
        var entityManager = entityManagerFactory.createEntityManager();
        var transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            var repository = new JpaReferenceDataPolicyRepository(entityManager);
            assertTrue(repository.existsInCurrentRelease(ReferenceDataCatalog.COUNTRY, "PY"));
            assertFalse(repository.existsInCurrentRelease(ReferenceDataCatalog.COUNTRY, "AA"));
            assertTrue(repository.find(company, ReferenceDataCatalog.COUNTRY, "PY").isEmpty());

            var disabled = repository.change(
                    company,
                    new ChangeReferenceDataPolicy(ReferenceDataCatalog.COUNTRY, "PY", false, 0),
                    actor,
                    "rd04-postgresql",
                    changedAt);
            var enabled = repository.change(
                    company,
                    new ChangeReferenceDataPolicy(ReferenceDataCatalog.COUNTRY, "PY", true, 1),
                    actor,
                    "rd04-postgresql",
                    changedAt.plusSeconds(1));
            transaction.commit();

            assertEquals(1, disabled.version());
            assertEquals(2, enabled.version());
            assertTrue(repository.find(company, ReferenceDataCatalog.COUNTRY, "PY")
                    .orElseThrow().enabled());
            var history = repository.history(company, ReferenceDataCatalog.COUNTRY, "PY");
            assertEquals(List.of(2L, 1L), history.stream()
                    .map(value -> value.version()).toList());
            assertEquals(List.of(true, false), history.stream()
                    .map(value -> value.enabled()).toList());
            assertEquals(actor, history.getFirst().actorUserId());
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
    }

    @Test
    void readsNotApplicableMinorUnitAsExplicitAbsence() throws SQLException {
        var entityManager = entityManagerFactory.createEntityManager();
        try {
            var directory = new JpaReferenceDataDirectory(entityManager);
            var xdr = directory.findCurrency(
                    new CompanyId(UUID.randomUUID()), new CurrencyCode("XDR"))
                    .orElseThrow();
            assertTrue(xdr.minorUnitIfDefined().isEmpty());
        } finally {
            entityManager.close();
        }
    }

    @Test
    void searchesCurrentPublicationsOnTheServerWithPolicyAndPageBoundaries()
            throws SQLException {
        CompanyId company = new CompanyId(UUID.randomUUID());
        execute("""
                INSERT INTO plg_reference_data.company_currency_policy
                    (company_id, alphabetic_code, enabled)
                VALUES ('%s', 'USD', false)
                """.formatted(company.value()));

        var entityManager = entityManagerFactory.createEntityManager();
        try {
            var directory = new JpaReferenceDataDirectory(entityManager);
            var countries = directory.searchCountries(
                    company, new ReferenceDataQuery("  PARA  ", 0, 50, false));
            var disabledExcluded = directory.searchCurrencies(
                    company, new ReferenceDataQuery("usd", 0, 50, true));
            var disabledVisible = directory.searchCurrencies(
                    company, new ReferenceDataQuery("usd", 0, 50, false));
            var firstCurrencyPage = directory.searchCurrencies(
                    company, new ReferenceDataQuery("", 0, 50, false));
            var secondCurrencyPage = directory.searchCurrencies(
                    company, new ReferenceDataQuery("", 50, 50, false));

            assertEquals(1, countries.total());
            assertEquals("PY", countries.entries().getFirst().code().value());
            assertEquals(0, disabledExcluded.total());
            assertEquals(1, disabledVisible.total());
            assertFalse(disabledVisible.entries().getFirst().enabled());
            assertEquals(178, firstCurrencyPage.total());
            assertEquals(50, firstCurrencyPage.entries().size());
            assertEquals(50, secondCurrencyPage.entries().size());
            assertEquals(50, secondCurrencyPage.offset());
            assertFalse(firstCurrencyPage.entries().getFirst().code()
                    .equals(secondCurrencyPage.entries().getFirst().code()));
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
