package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDetailId;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerIdentification;

@Entity
@Table(name = "business_partner_identification", schema = BusinessPartnersPersistenceNames.SCHEMA)
public class BusinessPartnerIdentificationEntity {

    @EmbeddedId
    @AttributeOverrides(@AttributeOverride(name = "detailId", column = @Column(
            name = "identification_id", nullable = false, updatable = false)))
    private BusinessPartnerDetailEntityId id;
    @Column(name = "type_code", nullable = false, length = 48)
    private String typeCode;
    @Column(name = "country_code", length = 2)
    private String countryCode;
    @Column(name = "presented_value", nullable = false, length = 100)
    private String presentedValue;
    @Column(name = "normalized_value", nullable = false, length = 100)
    private String normalizedValue;
    @Column(name = "check_digit", length = 16)
    private String checkDigit;
    @Column(name = "valid_until")
    private LocalDate validUntil;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BusinessPartnerIdentificationEntity() {
    }

    static BusinessPartnerIdentificationEntity from(
            UUID companyId, UUID partnerId, BusinessPartnerIdentification value) {
        BusinessPartnerIdentificationEntity entity = new BusinessPartnerIdentificationEntity();
        entity.id = new BusinessPartnerDetailEntityId(companyId, partnerId, value.id().value());
        entity.apply(value);
        return entity;
    }

    void apply(BusinessPartnerIdentification value) {
        typeCode = value.type().value();
        countryCode = value.countryCode().orElse(null);
        presentedValue = value.presentedValue();
        normalizedValue = value.normalizedValue();
        checkDigit = value.checkDigit().orElse(null);
        validUntil = value.validUntil().orElse(null);
        updatedAt = Instant.now();
    }

    BusinessPartnerIdentification toDomain() {
        return new BusinessPartnerIdentification(
                new BusinessPartnerDetailId(id.detailId()),
                new BusinessPartnerAttributeCode(typeCode),
                Optional.ofNullable(countryCode),
                presentedValue,
                normalizedValue,
                Optional.ofNullable(checkDigit),
                Optional.ofNullable(validUntil));
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }
}
