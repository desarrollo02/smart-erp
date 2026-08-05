package py.com.logixone.migrator;

import java.util.List;

record MigrationOutcome(
        int migrationsExecuted,
        String schemaVersion,
        List<SchemaMigrationOutcome> schemas) {

    MigrationOutcome {
        if (migrationsExecuted < 0) {
            throw new IllegalArgumentException("migrationsExecuted must not be negative");
        }
        schemas = List.copyOf(schemas);
    }
}
