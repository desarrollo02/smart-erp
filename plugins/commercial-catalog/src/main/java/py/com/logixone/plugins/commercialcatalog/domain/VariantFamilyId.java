package py.com.logixone.plugins.commercialcatalog.domain;

import java.util.Objects;
import java.util.UUID;

public record VariantFamilyId(UUID value) {
    public VariantFamilyId { Objects.requireNonNull(value, "value"); }
}
