package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class BusinessPartnerEntityId implements Serializable {

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "business_partner_id", nullable = false, updatable = false)
    private UUID businessPartnerId;

    protected BusinessPartnerEntityId() {
    }

    BusinessPartnerEntityId(UUID companyId, UUID businessPartnerId) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.businessPartnerId = Objects.requireNonNull(businessPartnerId, "businessPartnerId");
    }

    UUID companyId() {
        return companyId;
    }

    UUID businessPartnerId() {
        return businessPartnerId;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof BusinessPartnerEntityId that
                && Objects.equals(companyId, that.companyId)
                && Objects.equals(businessPartnerId, that.businessPartnerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, businessPartnerId);
    }
}
