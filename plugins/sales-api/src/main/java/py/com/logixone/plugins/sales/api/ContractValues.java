package py.com.logixone.plugins.sales.api;

import java.util.UUID;

final class ContractValues {
    private ContractValues() { }
    static UUID uuid(String value, String name) {
        try { return UUID.fromString(value); }
        catch (RuntimeException exception) { throw new IllegalArgumentException("Invalid " + name, exception); }
    }
    static String code(String value, String name, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum) {
            throw new IllegalArgumentException(name + " must contain between 1 and " + maximum + " characters");
        }
        return value;
    }
}
