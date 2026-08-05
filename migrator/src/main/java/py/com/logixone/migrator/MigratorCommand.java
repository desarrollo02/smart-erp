package py.com.logixone.migrator;

import java.io.PrintStream;
import java.util.Map;
import java.util.Objects;

final class MigratorCommand {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_MIGRATION_FAILURE = 1;
    static final int EXIT_CONFIGURATION_FAILURE = 2;

    private final MigrationExecutor executor;

    MigratorCommand(MigrationExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    int execute(Map<String, String> environment, PrintStream standardOutput, PrintStream standardError) {
        Objects.requireNonNull(standardOutput, "standardOutput");
        Objects.requireNonNull(standardError, "standardError");

        final MigratorConfiguration configuration;
        try {
            configuration = MigratorConfiguration.fromEnvironment(environment);
        } catch (MigratorConfigurationException exception) {
            standardError.printf("event=configuration_failed code=%s%n", exception.code());
            return EXIT_CONFIGURATION_FAILURE;
        }

        try {
            MigrationOutcome outcome = executor.migrate(configuration);
            outcome.schemas().forEach(schema -> standardOutput.printf(
                    "event=migration_succeeded owner=%s schema=%s migrations_executed=%d schema_version=%s%n",
                    safeToken(schema.owner()),
                    safeToken(schema.schema()),
                    schema.migrationsExecuted(),
                    safeVersion(schema.schemaVersion())));
            return EXIT_SUCCESS;
        } catch (RuntimeException exception) {
            standardError.printf("event=migration_failed type=%s%n", exception.getClass().getSimpleName());
            return EXIT_MIGRATION_FAILURE;
        }
    }

    private static String safeVersion(String schemaVersion) {
        if (schemaVersion == null || schemaVersion.isBlank()) {
            return "none";
        }
        return schemaVersion.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String safeToken(String token) {
        return token.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
