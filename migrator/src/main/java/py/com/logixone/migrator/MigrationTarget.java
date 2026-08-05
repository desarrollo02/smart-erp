package py.com.logixone.migrator;

import java.util.List;
import java.util.Objects;

record MigrationTarget(String owner, String schema, List<String> locations) {

    MigrationTarget {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(locations, "locations");
        if (owner.isBlank()) {
            throw new IllegalArgumentException("Migration owner must not be blank");
        }
        if (schema.isBlank()) {
            throw new IllegalArgumentException("Migration schema must not be blank");
        }
        locations = List.copyOf(locations);
        if (locations.isEmpty() || locations.stream().anyMatch(location -> location == null || location.isBlank())) {
            throw new IllegalArgumentException("Migration target must contain non-blank locations");
        }
    }
}

