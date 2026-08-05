package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import py.com.logixone.kernel.domain.security.system.AppUserSystemRoleAssignment;

@Entity
@Table(name = "app_user_system_role", schema = "core")
public class AppUserSystemRoleEntity {

    @EmbeddedId
    private AppUserSystemRoleKey id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AppUserSystemRoleEntity() {
    }

    private AppUserSystemRoleEntity(AppUserSystemRoleAssignment assignment) {
        id = new AppUserSystemRoleKey(assignment.userId(), assignment.roleId());
    }

    static AppUserSystemRoleEntity newEntity(AppUserSystemRoleAssignment assignment) {
        return new AppUserSystemRoleEntity(Objects.requireNonNull(assignment, "assignment"));
    }

    AppUserSystemRoleAssignment toDomain() {
        return new AppUserSystemRoleAssignment(id.userId(), id.roleId());
    }

    @PrePersist
    void initializeTimestamp() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
