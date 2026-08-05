package py.com.logixone.plugins.businesspartners.api;

import java.util.Objects;
import java.util.Set;

/** Minimal immutable projection safe for synchronous consumption by other plugins. */
public record BusinessPartnerReference(
        BusinessPartnerId id,
        String code,
        String displayName,
        BusinessPartnerKind kind,
        BusinessPartnerState state,
        Set<BusinessPartnerRole> roles,
        long version) {

    public BusinessPartnerReference {
        Objects.requireNonNull(id, "id");
        code = requireText(code, "code");
        displayName = requireText(displayName, "displayName");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(state, "state");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
