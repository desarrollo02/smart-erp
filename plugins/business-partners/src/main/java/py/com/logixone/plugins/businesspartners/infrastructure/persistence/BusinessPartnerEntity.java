package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerKind;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerName;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerSnapshot;

@Entity
@Table(name = "business_partner", schema = BusinessPartnersPersistenceNames.SCHEMA)
public class BusinessPartnerEntity {

    @EmbeddedId
    private BusinessPartnerEntityId id;
    @Column(name = "code", nullable = false, length = 64)
    private String code;
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 24)
    private BusinessPartnerKind kind;
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;
    @Column(name = "legal_name", length = 200)
    private String legalName;
    @Column(name = "trade_name", length = 200)
    private String tradeName;
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

    protected BusinessPartnerEntity() {
    }

    static BusinessPartnerEntity from(BusinessPartnerSnapshot snapshot) {
        BusinessPartnerEntity entity = new BusinessPartnerEntity();
        entity.id = new BusinessPartnerEntityId(
                snapshot.companyId().value(), snapshot.id().value());
        entity.apply(snapshot);
        entity.version = snapshot.version();
        return entity;
    }

    void apply(BusinessPartnerSnapshot snapshot) {
        code = snapshot.code().value();
        kind = snapshot.kind();
        displayName = snapshot.displayName().value();
        legalName = snapshot.legalName().map(BusinessPartnerName::value).orElse(null);
        tradeName = snapshot.tradeName().map(BusinessPartnerName::value).orElse(null);
        state = snapshot.state();
        updatedAt = Instant.now();
    }

    long version() {
        return version;
    }

    BusinessPartnerSnapshot rootSnapshot() {
        return new BusinessPartnerSnapshot(
                new CompanyId(id.companyId()),
                new BusinessPartnerId(id.businessPartnerId()),
                new BusinessPartnerCode(code),
                kind,
                new BusinessPartnerName(displayName),
                Optional.ofNullable(legalName).map(BusinessPartnerName::new),
                Optional.ofNullable(tradeName).map(BusinessPartnerName::new),
                state,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                version);
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
}
