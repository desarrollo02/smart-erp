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
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.UserStatus;

@Entity
@Table(name = "app_user", schema = "core")
public class AppUserEntity {

    @Id
    @Column(name = "app_user_id", nullable = false, updatable = false)
    private UUID appUserId;

    @Column(name = "issuer", nullable = false, length = 2048, updatable = false)
    private String issuer;

    @Column(name = "subject", nullable = false, length = 255, updatable = false)
    private String subject;

    @Column(name = "display_name", length = 160)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private UserStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUserEntity() {
    }

    private AppUserEntity(AppUser user) {
        appUserId = user.id().value();
        issuer = user.externalIdentity().issuer();
        subject = user.externalIdentity().subject();
        displayName = user.displayName().orElse(null);
        status = user.status();
        version = user.version();
    }

    static AppUserEntity newEntity(AppUser user) {
        Objects.requireNonNull(user, "user");
        if (user.version() != 0) {
            throw new IllegalArgumentException("a new application user must start at version zero");
        }
        return new AppUserEntity(user);
    }

    AppUser toDomain() {
        return new AppUser(
                new AppUserId(appUserId),
                new ExternalIdentity(issuer, subject),
                Optional.ofNullable(displayName),
                status,
                version);
    }

    boolean hasSameState(AppUser user) {
        Objects.requireNonNull(user, "user");
        return issuer.equals(user.externalIdentity().issuer())
                && subject.equals(user.externalIdentity().subject())
                && Objects.equals(displayName, user.displayName().orElse(null))
                && status == user.status();
    }

    void apply(AppUser user) {
        Objects.requireNonNull(user, "user");
        if (!appUserId.equals(user.id().value())
                || !issuer.equals(user.externalIdentity().issuer())
                || !subject.equals(user.externalIdentity().subject())) {
            throw new IllegalArgumentException("application user identity cannot change");
        }
        displayName = user.displayName().orElse(null);
        status = user.status();
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
