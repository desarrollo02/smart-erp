package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class BusinessPartnerContactChannelEntityId implements Serializable {

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Column(name = "business_partner_id", nullable = false, updatable = false)
    private UUID businessPartnerId;
    @Column(name = "contact_id", nullable = false, updatable = false)
    private UUID contactId;
    @Column(name = "channel_id", nullable = false, updatable = false)
    private UUID channelId;

    protected BusinessPartnerContactChannelEntityId() {
    }

    BusinessPartnerContactChannelEntityId(
            UUID companyId, UUID businessPartnerId, UUID contactId, UUID channelId) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.businessPartnerId = Objects.requireNonNull(businessPartnerId, "businessPartnerId");
        this.contactId = Objects.requireNonNull(contactId, "contactId");
        this.channelId = Objects.requireNonNull(channelId, "channelId");
    }

    UUID contactId() {
        return contactId;
    }

    UUID channelId() {
        return channelId;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof BusinessPartnerContactChannelEntityId that
                && Objects.equals(companyId, that.companyId)
                && Objects.equals(businessPartnerId, that.businessPartnerId)
                && Objects.equals(contactId, that.contactId)
                && Objects.equals(channelId, that.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, businessPartnerId, contactId, channelId);
    }
}
