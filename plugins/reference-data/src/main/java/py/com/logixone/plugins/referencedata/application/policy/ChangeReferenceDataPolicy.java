package py.com.logixone.plugins.referencedata.application.policy;

import java.util.Objects;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;

/** Optimistic command; version zero represents the absence of an override row. */
public record ChangeReferenceDataPolicy(
        ReferenceDataCatalog catalog,
        String code,
        boolean enabled,
        long expectedVersion) {

    public ChangeReferenceDataPolicy {
        Objects.requireNonNull(catalog, "catalog");
        code = ReferenceDataPolicy.canonicalCode(catalog, code);
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
    }
}
