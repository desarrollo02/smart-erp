package py.com.logixone.migrator;

final class MigratorConfigurationException extends IllegalArgumentException {

    private final String code;

    MigratorConfigurationException(String code) {
        super(code);
        this.code = code;
    }

    String code() {
        return code;
    }
}
