package py.com.logixone.plugins.commercialcatalog.domain;

import java.util.Objects;
import java.util.UUID;

public record BrandId(UUID value) {
    public BrandId { Objects.requireNonNull(value, "value"); }
}
