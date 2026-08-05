package py.com.logixone.plugins.businesspartners.domain;

import java.text.Normalizer;
import java.util.Objects;

/** Normalized display, legal or trade name. */
public record BusinessPartnerName(String value) {

    public static final int MAX_LENGTH = 200;

    public BusinessPartnerName {
        Objects.requireNonNull(value, "value");
        value = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Business partner name must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Business partner name must not exceed " + MAX_LENGTH + " characters");
        }
        if (value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "Business partner name must not contain control characters");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
