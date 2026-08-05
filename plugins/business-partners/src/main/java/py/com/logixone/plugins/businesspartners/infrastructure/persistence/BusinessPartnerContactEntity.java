package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerContact;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerContactChannel;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDetailId;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerName;

@Entity
@Table(name = "business_partner_contact", schema = BusinessPartnersPersistenceNames.SCHEMA)
public class BusinessPartnerContactEntity {

    @EmbeddedId
    @AttributeOverride(name = "detailId", column = @Column(
            name = "contact_id", nullable = false, updatable = false))
    private BusinessPartnerDetailEntityId id;
    @Column(name = "contact_name", nullable = false, length = 200)
    private String contactName;
    @Column(name = "position_name", length = 200)
    private String positionName;
    @Column(name = "active", nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BusinessPartnerContactEntity() {
    }

    static BusinessPartnerContactEntity from(
            UUID companyId, UUID partnerId, BusinessPartnerContact value) {
        BusinessPartnerContactEntity entity = new BusinessPartnerContactEntity();
        entity.id = new BusinessPartnerDetailEntityId(companyId, partnerId, value.id().value());
        entity.apply(value);
        return entity;
    }

    void apply(BusinessPartnerContact value) {
        contactName = value.name().value();
        positionName = value.position().map(BusinessPartnerName::value).orElse(null);
        active = value.active();
        updatedAt = Instant.now();
    }

    BusinessPartnerContact toDomain(List<BusinessPartnerContactChannel> channels) {
        return new BusinessPartnerContact(
                new BusinessPartnerDetailId(id.detailId()),
                new BusinessPartnerName(contactName),
                Optional.ofNullable(positionName).map(BusinessPartnerName::new),
                channels,
                active);
    }

    UUID contactId() {
        return id.detailId();
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }
}
