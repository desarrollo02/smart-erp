package py.com.logixone.plugins.commercialcatalog.domain;

import java.util.Map;
import java.util.Objects;

/** Explicit variant assignment for one item. */
public record CatalogVariant(
        VariantFamilyId familyId,
        long familyVersion,
        Map<VariantAttributeCode, VariantAttributeValue> attributes) {
    public CatalogVariant {
        Objects.requireNonNull(familyId, "familyId");
        if (familyVersion < 0) {
            throw new IllegalArgumentException("familyVersion must not be negative");
        }
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("Variant attributes must not be empty");
        }
    }
}
