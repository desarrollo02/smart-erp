package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import py.com.logixone.kernel.domain.security.system.SystemRolePermissionGrant;

@Entity
@Table(name = "system_role_permission", schema = "core")
public class SystemRolePermissionEntity {

    @EmbeddedId
    private SystemRolePermissionKey id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SystemRolePermissionEntity() {
    }

    private SystemRolePermissionEntity(SystemRolePermissionGrant grant) {
        id = new SystemRolePermissionKey(grant.roleId(), grant.permission());
    }

    static SystemRolePermissionEntity newEntity(SystemRolePermissionGrant grant) {
        return new SystemRolePermissionEntity(Objects.requireNonNull(grant, "grant"));
    }

    SystemRolePermissionGrant toDomain() {
        return new SystemRolePermissionGrant(id.roleId(), id.permission());
    }

    @PrePersist
    void initializeTimestamp() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
