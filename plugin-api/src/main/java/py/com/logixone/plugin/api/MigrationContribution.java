package py.com.logixone.plugin.api;

import java.util.Objects;
import java.util.regex.Pattern;

public record MigrationContribution(String schema, String location) {

    private static final Pattern SCHEMA = Pattern.compile("[a-z][a-z0-9_]{0,62}");
    private static final Pattern LOCATION = Pattern.compile("classpath:db/migration/[A-Za-z0-9_/-]+");

    public MigrationContribution {
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(location, "location");
        if (!SCHEMA.matcher(schema).matches()) {
            throw new IllegalArgumentException("Invalid migration schema: " + schema);
        }
        if (!LOCATION.matcher(location).matches() || location.contains("..") || location.endsWith("/")) {
            throw new IllegalArgumentException("Invalid classpath migration location: " + location);
        }
    }
}
