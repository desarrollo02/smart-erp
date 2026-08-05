package py.com.logixone.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import py.com.logixone.plugin.api.MigrationContribution;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

@Testcontainers
class CoreMigrationPostgreSqlIT {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse(
                    "postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0")
            .asCompatibleSubstituteFor("postgres");
    private static final String COMPANY_A = "00000000-0000-0000-0000-000000000001";
    private static final String COMPANY_B = "00000000-0000-0000-0000-000000000002";

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("postgres")
            .withUsername("logixone_test")
            .withPassword("test-only-password");

    @TempDir
    Path temporaryDirectory;

    @Test
    void emptyDatabaseAppliesAllMigrationsAndSecondRunIsIdempotent() throws Exception {
        String jdbcUrl = createDatabase("empty");
        MigratorConfiguration configuration = configuration(jdbcUrl);

        MigrationOutcome first = coreExecutor().migrate(configuration);
        MigrationOutcome second = coreExecutor().migrate(configuration);

        assertEquals(6, first.migrationsExecuted());
        assertEquals("6", first.schemaVersion());
        assertEquals(0, second.migrationsExecuted());
        assertEquals("6", second.schemaVersion());
        assertEquals(6, queryInt(jdbcUrl, "SELECT count(*) FROM core.flyway_schema_history "
                + "WHERE success AND version IS NOT NULL"));
        assertCoreTables(jdbcUrl);
        assertEquals(0, queryInt(jdbcUrl, "SELECT count(*) FROM core.audit_event"));
    }

    @Test
    void databaseAtV1AppliesV2ThroughV6AndConvergesWithAnEmptyDatabase() throws Exception {
        String upgradedUrl = createDatabase("upgrade");
        MigrateResult v1 = flyway(upgradedUrl, "classpath:db/migration/core", MigrationVersion.fromVersion("1"))
                .migrate();
        int v1Checksum = queryInt(
                upgradedUrl,
                "SELECT checksum FROM core.flyway_schema_history WHERE version = '1' AND success");

        MigrationOutcome upgrade = coreExecutor().migrate(configuration(upgradedUrl));
        int checksumAfterUpgrade = queryInt(
                upgradedUrl,
                "SELECT checksum FROM core.flyway_schema_history WHERE version = '1' AND success");

        String emptyUrl = createDatabase("comparison");
        coreExecutor().migrate(configuration(emptyUrl));

        assertEquals(1, v1.migrationsExecuted);
        assertEquals(5, upgrade.migrationsExecuted());
        assertEquals("6", upgrade.schemaVersion());
        assertEquals(v1Checksum, checksumAfterUpgrade);
        assertEquals(schemaSignature(emptyUrl), schemaSignature(upgradedUrl));
    }

    @Test
    void databaseAtV2AppliesV3ThroughV6AndConvergesWithAnEmptyDatabase() throws Exception {
        String upgradedUrl = createDatabase("upgrade_v2");
        MigrateResult v2 = flyway(upgradedUrl, "classpath:db/migration/core", MigrationVersion.fromVersion("2"))
                .migrate();
        int v1Checksum = queryInt(
                upgradedUrl,
                "SELECT checksum FROM core.flyway_schema_history WHERE version = '1' AND success");
        int v2Checksum = queryInt(
                upgradedUrl,
                "SELECT checksum FROM core.flyway_schema_history WHERE version = '2' AND success");

        MigrationOutcome upgrade = coreExecutor().migrate(configuration(upgradedUrl));

        String emptyUrl = createDatabase("comparison_v2");
        coreExecutor().migrate(configuration(emptyUrl));

        assertEquals(2, v2.migrationsExecuted);
        assertEquals(4, upgrade.migrationsExecuted());
        assertEquals("6", upgrade.schemaVersion());
        assertEquals(v1Checksum, queryInt(
                upgradedUrl,
                "SELECT checksum FROM core.flyway_schema_history WHERE version = '1' AND success"));
        assertEquals(v2Checksum, queryInt(
                upgradedUrl,
                "SELECT checksum FROM core.flyway_schema_history WHERE version = '2' AND success"));
        assertEquals(schemaSignature(emptyUrl), schemaSignature(upgradedUrl));
    }

