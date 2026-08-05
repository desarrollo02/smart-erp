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
import py.com.logixone.kernel.domain.security.CompanyRole;
import py.com.logixone.kernel.domain.security.RoleCode;
import py.com.logixone.kernel.domain.security.RoleId;
import py.com.logixone.kernel.domain.security.RoleStatus;

@Entity
@Table(name = "security_role", schema = "core")
public class SecurityRoleEntity {

    @Id
    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID roleId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "role_code", nullable = false, length = 128, updatable = false)
    private String roleCode;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private RoleStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SecurityRoleEntity() {
    }

    private SecurityRoleEntity(CompanyRole role) {
        roleId = role.id().value();
        companyId = role.companyId().value();
        roleCode = role.code().value();
        displayName = role.displayName();
        status = role.status();
        version = role.version();
    }

    static SecurityRoleEntity newEntity(CompanyRole role) {
        Objects.requireNonNull(role, "role");
        if (role.version() != 0) {
            throw new IllegalArgumentException("a new role must start at version zero");
        }
        return new SecurityRoleEntity(role);
    }

    CompanyRole toDomain() {
        return new CompanyRole(
                new RoleId(roleId),
                new CompanyId(companyId),
                new RoleCode(roleCode),
                displayName,
                status,
                version);
    }

    boolean hasSameState(CompanyRole role) {
        Objects.requireNonNull(role, "role");
        return companyId.equals(role.companyId().value())
                && roleCode.equals(role.code().value())
                && displayName.equals(role.displayName())
                && status == role.status();
    }

    void apply(CompanyRole role) {
        Objects.requireNonNull(role, "role");
        if (!roleId.equals(role.id().value())
                || !companyId.equals(role.companyId().value())
                || !roleCode.equals(role.code().value())) {
            throw new IllegalArgumentException("role identity and company cannot change");
        }
        displayName = role.displayName();
        status = role.status();
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
