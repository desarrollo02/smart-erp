package py.com.logixone.plugins.businesspartners.domain;

import java.util.Objects;
import java.util.UUID;

/** Opaque identity for a child owned by one business partner aggregate. */
public record BusinessPartnerDetailId(UUID value) implements Comparable<BusinessPartnerDetailId> {

    public BusinessPartnerDetailId {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public int compareTo(BusinessPartnerDetailId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