    @Test
    void databasesAtV3AndV4ConvergeWithAnEmptyDatabase() throws Exception {
        assertUpgradeConverges("3", 3, "upgrade_v3");
        assertUpgradeConverges("4", 2, "upgrade_v4");
        assertUpgradeConverges("5", 1, "upgrade_v5");
    }

    @Test
    void changingAnyAppliedMigrationFailsChecksumValidation() throws Exception {
        assertChangedMigrationRejected("V1__initialize_core_schema.sql");
        assertChangedMigrationRejected("V2__add_companies_and_plugin_activation.sql");
        assertChangedMigrationRejected("V3__add_identity_membership_and_authorization.sql");
        assertChangedMigrationRejected("V4__add_system_authority.sql");
        assertChangedMigrationRejected("V5__add_technical_audit_event.sql");
        assertChangedMigrationRejected("V6__extend_audit_for_plugin_operations.sql");
    }

    @Test
    void pluginOwnedSchemaMigratesAfterCoreAndIsIdempotent() throws Exception {
        String jdbcUrl = createDatabase("plugin_owned_schema");
        MigratorConfiguration configuration = configuration(jdbcUrl);
        FlywayMigrationExecutor executor = new FlywayMigrationExecutor(List.of(pluginFixture()));

        MigrationOutcome first = executor.migrate(configuration);
        MigrationOutcome second = executor.migrate(configuration);

        assertEquals(7, first.migrationsExecuted());
        assertEquals(List.of("core", "plg_plugin_fixture"),
                first.schemas().stream().map(SchemaMigrationOutcome::schema).toList());
        assertEquals(List.of(6, 1),
                first.schemas().stream().map(SchemaMigrationOutcome::migrationsExecuted).toList());
        assertEquals(0, second.migrationsExecuted());
        assertEquals(1, queryInt(jdbcUrl,
                "SELECT count(*) FROM plg_plugin_fixture.flyway_schema_history "
                        + "WHERE success AND version = '1'"));
        assertEquals(1, queryInt(jdbcUrl,
                "SELECT count(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'plg_plugin_fixture' AND table_name = 'migration_fixture'"));
    }

    @Test
    void globalAuthorityConstraintsPreserveTypedReferencesAndVersions() throws Exception {
        String jdbcUrl = migratedDatabase("system_authority_constraints");
        String userId = "00000000-0000-0000-0000-000000000201";
        String roleId = "00000000-0000-0000-0000-000000000101";

        execute(jdbcUrl, "INSERT INTO core.app_user "
                + "(app_user_id, issuer, subject, display_name, status) VALUES "
                + "('" + userId + "', 'https://identity.example.test/realms/logixone', "
                + "'administrator', 'Administrador', 'ACTIVE')");
        execute(jdbcUrl, "INSERT INTO core.system_role "
                + "(system_role_id, role_code, display_name, status) VALUES "
                + "('" + roleId + "', 'system.administrator', 'Administrador global', 'ACTIVE')");
        execute(jdbcUrl, "INSERT INTO core.system_role_permission "
                + "(system_role_id, permission_id) VALUES "
                + "('" + roleId + "', 'kernel.system_administration.manage')");
        execute(jdbcUrl, "INSERT INTO core.app_user_system_role "
                + "(app_user_id, system_role_id) VALUES ('" + userId + "', '" + roleId + "')");

        assertStatementRejected(jdbcUrl, "INSERT INTO core.system_role "
                + "(system_role_id, role_code, display_name, status) VALUES "
                + "('00000000-0000-0000-0000-000000000102', 'Invalid-Role', 'Inválido', 'ACTIVE')");
        assertStatementRejected(jdbcUrl, "INSERT INTO core.system_role "
                + "(system_role_id, role_code, display_name, status, version) VALUES "
                + "('00000000-0000-0000-0000-000000000103', 'system.invalid', 'Inválido', "
                + "'UNKNOWN', -1)");
        assertStatementRejected(jdbcUrl, "INSERT INTO core.system_role_permission "
                + "(system_role_id, permission_id) VALUES "
                + "('00000000-0000-0000-0000-000000000199', 'kernel.audit.view')");
        assertStatementRejected(jdbcUrl, "INSERT INTO core.app_user_system_role "
                + "(app_user_id, system_role_id) VALUES "
                + "('00000000-0000-0000-0000-000000000299', '" + roleId + "')");
        assertEquals(1, queryInt(jdbcUrl, "SELECT count(*) FROM core.system_role"));
        assertEquals(1, queryInt(jdbcUrl, "SELECT count(*) FROM core.system_role_permission"));
        assertEquals(1, queryInt(jdbcUrl, "SELECT count(*) FROM core.app_user_system_role"));
    }

