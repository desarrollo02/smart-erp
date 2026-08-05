package py.com.logixone.plugins.businesspartners.api;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity used to reference a business partner across plugin boundaries. */
public record BusinessPartnerId(UUID value) implements Comparable<BusinessPartnerId> {

    public BusinessPartnerId {
        Objects.requireNonNull(value, "value");
    }

    public static BusinessPartnerId parse(String value) {
        Objects.requireNonNull(value, "value");
        UUID parsed;
        try {
            parsed = UUID.fromString(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "Business partner id must be a canonical UUID", failure);
        }
        if (!parsed.toString().equals(value)) {
            throw new IllegalArgumentException(
                    "Business partner id must be a canonical lower-case UUID");
        }
        return new BusinessPartnerId(parsed);
    }

    @Override
    public int compareTo(BusinessPartnerId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
