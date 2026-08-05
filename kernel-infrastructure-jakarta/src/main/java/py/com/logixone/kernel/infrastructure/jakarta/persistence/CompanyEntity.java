package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyStatus;
import py.com.logixone.plugin.api.PluginId;

@Entity
@Table(name = "company", schema = "core")
public class CompanyEntity {

    @Id
    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CompanyStatus status;

    @Column(name = "customization_plugin_id", nullable = false, length = 59)
    private String customizationPluginId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CompanyEntity() {
    }

    private CompanyEntity(Company company) {
        companyId = company.id().value();
        status = company.status();
        customizationPluginId = company.customizationPluginId().value();
        version = company.version();
    }

    static CompanyEntity newEntity(Company company) {
        Objects.requireNonNull(company, "company");
        if (company.version() != 0) {
            throw new IllegalArgumentException("a new company must start at version zero");
        }
        return new CompanyEntity(company);
    }

    Company toDomain() {
        return new Company(
                new CompanyId(companyId),
                status,
                new PluginId(customizationPluginId),
                version);
    }

    boolean hasSameState(Company company) {
        Objects.requireNonNull(company, "company");
        return status == company.status()
                && customizationPluginId.equals(company.customizationPluginId().value());
    }

    void apply(Company company) {
        Objects.requireNonNull(company, "company");
        if (!companyId.equals(company.id().value())) {
            throw new IllegalArgumentException("company identity cannot change");
        }
        status = company.status();
        customizationPluginId = company.customizationPluginId().value();
    }

    long version() {
        return version;
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
