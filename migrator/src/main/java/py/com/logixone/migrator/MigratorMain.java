package py.com.logixone.migrator;

public final class MigratorMain {

    private MigratorMain() {
    }

    public static void main(String[] arguments) {
        int exitCode = new MigratorCommand(new FlywayMigrationExecutor())
                .execute(System.getenv(), System.out, System.err);
        if (exitCode != MigratorCommand.EXIT_SUCCESS) {
            System.exit(exitCode);
        }
    }
}
