package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestState;
import py.com.logixone.plugins.purchasing.domain.PurchaseRequest;

@Entity
@Table(name = "purchase_request", schema = PurchasingPersistenceNames.SCHEMA)
@IdClass(PurchaseRequestEntity.Key.class)
public class PurchaseRequestEntity {
    @Id @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;
    @Id @Column(name = "purchase_request_id", nullable = false, updatable = false)
    private UUID purchaseRequestId;
    @Column(name = "request_number", nullable = false, length = 64, updatable = false)
    private String number;
    @Column(name = "requester_id", nullable = false, updatable = false)
    private UUID requesterId;
    @Column(name = "requested_on", nullable = false, updatable = false)
    private LocalDate requestedOn;
    @Enumerated(EnumType.STRING) @Column(name = "request_state", nullable = false, length = 24)
    private PurchaseRequestState state;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(name = "decision_actor_id")
    private UUID decisionActorId;
    @Column(name = "decision_at")
    private Instant decisionAt;
    @Column(name = "decision_reason", length = 240)
    private String decisionReason;
    @Version @Column(name = "entity_version", nullable = false)
    private long version;

    protected PurchaseRequestEntity() {
    }

    static PurchaseRequestEntity from(PurchaseRequest.Snapshot snapshot) {
        PurchaseRequestEntity entity = new PurchaseRequestEntity();
        entity.companyId = snapshot.companyId().value();
        entity.purchaseRequestId = snapshot.id().value();
        entity.number = snapshot.number();
        entity.requesterId = snapshot.requesterId().value();
        entity.requestedOn = snapshot.requestedOn();
        entity.version = snapshot.version();
        entity.apply(snapshot);
        return entity;
    }

    void apply(PurchaseRequest.Snapshot snapshot) {
        state = snapshot.state();
        submittedAt = snapshot.submittedAt().orElse(null);
        decisionActorId = snapshot.decisionActorId().map(AppUserId::value).orElse(null);
        decisionAt = snapshot.decisionAt().orElse(null);
        decisionReason = snapshot.decisionReason().orElse(null);
    }

    PurchaseRequest.Snapshot snapshot(List<PurchaseRequest.Line> lines) {
        return new PurchaseRequest.Snapshot(
                new CompanyId(companyId), new PurchaseRequestId(purchaseRequestId), number,
                new AppUserId(requesterId), requestedOn, lines, state,
                Optional.ofNullable(submittedAt),
                Optional.ofNullable(decisionActorId).map(AppUserId::new),
                Optional.ofNullable(decisionAt), Optional.ofNullable(decisionReason), version);
    }

    UUID companyId() { return companyId; }
    UUID id() { return purchaseRequestId; }
    PurchaseRequestState state() { return state; }
    long version() { return version; }

    public static final class Key implements Serializable {
        public UUID companyId;
        public UUID purchaseRequestId;
        public Key() { }
        Key(UUID companyId, UUID purchaseRequestId) { this.companyId = companyId; this.purchaseRequestId = purchaseRequestId; }
        @Override public boolean equals(Object other) { return this == other || other instanceof Key that && Objects.equals(companyId, that.companyId) && Objects.equals(purchaseRequestId, that.purchaseRequestId); }
        @Override public int hashCode() { return Objects.hash(companyId, purchaseRequestId); }
    }
}
