package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import py.com.logixone.kernel.domain.security.RolePermissionGrant;

@Entity
@Table(name = "role_permission", schema = "core")
public class RolePermissionEntity {

    @EmbeddedId
    private RolePermissionKey id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RolePermissionEntity() {
    }

    private RolePermissionEntity(RolePermissionGrant grant) {
        id = new RolePermissionKey(grant.companyId(), grant.roleId(), grant.permissionId());
    }

    static RolePermissionEntity newEntity(RolePermissionGrant grant) {
        return new RolePermissionEntity(Objects.requireNonNull(grant, "grant"));
    }

    RolePermissionGrant toDomain() {
        return new RolePermissionGrant(id.companyId(), id.roleId(), id.permissionId());
    }

    @PrePersist
    void initializeTimestamp() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