    @Test
    void technicalAuditIsValidatedAppendOnlyAndStartsWithoutBackfill() throws Exception {
        String jdbcUrl = migratedDatabase("audit_append_only");
        String eventId = "00000000-0000-0000-0000-000000000501";
        assertEquals(0, queryInt(jdbcUrl, "SELECT count(*) FROM core.audit_event"));

        execute(jdbcUrl, "INSERT INTO core.audit_event "
                + "(audit_event_id, category, operation, outcome, actor_kind, correlation_id, occurred_at) "
                + "VALUES ('" + eventId + "', 'SYSTEM_AUTHORITY_ACCESS', 'AUTHORIZE', "
                + "'DENIED', 'UNRESOLVED', 'request-501', CURRENT_TIMESTAMP)");

        assertStatementRejected(jdbcUrl, "UPDATE core.audit_event SET outcome = 'ALLOWED' "
                + "WHERE audit_event_id = '" + eventId + "'");
        assertStatementRejected(jdbcUrl, "DELETE FROM core.audit_event "
                + "WHERE audit_event_id = '" + eventId + "'");
        assertStatementRejected(jdbcUrl, "INSERT INTO core.audit_event "
                + "(audit_event_id, category, operation, outcome, actor_kind, occurred_at) VALUES "
                + "('00000000-0000-0000-0000-000000000502', 'UNKNOWN', 'AUTHORIZE', "
                + "'DENIED', 'UNRESOLVED', CURRENT_TIMESTAMP)");
        assertStatementRejected(jdbcUrl, "INSERT INTO core.audit_event "
                + "(audit_event_id, category, operation, outcome, actor_kind, occurred_at) VALUES "
                + "('00000000-0000-0000-0000-000000000503', 'SYSTEM_AUTHORITY_ACCESS', "
                + "'AUTHORIZE', 'ALLOWED', 'AUTHENTICATED_USER', CURRENT_TIMESTAMP)");
        assertStatementRejected(jdbcUrl, "INSERT INTO core.audit_event "
                + "(audit_event_id, category, operation, outcome, actor_kind, correlation_id, occurred_at) "
                + "VALUES ('00000000-0000-0000-0000-000000000504', 'SYSTEM_AUTHORITY_ACCESS', "
                + "'AUTHORIZE', 'DENIED', 'UNRESOLVED', 'bad correlation', CURRENT_TIMESTAMP)");

        assertEquals(1, queryInt(jdbcUrl, "SELECT count(*) FROM core.audit_event"));
        assertEquals(6, queryInt(jdbcUrl, "SELECT count(*) FROM pg_indexes "
                + "WHERE schemaname = 'core' AND indexname LIKE 'audit_event_%_idx'"));
    }

    @Test
    void databaseConstraintsRejectInvalidCompaniesAndActivations() throws Exception {
        String jdbcUrl = migratedDatabase("constraints");

        assertStatementRejected(
                jdbcUrl,
                "INSERT INTO core.company (company_id, status, customization_plugin_id) "
                        + "VALUES ('" + COMPANY_A + "', 'UNKNOWN', 'custom_a')");
        assertStatementRejected(
                jdbcUrl,
                "INSERT INTO core.company (company_id, status, customization_plugin_id) "
                        + "VALUES ('" + COMPANY_A + "', 'INACTIVE', NULL)");
        assertStatementRejected(
                jdbcUrl,
                "INSERT INTO core.company (company_id, status, customization_plugin_id) "
                        + "VALUES ('" + COMPANY_A + "', 'INACTIVE', 'Invalid-Plugin')");
        assertStatementRejected(
                jdbcUrl,
                "INSERT INTO core.company (company_id, status, customization_plugin_id, version) "
                        + "VALUES ('" + COMPANY_A + "', 'INACTIVE', 'custom_a', -1)");
        assertStatementRejected(
                jdbcUrl,
                "INSERT INTO core.company_plugin_activation (company_id, plugin_id, desired_state) "
                        + "VALUES ('" + COMPANY_A + "', 'sales', 'ENABLED')");

        insertCompany(jdbcUrl, COMPANY_A, "custom_a");
        assertStatementRejected(
                jdbcUrl,
                "INSERT INTO core.company_plugin_activation (company_id, plugin_id, desired_state) "
                        + "VALUES ('" + COMPANY_A + "', 'Invalid-Plugin', 'ENABLED')");
        assertStatementRejected(
                jdbcUrl,
                "INSERT INTO core.company_plugin_activation (company_id, plugin_id, desired_state) "
                        + "VALUES ('" + COMPANY_A + "', 'sales', 'UNKNOWN')");
        assertStatementRejected(
                jdbcUrl,
                "INSERT INTO core.company_plugin_activation "
                        + "(company_id, plugin_id, desired_state, version) VALUES "
                        + "('" + COMPANY_A + "', 'sales', 'ENABLED', -1)");
    }

