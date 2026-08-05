package py.com.logixone.plugins.businesspartners.domain;

import java.util.Objects;
import java.util.Optional;

/** Candidate duplicate key; it informs review but does not merge partners. */
public record BusinessPartnerIdentificationKey(
        BusinessPartnerAttributeCode type,
        Optional<String> countryCode,
        String normalizedValue) {

    public BusinessPartnerIdentificationKey {
        Objects.requireNonNull(type, "type");
        countryCode = Objects.requireNonNull(countryCode, "countryCode");
        normalizedValue = Objects.requireNonNull(normalizedValue, "normalizedValue");
    }
}
