package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAddress;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDetailId;

@Entity
@Table(name = "business_partner_address", schema = BusinessPartnersPersistenceNames.SCHEMA)
public class BusinessPartnerAddressEntity {

    @EmbeddedId
    @AttributeOverride(name = "detailId", column = @Column(
            name = "address_id", nullable = false, updatable = false))
    private BusinessPartnerDetailEntityId id;
    @Column(name = "type_code", nullable = false, length = 48)
    private String typeCode;
    @Column(name = "purpose_code", nullable = false, length = 48)
    private String purposeCode;
    @Column(name = "address_line", nullable = false, length = 250)
    private String addressLine;
    @Column(name = "additional_line", length = 250)
    private String additionalLine;
    @Column(name = "house_number", length = 32)
    private String houseNumber;
    @Column(name = "postal_code", length = 32)
    private String postalCode;
    @Column(name = "country_code", length = 2)
    private String countryCode;
    @Column(name = "first_administrative_area", length = 120)
    private String firstAdministrativeArea;
    @Column(name = "locality", length = 120)
    private String locality;
    @Column(name = "active", nullable = false)
    private boolean active;
    @Column(name = "is_primary", nullable = false)
    private boolean primary;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BusinessPartnerAddressEntity() {
    }

    static BusinessPartnerAddressEntity from(
            UUID companyId, UUID partnerId, BusinessPartnerAddress value) {
        BusinessPartnerAddressEntity entity = new BusinessPartnerAddressEntity();
        entity.id = new BusinessPartnerDetailEntityId(companyId, partnerId, value.id().value());
        entity.apply(value);
        return entity;
    }

    void apply(BusinessPartnerAddress value) {
        typeCode = value.type().value();
        purposeCode = value.purpose().value();
        addressLine = value.addressLine();
        additionalLine = value.additionalLine().orElse(null);
        houseNumber = value.houseNumber().orElse(null);
        postalCode = value.postalCode().orElse(null);
        countryCode = value.countryCode().orElse(null);
        firstAdministrativeArea = value.firstAdministrativeArea().orElse(null);
        locality = value.locality().orElse(null);
        active = value.active();
        primary = value.primary();
        updatedAt = Instant.now();
    }

    BusinessPartnerAddress toDomain() {
        return new BusinessPartnerAddress(
                new BusinessPartnerDetailId(id.detailId()),
                new BusinessPartnerAttributeCode(typeCode),
                new BusinessPartnerAttributeCode(purposeCode),
                addressLine,
                Optional.ofNullable(additionalLine),
                Optional.ofNullable(houseNumber),
                Optional.ofNullable(postalCode),
                Optional.ofNullable(countryCode),
                Optional.ofNullable(firstAdministrativeArea),
                Optional.ofNullable(locality),
                active,
                primary);
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }
}
