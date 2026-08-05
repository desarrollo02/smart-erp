package py.com.logixone.plugins.businesspartners.domain;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Identification preserving the presented value and a deterministic search value. */
public record BusinessPartnerIdentification(
        BusinessPartnerDetailId id,
        BusinessPartnerAttributeCode type,
        Optional<String> countryCode,
        String presentedValue,
        String normalizedValue,
        Optional<String> checkDigit,
        Optional<LocalDate> validUntil) {

    public BusinessPartnerIdentification {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        countryCode = normalizeCountry(countryCode);
        presentedValue = requireText(presentedValue, "presentedValue", 100);
        normalizedValue = normalizeIdentificationValue(presentedValue);
        checkDigit = normalizeOptionalText(checkDigit, "checkDigit", 16);
        validUntil = Objects.requireNonNull(validUntil, "validUntil");
    }

    public static BusinessPartnerIdentification create(
            BusinessPartnerDetailId id,
            BusinessPartnerAttributeCode type,
            Optional<String> countryCode,
            String presentedValue,
            Optional<String> checkDigit,
            Optional<LocalDate> validUntil) {
        return new BusinessPartnerIdentification(
                id, type, countryCode, presentedValue, presentedValue, checkDigit, validUntil);
    }

    public BusinessPartnerIdentificationKey duplicateCandidateKey() {
        return new BusinessPartnerIdentificationKey(type, countryCode, normalizedValue);
    }

    private static String normalizeIdentificationValue(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toUpperCase(Locale.ROOT)
                .replaceAll("[\\p{Z}\\s./-]", "");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Identification value must contain searchable data");
        }
        return normalized;
    }

    static Optional<String> normalizeCountry(Optional<String> value) {
        Objects.requireNonNull(value, "countryCode");
        return value.map(country -> {
            String normalized = Normalizer.normalize(country, Normalizer.Form.NFKC)
                    .trim()
                    .toUpperCase(Locale.ROOT);
            if (!normalized.matches("[A-Z]{2}")) {
                throw new IllegalArgumentException("Country code must be ISO alpha-2");
            }
            return normalized;
        });
    }

    static String requireText(String value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(name + " must not exceed " + maxLength + " characters");
        }
        if (normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(name + " must not contain control characters");
        }
        return normalized;
    }

    static Optional<String> normalizeOptionalText(
            Optional<String> value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        return value.map(text -> requireText(text, name, maxLength));
    }
}
