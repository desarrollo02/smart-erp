package py.com.logixone.migrator;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import py.com.logixone.plugin.api.PluginDefinition;

final class FlywayMigrationExecutor implements MigrationExecutor {

    static final String CORE_OWNER = "kernel";
    static final String CORE_SCHEMA = "core";
    static final String CORE_LOCATION = "classpath:db/migration/core";
    static final String HISTORY_TABLE = "flyway_schema_history";

    private final Supplier<MigrationPlan> migrationPlan;

    FlywayMigrationExecutor() {
        this(MigrationPlan::discover);
    }

    FlywayMigrationExecutor(Collection<? extends PluginDefinition> definitions) {
        List<PluginDefinition> immutableDefinitions = List.copyOf(definitions);
        this.migrationPlan = () -> MigrationPlan.from(immutableDefinitions);
    }

    private FlywayMigrationExecutor(Supplier<MigrationPlan> migrationPlan) {
        this.migrationPlan = migrationPlan;
    }

    @Override
    public MigrationOutcome migrate(MigratorConfiguration configuration) {
        MigrationPlan plan = migrationPlan.get();
        List<SchemaMigrationOutcome> outcomes = new ArrayList<>();
        for (MigrationTarget target : plan.targets()) {
            outcomes.add(migrateTarget(configuration, target));
        }
        int total = outcomes.stream().mapToInt(SchemaMigrationOutcome::migrationsExecuted).sum();
        String coreVersion = outcomes.stream()
                .filter(outcome -> CORE_SCHEMA.equals(outcome.schema()))
                .map(SchemaMigrationOutcome::schemaVersion)
                .findFirst()
                .orElse(null);
        return new MigrationOutcome(total, coreVersion, outcomes);
    }

    private SchemaMigrationOutcome migrateTarget(
            MigratorConfiguration configuration,
            MigrationTarget target) {
        Flyway flyway = Flyway.configure()
                .dataSource(configuration.jdbcUrl(), configuration.user(), configuration.password())
                .schemas(target.schema())
                .defaultSchema(target.schema())
                .table(HISTORY_TABLE)
                .locations(target.locations().toArray(String[]::new))
                .createSchemas(true)
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .validateOnMigrate(true)
                .failOnMissingLocations(true)
                .outOfOrder(false)
                .load();

        MigrateResult result = flyway.migrate();
        flyway.validate();

        MigrationInfo current = flyway.info().current();
        String schemaVersion = current == null || current.getVersion() == null
                ? null
                : current.getVersion().getVersion();
        return new SchemaMigrationOutcome(
                target.owner(),
                target.schema(),
                result.migrationsExecuted,
                schemaVersion);
    }
}
