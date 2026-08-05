package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerRole;

@Embeddable
public class BusinessPartnerRoleEntityId implements Serializable {

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "business_partner_id", nullable = false, updatable = false)
    private UUID businessPartnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, updatable = false, length = 16)
    private BusinessPartnerRole roleType;

    protected BusinessPartnerRoleEntityId() {
    }

    BusinessPartnerRoleEntityId(
            UUID companyId, UUID businessPartnerId, BusinessPartnerRole roleType) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.businessPartnerId = Objects.requireNonNull(businessPartnerId, "businessPartnerId");
        this.roleType = Objects.requireNonNull(roleType, "roleType");
    }

    BusinessPartnerRole roleType() {
        return roleType;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof BusinessPartnerRoleEntityId that
                && Objects.equals(companyId, that.companyId)
                && Objects.equals(businessPartnerId, that.businessPartnerId)
                && roleType == that.roleType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, businessPartnerId, roleType);
    }
}
