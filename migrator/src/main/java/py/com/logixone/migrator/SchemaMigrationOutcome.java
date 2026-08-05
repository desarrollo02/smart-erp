package py.com.logixone.migrator;

import java.util.Objects;

record SchemaMigrationOutcome(
        String owner,
        String schema,
        int migrationsExecuted,
        String schemaVersion) {

    SchemaMigrationOutcome {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(schema, "schema");
        if (migrationsExecuted < 0) {
            throw new IllegalArgumentException("migrationsExecuted must not be negative");
        }
    }
}

