package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerContactChannel;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDetailId;

@Entity
@Table(name = "business_partner_contact_channel", schema = BusinessPartnersPersistenceNames.SCHEMA)
public class BusinessPartnerContactChannelEntity {

    @EmbeddedId
    private BusinessPartnerContactChannelEntityId id;
    @Column(name = "kind_code", nullable = false, length = 48)
    private String kindCode;
    @Column(name = "purpose_code", nullable = false, length = 48)
    private String purposeCode;
    @Column(name = "channel_value", nullable = false, length = 254)
    private String channelValue;
    @Column(name = "active", nullable = false)
    private boolean active;
    @Column(name = "is_primary", nullable = false)
    private boolean primary;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BusinessPartnerContactChannelEntity() {
    }

    static BusinessPartnerContactChannelEntity from(
            UUID companyId,
            UUID partnerId,
            UUID contactId,
            BusinessPartnerContactChannel value) {
        BusinessPartnerContactChannelEntity entity = new BusinessPartnerContactChannelEntity();
        entity.id = new BusinessPartnerContactChannelEntityId(
                companyId, partnerId, contactId, value.id().value());
        entity.apply(value);
        return entity;
    }

    void apply(BusinessPartnerContactChannel value) {
        kindCode = value.kind().value();
        purposeCode = value.purpose().value();
        channelValue = value.value();
        active = value.active();
        primary = value.primary();
        updatedAt = Instant.now();
    }

    BusinessPartnerContactChannel toDomain() {
        return new BusinessPartnerContactChannel(
                new BusinessPartnerDetailId(id.channelId()),
                new BusinessPartnerAttributeCode(kindCode),
                new BusinessPartnerAttributeCode(purposeCode),
                channelValue,
                active,
                primary);
    }

    UUID contactId() {
        return id.contactId();
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }
}
