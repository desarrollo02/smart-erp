package py.com.logixone.migrator;

@FunctionalInterface
interface MigrationExecutor {

    MigrationOutcome migrate(MigratorConfiguration configuration);
}
