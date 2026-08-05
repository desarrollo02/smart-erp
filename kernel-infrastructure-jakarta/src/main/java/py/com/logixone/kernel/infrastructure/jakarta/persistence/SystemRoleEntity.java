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
import py.com.logixone.kernel.domain.security.system.SystemRole;
import py.com.logixone.kernel.domain.security.system.SystemRoleCode;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;
import py.com.logixone.kernel.domain.security.system.SystemRoleStatus;

@Entity
@Table(name = "system_role", schema = "core")
public class SystemRoleEntity {

    @Id
    @Column(name = "system_role_id", nullable = false, updatable = false)
    private UUID systemRoleId;

    @Column(name = "role_code", nullable = false, length = 128, updatable = false)
    private String roleCode;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SystemRoleStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SystemRoleEntity() {
    }

    private SystemRoleEntity(SystemRole role) {
        systemRoleId = role.id().value();
        roleCode = role.code().value();
        displayName = role.displayName();
        status = role.status();
        version = role.version();
    }

    static SystemRoleEntity newEntity(SystemRole role) {
        Objects.requireNonNull(role, "role");
        if (role.version() != 0) {
            throw new IllegalArgumentException("a new system role must start at version zero");
        }
        return new SystemRoleEntity(role);
    }

    SystemRole toDomain() {
        return new SystemRole(new SystemRoleId(systemRoleId), new SystemRoleCode(roleCode),
                displayName, status, version);
    }

    boolean hasSameState(SystemRole role) {
        Objects.requireNonNull(role, "role");
        return roleCode.equals(role.code().value())
                && displayName.equals(role.displayName())
                && status == role.status();
    }

    void apply(SystemRole role) {
        Objects.requireNonNull(role, "role");
        if (!systemRoleId.equals(role.id().value()) || !roleCode.equals(role.code().value())) {
            throw new IllegalArgumentException("system role identity and code cannot change");
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