    @Test
    void uniquenessPreventsSharedCustomizationAndDuplicateDecision() throws Exception {
        String jdbcUrl = migratedDatabase("unique_rules");
        insertCompany(jdbcUrl, COMPANY_A, "custom_a");
        insertCompany(jdbcUrl, COMPANY_B, "custom_b");

        assertStatementRejected(
                jdbcUrl,
                "UPDATE core.company SET customization_plugin_id = 'custom_a' "
                        + "WHERE company_id = '" + COMPANY_B + "'");
        execute(jdbcUrl, "INSERT INTO core.company_plugin_activation "
                + "(company_id, plugin_id, desired_state) VALUES "
                + "('" + COMPANY_A + "', 'sales', 'ENABLED')");
        assertStatementRejected(
                jdbcUrl,
                "INSERT INTO core.company_plugin_activation "
                        + "(company_id, plugin_id, desired_state) VALUES "
                        + "('" + COMPANY_A + "', 'sales', 'DISABLED')");

        assertEquals(2, queryInt(jdbcUrl, "SELECT count(*) FROM core.company"));
        assertEquals(1, queryInt(jdbcUrl, "SELECT count(*) FROM core.company_plugin_activation"));
    }

    @Test
    void disablingAnActivationPreservesItsRowAndVersionHistory() throws Exception {
        String jdbcUrl = migratedDatabase("disable");
        insertCompany(jdbcUrl, COMPANY_A, "custom_a");
        execute(jdbcUrl, "INSERT INTO core.company_plugin_activation "
                + "(company_id, plugin_id, desired_state) VALUES "
                + "('" + COMPANY_A + "', 'sales', 'ENABLED')");

        execute(jdbcUrl, "UPDATE core.company_plugin_activation "
                + "SET desired_state = 'DISABLED', version = version + 1, updated_at = CURRENT_TIMESTAMP "
                + "WHERE company_id = '" + COMPANY_A + "' AND plugin_id = 'sales'");

        assertEquals(1, queryInt(jdbcUrl, "SELECT count(*) FROM core.company_plugin_activation "
                + "WHERE company_id = '" + COMPANY_A + "' AND plugin_id = 'sales'"));
        assertEquals("DISABLED", queryString(jdbcUrl, "SELECT desired_state "
                + "FROM core.company_plugin_activation WHERE company_id = '" + COMPANY_A
                + "' AND plugin_id = 'sales'"));
        assertEquals(1, queryInt(jdbcUrl, "SELECT version FROM core.company_plugin_activation "
                + "WHERE company_id = '" + COMPANY_A + "' AND plugin_id = 'sales'"));
    }

    @Test
    void concurrentCustomizationReplacementAllowsExactlyOneWinner() throws Exception {
        String jdbcUrl = migratedDatabase("concurrent");
        insertCompany(jdbcUrl, COMPANY_A, "custom_a");
        insertCompany(jdbcUrl, COMPANY_B, "custom_b");

        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> replaceCustomization(
                    jdbcUrl, COMPANY_A, "custom_shared", start));
            Future<Boolean> second = executor.submit(() -> replaceCustomization(
                    jdbcUrl, COMPANY_B, "custom_shared", start));

