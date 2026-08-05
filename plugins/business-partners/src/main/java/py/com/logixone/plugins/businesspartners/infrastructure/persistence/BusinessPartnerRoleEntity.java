package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerCode;
import py.com.logixone.plugins.businesspartners.domain.CommercialRole;

@Entity
@Table(name = "business_partner_role", schema = BusinessPartnersPersistenceNames.SCHEMA)
public class BusinessPartnerRoleEntity {

    @EmbeddedId
    private BusinessPartnerRoleEntityId id;
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private BusinessPartnerState state;
    @Column(name = "role_code", length = 64)
    private String roleCode;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BusinessPartnerRoleEntity() {
    }

    static BusinessPartnerRoleEntity from(UUID companyId, UUID partnerId, CommercialRole role) {
        BusinessPartnerRoleEntity entity = new BusinessPartnerRoleEntity();
        entity.id = new BusinessPartnerRoleEntityId(companyId, partnerId, role.type());
        entity.apply(role);
        return entity;
    }

    void apply(CommercialRole role) {
        state = role.state();
        roleCode = role.code().map(BusinessPartnerCode::value).orElse(null);
        updatedAt = Instant.now();
    }

    CommercialRole toDomain() {
        return new CommercialRole(
                id.roleType(),
                state,
                Optional.ofNullable(roleCode).map(BusinessPartnerCode::new));
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }
}
