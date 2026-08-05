package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerName;

@Entity
@Table(name = "business_partner_definition", schema = BusinessPartnersPersistenceNames.SCHEMA)
public class BusinessPartnerDefinitionEntity {

    @EmbeddedId
    private BusinessPartnerDefinitionEntityId id;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private BusinessPartnerState state;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BusinessPartnerDefinitionEntity() {
    }

    static BusinessPartnerDefinitionEntity from(BusinessPartnerDefinition definition) {
        BusinessPartnerDefinitionEntity entity = new BusinessPartnerDefinitionEntity();
        entity.id = new BusinessPartnerDefinitionEntityId(
                definition.companyId().value(), definition.kind(), definition.code().value());
        entity.displayName = definition.displayName().value();
        entity.state = definition.state();
        entity.version = definition.version();
        return entity;
    }

    BusinessPartnerDefinition toDomain() {
        return new BusinessPartnerDefinition(
                new CompanyId(id.companyId()),
                id.kind(),
                new BusinessPartnerAttributeCode(id.code()),
                new BusinessPartnerName(displayName),
                state,
                version);
    }

    long version() {
        return version;
    }

    void changeState(BusinessPartnerState targetState) {
        state = java.util.Objects.requireNonNull(targetState, "targetState");
    }

    void apply(BusinessPartnerDefinition definition) {
        displayName = definition.displayName().value();
        state = definition.state();
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
