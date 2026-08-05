package py.com.logixone.kernel.infrastructure.jakarta.persistence;

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
import java.util.Objects;
import py.com.logixone.kernel.domain.security.CompanyMembership;
import py.com.logixone.kernel.domain.security.MembershipStatus;

@Entity
@Table(name = "company_membership", schema = "core")
public class CompanyMembershipEntity {

    @EmbeddedId
    private CompanyMembershipKey id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private MembershipStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CompanyMembershipEntity() {
    }

    private CompanyMembershipEntity(CompanyMembership membership) {
        id = new CompanyMembershipKey(membership.userId(), membership.companyId());
        status = membership.status();
        version = membership.version();
    }

    static CompanyMembershipEntity newEntity(CompanyMembership membership) {
        Objects.requireNonNull(membership, "membership");
        if (membership.version() != 0) {
            throw new IllegalArgumentException("a new membership must start at version zero");
        }
        return new CompanyMembershipEntity(membership);
    }

    CompanyMembership toDomain() {
        return new CompanyMembership(id.userId(), id.companyId(), status, version);
    }

    boolean hasSameState(CompanyMembership membership) {
        return status == Objects.requireNonNull(membership, "membership").status();
    }

    void apply(CompanyMembership membership) {
        Objects.requireNonNull(membership, "membership");
        if (!id.equals(new CompanyMembershipKey(membership.userId(), membership.companyId()))) {
            throw new IllegalArgumentException("membership identity cannot change");
        }
        status = membership.status();
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
