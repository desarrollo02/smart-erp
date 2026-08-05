package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import py.com.logixone.kernel.domain.security.MembershipRoleAssignment;

@Entity
@Table(name = "membership_role", schema = "core")
public class MembershipRoleEntity {

    @EmbeddedId
    private MembershipRoleKey id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MembershipRoleEntity() {
    }

    private MembershipRoleEntity(MembershipRoleAssignment assignment) {
        id = new MembershipRoleKey(
                assignment.userId(), assignment.companyId(), assignment.roleId());
    }

    static MembershipRoleEntity newEntity(MembershipRoleAssignment assignment) {
        return new MembershipRoleEntity(Objects.requireNonNull(assignment, "assignment"));
    }

    MembershipRoleAssignment toDomain() {
        return new MembershipRoleAssignment(id.userId(), id.companyId(), id.roleId());
    }

    @PrePersist
    void initializeTimestamp() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
