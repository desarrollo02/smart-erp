package py.com.logixone.plugins.inventory.api;

import java.util.Objects;

/** Public immutable location identity safe for operational selectors. */
public record StockLocationReference(
        StockLocationId id,
        String code,
        String name,
        boolean active,
        long version) {

    public StockLocationReference {
        Objects.requireNonNull(id, "id");
        code = ContractValues.code(code, "code", 64);
        name = ContractValues.text(name, "name", 160);
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
