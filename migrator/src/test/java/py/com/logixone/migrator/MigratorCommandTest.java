package py.com.logixone.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigratorCommandTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void reportsSuccessfulMigrationWithoutPrintingConfiguration() throws IOException {
        CapturedExecution execution = execute(
                configuration -> outcome(1, "1"),
                validEnvironment("do-not-print-this"));

        assertEquals(MigratorCommand.EXIT_SUCCESS, execution.exitCode());
        assertEquals(
                "event=migration_succeeded owner=kernel schema=core migrations_executed=1 schema_version=1",
                execution.standardOutput().strip());
        assertTrue(execution.standardError().isEmpty());
        assertFalse(execution.allOutput().contains("do-not-print-this"));
    }

    @Test
    void reportsConfigurationFailureWithStableCode() {
        CapturedExecution execution = execute(
                configuration -> outcome(0, null),
                Map.of());

        assertEquals(MigratorCommand.EXIT_CONFIGURATION_FAILURE, execution.exitCode());
        assertEquals("event=configuration_failed code=MISSING_DB_URL", execution.standardError().strip());
        assertTrue(execution.standardOutput().isEmpty());
    }

    @Test
    void reportsMigrationFailureWithoutPrintingExceptionMessageOrSecret() throws IOException {
        String secret = "never-leak-this-password";
        CapturedExecution execution = execute(
                configuration -> {
                    throw new IllegalStateException("database rejected password=" + secret);
                },
                validEnvironment(secret));

        assertEquals(MigratorCommand.EXIT_MIGRATION_FAILURE, execution.exitCode());
        assertEquals("event=migration_failed type=IllegalStateException", execution.standardError().strip());
        assertTrue(execution.standardOutput().isEmpty());
        assertFalse(execution.allOutput().contains(secret));
        assertFalse(execution.allOutput().contains("database rejected"));
    }

    private CapturedExecution execute(MigrationExecutor executor, Map<String, String> environment) {
        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream standardError = new ByteArrayOutputStream();
        int exitCode = new MigratorCommand(executor).execute(
                environment,
                new PrintStream(standardOutput, true, StandardCharsets.UTF_8),
                new PrintStream(standardError, true, StandardCharsets.UTF_8));
        return new CapturedExecution(
                exitCode,
                standardOutput.toString(StandardCharsets.UTF_8),
                standardError.toString(StandardCharsets.UTF_8));
    }

    private static MigrationOutcome outcome(int migrationsExecuted, String schemaVersion) {
        return new MigrationOutcome(
                migrationsExecuted,
                schemaVersion,
                java.util.List.of(new SchemaMigrationOutcome(
                        FlywayMigrationExecutor.CORE_OWNER,
                        FlywayMigrationExecutor.CORE_SCHEMA,
                        migrationsExecuted,
                        schemaVersion)));
    }

    private Map<String, String> validEnvironment(String secret) throws IOException {
        Path passwordFile = temporaryDirectory.resolve("postgres-password.txt");
        Files.writeString(passwordFile, secret, StandardCharsets.UTF_8);
        Map<String, String> environment = new HashMap<>();
        environment.put(MigratorConfiguration.DB_URL, "jdbc:postgresql://postgres:5432/logixone");
        environment.put(MigratorConfiguration.DB_USER, "logixone");
        environment.put(MigratorConfiguration.DB_PASSWORD_FILE, passwordFile.toString());
        return environment;
    }

    private record CapturedExecution(int exitCode, String standardOutput, String standardError) {

        String allOutput() {
            return standardOutput + standardError;
        }
    }
}
