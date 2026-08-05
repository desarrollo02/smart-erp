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
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.kernel.domain.company.PluginActivationState;

@Entity
@Table(name = "company_plugin_activation", schema = "core")
public class PluginActivationEntity {

    @EmbeddedId
    private PluginActivationKey id;

    @Enumerated(EnumType.STRING)
    @Column(name = "desired_state", nullable = false, length = 16)
    private PluginActivationState desiredState;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PluginActivationEntity() {
    }

    private PluginActivationEntity(PluginActivationDecision decision) {
        id = new PluginActivationKey(decision.companyId(), decision.pluginId());
        desiredState = decision.desiredState();
        version = decision.version();
    }

    static PluginActivationEntity newEntity(PluginActivationDecision decision) {
        Objects.requireNonNull(decision, "decision");
        if (decision.version() != 0) {
            throw new IllegalArgumentException("a new activation decision must start at version zero");
        }
        return new PluginActivationEntity(decision);
    }

    PluginActivationDecision toDomain() {
        return new PluginActivationDecision(
                id.companyId(),
                id.pluginId(),
                desiredState,
                version);
    }

    boolean hasSameState(PluginActivationDecision decision) {
        return desiredState == Objects.requireNonNull(decision, "decision").desiredState();
    }

    void apply(PluginActivationDecision decision) {
        Objects.requireNonNull(decision, "decision");
        PluginActivationKey expectedId = new PluginActivationKey(decision.companyId(), decision.pluginId());
        if (!id.equals(expectedId)) {
            throw new IllegalArgumentException("activation identity cannot change");
        }
        desiredState = decision.desiredState();
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
