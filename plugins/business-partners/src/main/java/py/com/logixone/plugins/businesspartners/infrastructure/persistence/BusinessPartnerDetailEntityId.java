package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class BusinessPartnerDetailEntityId implements Serializable {

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "business_partner_id", nullable = false, updatable = false)
    private UUID businessPartnerId;

    @Column(name = "detail_id", nullable = false, updatable = false)
    private UUID detailId;

    protected BusinessPartnerDetailEntityId() {
    }

    BusinessPartnerDetailEntityId(UUID companyId, UUID businessPartnerId, UUID detailId) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.businessPartnerId = Objects.requireNonNull(businessPartnerId, "businessPartnerId");
        this.detailId = Objects.requireNonNull(detailId, "detailId");
    }

    UUID companyId() {
        return companyId;
    }

    UUID businessPartnerId() {
        return businessPartnerId;
    }

    UUID detailId() {
        return detailId;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof BusinessPartnerDetailEntityId that
                && Objects.equals(companyId, that.companyId)
                && Objects.equals(businessPartnerId, that.businessPartnerId)
                && Objects.equals(detailId, that.detailId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, businessPartnerId, detailId);
    }
}
