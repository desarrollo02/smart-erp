package py.com.logixone.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigratorConfigurationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsRequiredConfigurationAndRemovesOneTrailingLineEnding() throws IOException {
        Path passwordFile = writePassword("s3cret-value\r\n");

        MigratorConfiguration configuration = MigratorConfiguration.fromEnvironment(
                validEnvironment(passwordFile));

        assertEquals("jdbc:postgresql://postgres:5432/logixone", configuration.jdbcUrl());
        assertEquals("logixone", configuration.user());
        assertEquals("s3cret-value", configuration.password());
        assertFalse(configuration.toString().contains("s3cret-value"));
    }

    @Test
    void rejectsMissingUrlWithoutIncludingEnvironmentValues() throws IOException {
        Map<String, String> environment = validEnvironment(writePassword("never-print-me"));
        environment.remove(MigratorConfiguration.DB_URL);

        MigratorConfigurationException exception = assertThrows(
                MigratorConfigurationException.class,
                () -> MigratorConfiguration.fromEnvironment(environment));

        assertEquals("MISSING_DB_URL", exception.code());
        assertFalse(exception.getMessage().contains("never-print-me"));
    }

    @Test
    void rejectsNonPostgresqlUrlAndCredentialsEmbeddedInUrl() throws IOException {
        Path passwordFile = writePassword("external-secret");
        Map<String, String> nonPostgresql = validEnvironment(passwordFile);
        nonPostgresql.put(MigratorConfiguration.DB_URL, "jdbc:h2:mem:test");
        Map<String, String> embeddedCredentials = validEnvironment(passwordFile);
        embeddedCredentials.put(
                MigratorConfiguration.DB_URL,
                "jdbc:postgresql://postgres/logixone?user=inline");

        assertEquals(
                "INVALID_DB_URL",
                assertThrows(
                                MigratorConfigurationException.class,
                                () -> MigratorConfiguration.fromEnvironment(nonPostgresql))
                        .code());
        assertEquals(
                "DB_URL_CONTAINS_CREDENTIALS",
                assertThrows(
                                MigratorConfigurationException.class,
                                () -> MigratorConfiguration.fromEnvironment(embeddedCredentials))
                        .code());
    }

    @Test
    void rejectsMissingEmptyAndMultilinePasswordFiles() throws IOException {
        Path missing = temporaryDirectory.resolve("missing.secret");
        Path empty = writePassword("");
        Path multiline = writePassword("first\nsecond\n");

        assertEquals(
                "DB_PASSWORD_FILE_NOT_REGULAR",
                assertThrows(
                                MigratorConfigurationException.class,
                                () -> MigratorConfiguration.fromEnvironment(validEnvironment(missing)))
                        .code());
        assertEquals(
                "INVALID_DB_PASSWORD_LENGTH",
                assertThrows(
                                MigratorConfigurationException.class,
                                () -> MigratorConfiguration.fromEnvironment(validEnvironment(empty)))
                        .code());
        assertEquals(
                "MULTILINE_DB_PASSWORD",
                assertThrows(
                                MigratorConfigurationException.class,
                                () -> MigratorConfiguration.fromEnvironment(validEnvironment(multiline)))
                        .code());
    }

    private Map<String, String> validEnvironment(Path passwordFile) {
        Map<String, String> environment = new HashMap<>();
        environment.put(MigratorConfiguration.DB_URL, "jdbc:postgresql://postgres:5432/logixone");
        environment.put(MigratorConfiguration.DB_USER, "logixone");
        environment.put(MigratorConfiguration.DB_PASSWORD_FILE, passwordFile.toString());
        return environment;
    }

    private Path writePassword(String value) throws IOException {
        Path path = temporaryDirectory.resolve("password-" + System.nanoTime() + ".secret");
        Files.writeString(path, value, StandardCharsets.UTF_8);
        return path;
    }
}
