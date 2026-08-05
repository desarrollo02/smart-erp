package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionRevision;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerName;

@Entity
@Table(name = "business_partner_definition_revision", schema = BusinessPartnersPersistenceNames.SCHEMA)
public class BusinessPartnerDefinitionRevisionEntity {

    @EmbeddedId
    private BusinessPartnerDefinitionRevisionEntityId id;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private BusinessPartnerState state;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected BusinessPartnerDefinitionRevisionEntity() {
    }

    static BusinessPartnerDefinitionRevisionEntity from(BusinessPartnerDefinition definition) {
        BusinessPartnerDefinitionRevisionEntity entity =
                new BusinessPartnerDefinitionRevisionEntity();
        entity.id = new BusinessPartnerDefinitionRevisionEntityId(
                definition.companyId().value(), definition.kind(),
                definition.code().value(), definition.version());
        entity.displayName = definition.displayName().value();
        entity.state = definition.state();
        return entity;
    }

    BusinessPartnerDefinitionRevision toDomain() {
        return new BusinessPartnerDefinitionRevision(
                new CompanyId(id.companyId()), id.kind(),
                new BusinessPartnerAttributeCode(id.code()),
                new BusinessPartnerName(displayName), state, id.version(), changedAt);
    }

    @PrePersist
    void initializeTimestamp() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
