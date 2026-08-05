package py.com.logixone.plugins.commercialcatalog.domain;

import java.util.Objects;
import java.util.UUID;

/** Internal tax-profile identity; it is deliberately not a SIFEN code. */
public record TaxProfileId(UUID value) {
    public TaxProfileId { Objects.requireNonNull(value, "value"); }
}
