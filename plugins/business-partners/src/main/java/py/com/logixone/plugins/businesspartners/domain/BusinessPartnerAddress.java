package py.com.logixone.plugins.businesspartners.domain;

import java.util.Objects;
import java.util.Optional;

/** Postal or physical location with extensible type and purpose codes. */
public record BusinessPartnerAddress(
        BusinessPartnerDetailId id,
        BusinessPartnerAttributeCode type,
        BusinessPartnerAttributeCode purpose,
        String addressLine,
        Optional<String> additionalLine,
        Optional<String> houseNumber,
        Optional<String> postalCode,
        Optional<String> countryCode,
        Optional<String> firstAdministrativeArea,
        Optional<String> locality,
        boolean active,
        boolean primary) {

    public BusinessPartnerAddress {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(purpose, "purpose");
        addressLine = BusinessPartnerIdentification.requireText(
                addressLine, "addressLine", 250);
        additionalLine = BusinessPartnerIdentification.normalizeOptionalText(
                additionalLine, "additionalLine", 250);
        houseNumber = BusinessPartnerIdentification.normalizeOptionalText(
                houseNumber, "houseNumber", 32);
        postalCode = BusinessPartnerIdentification.normalizeOptionalText(
                postalCode, "postalCode", 32);
        countryCode = BusinessPartnerIdentification.normalizeCountry(countryCode);
        firstAdministrativeArea = BusinessPartnerIdentification.normalizeOptionalText(
                firstAdministrativeArea, "firstAdministrativeArea", 120);
        locality = BusinessPartnerIdentification.normalizeOptionalText(
                locality, "locality", 120);
        if (!active && primary) {
            throw new IllegalArgumentException("An inactive address cannot be primary");
        }
    }

    BusinessPartnerAddress withoutPrimary() {
        if (!primary) {
            return this;
        }
        return new BusinessPartnerAddress(
                id,
                type,
                purpose,
                addressLine,
                additionalLine,
                houseNumber,
                postalCode,
                countryCode,
                firstAdministrativeArea,
                locality,
                active,
                false);
    }
}
