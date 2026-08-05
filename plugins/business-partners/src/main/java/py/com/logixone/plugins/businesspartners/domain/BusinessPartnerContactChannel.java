package py.com.logixone.plugins.businesspartners.domain;

import java.util.Objects;

/** Email, phone, WhatsApp, website or future channel identified by stable codes. */
public record BusinessPartnerContactChannel(
        BusinessPartnerDetailId id,
        BusinessPartnerAttributeCode kind,
        BusinessPartnerAttributeCode purpose,
        String value,
        boolean active,
        boolean primary) {

    public BusinessPartnerContactChannel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(purpose, "purpose");
        value = BusinessPartnerIdentification.requireText(value, "value", 254);
        if (!active && primary) {
            throw new IllegalArgumentException("An inactive contact channel cannot be primary");
        }
    }

    BusinessPartnerContactChannel withoutPrimary() {
        if (!primary) {
            return this;
        }
        return new BusinessPartnerContactChannel(id, kind, purpose, value, active, false);
    }
}