            int successes = (first.get(30, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(30, TimeUnit.SECONDS) ? 1 : 0);

            assertEquals(1, successes);
            assertEquals(1, queryInt(jdbcUrl, "SELECT count(*) FROM core.company "
                    + "WHERE customization_plugin_id = 'custom_shared'"));
            assertEquals(2, queryInt(jdbcUrl, "SELECT count(*) FROM core.company"));
            assertEquals(1, queryInt(jdbcUrl, "SELECT sum(version) FROM core.company"));
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertChangedMigrationRejected(String migrationName) throws Exception {
        String jdbcUrl = migratedDatabase("checksum");
        Path location = Files.createDirectory(temporaryDirectory.resolve(
                "changed_" + migrationName.substring(0, 2).toLowerCase()));
        copyMigration(location, "V1__initialize_core_schema.sql");
        copyMigration(location, "V2__add_companies_and_plugin_activation.sql");
        copyMigration(location, "V3__add_identity_membership_and_authorization.sql");
        copyMigration(location, "V4__add_system_authority.sql");
        copyMigration(location, "V5__add_technical_audit_event.sql");
        copyMigration(location, "V6__extend_audit_for_plugin_operations.sql");
        Files.writeString(
                location.resolve(migrationName),
                System.lineSeparator() + "-- controlled checksum mutation" + System.lineSeparator(),
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND);

        String filesystemLocation = "filesystem:"
                + location.toAbsolutePath().toString().replace('\\', '/');
        Flyway changed = flyway(jdbcUrl, filesystemLocation, null);

        assertThrows(FlywayValidateException.class, changed::validate);
        assertEquals(6, queryInt(jdbcUrl, "SELECT count(*) FROM core.flyway_schema_history "
                + "WHERE success AND version IS NOT NULL"));
    }

    private void assertUpgradeConverges(
            String targetVersion,
            int expectedMigrations,
            String prefix) throws Exception {
        String upgradedUrl = createDatabase(prefix);
        flyway(
                upgradedUrl,
                "classpath:db/migration/core",
                MigrationVersion.fromVersion(targetVersion)).migrate();

        MigrationOutcome upgrade = coreExecutor().migrate(configuration(upgradedUrl));
        String emptyUrl = createDatabase(prefix + "_comparison");
        coreExecutor().migrate(configuration(emptyUrl));

        assertEquals(expectedMigrations, upgrade.migrationsExecuted());
        assertEquals("6", upgrade.schemaVersion());
        assertEquals(schemaSignature(emptyUrl), schemaSignature(upgradedUrl));
    }

    private void copyMigration(Path targetDirectory, String name) throws IOException {
        try (InputStream source = getClass().getResourceAsStream("/db/migration/core/" + name)) {
            if (source == null) {
                throw new IOException("Missing migration resource " + name);
            }
            Files.copy(source, targetDirectory.resolve(name));
        }
    }

    private String migratedDatabase(String prefix) throws Exception {
        String jdbcUrl = createDatabase(prefix);
        MigrationOutcome outcome = coreExecutor().migrate(configuration(jdbcUrl));
        assertEquals(6, outcome.migrationsExecuted());
        return jdbcUrl;
    }

    private static FlywayMigrationExecutor coreExecutor() {
        return new FlywayMigrationExecutor(List.of());
    }

    private static PluginDefinition pluginFixture() {
        PluginDescriptor descriptor = new PluginDescriptor(
                new PluginId("plugin_fixture"),
                PluginKind.FUNCTIONAL,
                SemanticVersion.parse("1.0.0"),
                new VersionRange(
                        SemanticVersion.parse("0.4.0"),
                        SemanticVersion.parse("0.5.0")),
                "Plugin migration fixture",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new MigrationContribution(
                        "plg_plugin_fixture",
                        "classpath:db/migration/plugin_fixture")));
        return () -> descriptor;
    }

    private MigratorConfiguration configuration(String jdbcUrl) throws IOException {
        Path passwordFile = Files.createTempFile(temporaryDirectory, "postgres-password-", ".txt");
        Files.writeString(passwordFile, POSTGRES.getPassword(), StandardCharsets.UTF_8);
        return MigratorConfiguration.fromEnvironment(Map.of(
                MigratorConfiguration.DB_URL, jdbcUrl,
                MigratorConfiguration.DB_USER, POSTGRES.getUsername(),
                MigratorConfiguration.DB_PASSWORD_FILE, passwordFile.toString()));
    }

