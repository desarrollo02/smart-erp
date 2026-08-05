package py.com.logixone.migrator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class MigratorConfiguration {

    static final String DB_URL = "LOGIXONE_DB_URL";
    static final String DB_USER = "LOGIXONE_DB_USER";
    static final String DB_PASSWORD_FILE = "LOGIXONE_DB_PASSWORD_FILE";

    private static final int MAX_PASSWORD_BYTES = 4096;

    private final String jdbcUrl;
    private final String user;
    private final String password;

    private MigratorConfiguration(String jdbcUrl, String user, String password) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
    }

    static MigratorConfiguration fromEnvironment(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");

        String jdbcUrl = required(environment, DB_URL, "MISSING_DB_URL");
        validateJdbcUrl(jdbcUrl);
        String user = required(environment, DB_USER, "MISSING_DB_USER");
        String passwordFile = required(environment, DB_PASSWORD_FILE, "MISSING_DB_PASSWORD_FILE");

        return new MigratorConfiguration(jdbcUrl, user, readPassword(passwordFile));
    }

    String jdbcUrl() {
        return jdbcUrl;
    }

    String user() {
        return user;
    }

    String password() {
        return password;
    }

    @Override
    public String toString() {
        return "MigratorConfiguration[jdbcUrl=" + jdbcUrl + ", user=" + user + ", password=REDACTED]";
    }

    private static String required(Map<String, String> environment, String key, String errorCode) {
        String value = environment.get(key);
        if (value == null || value.isBlank()) {
            throw new MigratorConfigurationException(errorCode);
        }
        return value.trim();
    }

    private static void validateJdbcUrl(String jdbcUrl) {
        String normalized = jdbcUrl.toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("jdbc:postgresql://")) {
            throw new MigratorConfigurationException("INVALID_DB_URL");
        }
        if (normalized.matches(".*[?&](?:user|password)=.*")) {
            throw new MigratorConfigurationException("DB_URL_CONTAINS_CREDENTIALS");
        }
    }

    private static String readPassword(String passwordFile) {
        final Path path;
        try {
            path = Path.of(passwordFile);
        } catch (InvalidPathException exception) {
            throw new MigratorConfigurationException("INVALID_DB_PASSWORD_FILE");
        }

        if (!Files.isRegularFile(path)) {
            throw new MigratorConfigurationException("DB_PASSWORD_FILE_NOT_REGULAR");
        }

        final byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new MigratorConfigurationException("DB_PASSWORD_FILE_UNREADABLE");
        }

        if (bytes.length == 0 || bytes.length > MAX_PASSWORD_BYTES) {
            throw new MigratorConfigurationException("INVALID_DB_PASSWORD_LENGTH");
        }

        String password = decodeUtf8(bytes);
        if (password.endsWith("\r\n")) {
            password = password.substring(0, password.length() - 2);
        } else if (password.endsWith("\n")) {
            password = password.substring(0, password.length() - 1);
        }

        if (password.isBlank()) {
            throw new MigratorConfigurationException("EMPTY_DB_PASSWORD");
        }
        if (password.indexOf('\r') >= 0 || password.indexOf('\n') >= 0) {
            throw new MigratorConfigurationException("MULTILINE_DB_PASSWORD");
        }
        return password;
    }

    private static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new MigratorConfigurationException("DB_PASSWORD_NOT_UTF8");
        }
    }
}
