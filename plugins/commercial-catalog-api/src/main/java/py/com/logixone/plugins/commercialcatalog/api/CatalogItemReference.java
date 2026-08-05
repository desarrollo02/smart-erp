package py.com.logixone.plugins.commercialcatalog.api;

import java.util.Objects;
import java.util.Set;

/** Minimal immutable catalog projection safe for other plugins. */
public record CatalogItemReference(
        CatalogItemId id,
        String code,
        String displayName,
        CatalogItemType type,
        CatalogItemState state,
        Set<CatalogItemScope> scopes,
        String baseUnitCode,
        long version) {

    public CatalogItemReference {
        Objects.requireNonNull(id, "id");
        code = ContractValues.code(code, "code", 64);
        displayName = ContractValues.text(displayName, "displayName", 200);
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(state, "state");
        scopes = Set.copyOf(Objects.requireNonNull(scopes, "scopes"));
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException("scopes must not be empty");
        }
        baseUnitCode = ContractValues.code(baseUnitCode, "baseUnitCode", 16);
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