    private String createDatabase(String prefix) throws SQLException {
        String database = "logixone_" + prefix + "_" + UUID.randomUUID().toString().replace("-", "");
        execute(POSTGRES.getJdbcUrl(), "CREATE DATABASE " + database);
        String base = POSTGRES.getJdbcUrl();
        int queryIndex = base.indexOf('?');
        String query = queryIndex < 0 ? "" : base.substring(queryIndex);
        String withoutQuery = queryIndex < 0 ? base : base.substring(0, queryIndex);
        return withoutQuery.substring(0, withoutQuery.lastIndexOf('/') + 1) + database + query;
    }

    private static Flyway flyway(String jdbcUrl, String location, MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(jdbcUrl, POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(FlywayMigrationExecutor.CORE_SCHEMA)
                .defaultSchema(FlywayMigrationExecutor.CORE_SCHEMA)
                .table(FlywayMigrationExecutor.HISTORY_TABLE)
                .locations(location)
                .createSchemas(true)
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .validateOnMigrate(true)
                .failOnMissingLocations(true)
                .outOfOrder(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private static void insertCompany(String jdbcUrl, String companyId, String customizationPluginId)
            throws SQLException {
        execute(jdbcUrl, "INSERT INTO core.company (company_id, status, customization_plugin_id) "
                + "VALUES ('" + companyId + "', 'INACTIVE', '" + customizationPluginId + "')");
    }

    private static boolean replaceCustomization(
            String jdbcUrl,
            String companyId,
            String customizationPluginId,
            CyclicBarrier start) throws Exception {
        try (Connection connection = connect(jdbcUrl);
                Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            start.await(10, TimeUnit.SECONDS);
            try {
                statement.executeUpdate("UPDATE core.company SET customization_plugin_id = '"
                        + customizationPluginId + "', version = version + 1, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE company_id = '" + companyId + "'");
                connection.commit();
                return true;
            } catch (SQLException failure) {
                connection.rollback();
                if (!"23505".equals(failure.getSQLState())) {
                    throw failure;
                }
                return false;
            }
        }
    }

    private static void assertCoreTables(String jdbcUrl) throws SQLException {
        assertEquals(11, queryInt(jdbcUrl, "SELECT count(*) FROM information_schema.tables "
                + "WHERE table_schema = 'core' AND table_name IN "
                + "('company', 'company_plugin_activation', 'app_user', 'company_membership', "
                + "'security_role', 'role_permission', 'membership_role', 'system_role', "
                + "'system_role_permission', 'app_user_system_role', 'audit_event')"));
    }

    private static List<String> schemaSignature(String jdbcUrl) throws SQLException {
        List<String> signature = new ArrayList<>();
        collect(jdbcUrl, signature, "column", "SELECT table_name || ':' || column_name || ':' || data_type || ':' "
                + "|| is_nullable || ':' || coalesce(character_maximum_length::text, '') "
                + "FROM information_schema.columns WHERE table_schema = 'core' "
                + "ORDER BY table_name, ordinal_position");
        collect(jdbcUrl, signature, "constraint", "SELECT conrelid::regclass::text || ':' || conname || ':' "
                + "|| pg_get_constraintdef(oid) FROM pg_constraint "
                + "WHERE connamespace = 'core'::regnamespace ORDER BY conrelid::regclass::text, conname");
        collect(jdbcUrl, signature, "index", "SELECT tablename || ':' || indexname || ':' || indexdef "
                + "FROM pg_indexes WHERE schemaname = 'core' ORDER BY tablename, indexname");
        return List.copyOf(signature);
    }

    private static void collect(
            String jdbcUrl,
            List<String> target,
            String prefix,
            String sql) throws SQLException {
        try (Connection connection = connect(jdbcUrl);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                target.add(prefix + ':' + result.getString(1));
            }
        }
    }

    private static void assertStatementRejected(String jdbcUrl, String sql) {
        assertThrows(SQLException.class, () -> execute(jdbcUrl, sql));
    }

    private static void execute(String jdbcUrl, String sql) throws SQLException {
        try (Connection connection = connect(jdbcUrl);
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static int queryInt(String jdbcUrl, String sql) throws SQLException {
        try (Connection connection = connect(jdbcUrl);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static String queryString(String jdbcUrl, String sql) throws SQLException {
        try (Connection connection = connect(jdbcUrl);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private static Connection connect(String jdbcUrl) throws SQLException {
        return DriverManager.getConnection(jdbcUrl, POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
