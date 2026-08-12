package py.com.logixone.plugins.purchasing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestReference;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestState;

/** Purchase need whose approval cycle is independent from supplier ordering. */
public final class PurchaseRequest {
    private final CompanyId companyId;
    private final PurchaseRequestId id;
    private final String number;
    private final AppUserId requesterId;
    private final LocalDate requestedOn;
    private List<Line> lines;
    private PurchaseRequestState state = PurchaseRequestState.DRAFT;
    private Optional<Instant> submittedAt = Optional.empty();
    private Optional<AppUserId> decisionActorId = Optional.empty();
    private Optional<Instant> decisionAt = Optional.empty();
    private Optional<String> decisionReason = Optional.empty();
    private long version;

    private PurchaseRequest(
            CompanyId companyId,
            PurchaseRequestId id,
            String number,
            AppUserId requesterId,
            LocalDate requestedOn,
            List<Line> lines) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.id = Objects.requireNonNull(id, "id");
        this.number = PurchasingValues.code(number, "number", 64);
        this.requesterId = Objects.requireNonNull(requesterId, "requesterId");
        this.requestedOn = Objects.requireNonNull(requestedOn, "requestedOn");
        this.lines = validateLines(lines);
    }

    public static PurchaseRequest draft(
            CompanyId companyId,
            PurchaseRequestId id,
            String number,
            AppUserId requesterId,
            LocalDate requestedOn,
            List<Line> lines) {
        return new PurchaseRequest(companyId, id, number, requesterId, requestedOn, lines);
    }

    public static PurchaseRequest restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        PurchaseRequest request = new PurchaseRequest(
                snapshot.companyId(), snapshot.id(), snapshot.number(), snapshot.requesterId(),
                snapshot.requestedOn(), snapshot.lines());
        request.state = Objects.requireNonNull(snapshot.state(), "state");
        request.submittedAt = Objects.requireNonNull(snapshot.submittedAt(), "submittedAt");
        request.decisionActorId = Objects.requireNonNull(snapshot.decisionActorId(), "decisionActorId");
        request.decisionAt = Objects.requireNonNull(snapshot.decisionAt(), "decisionAt");
        request.decisionReason = Objects.requireNonNull(snapshot.decisionReason(), "decisionReason")
                .map(value -> PurchasingValues.text(value, "decisionReason", 240));
        if (snapshot.version() < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        request.version = snapshot.version();
        request.validateStateShape();
        return request;
    }

    public void replaceLines(List<Line> replacement, AppUserId actorId, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireState(PurchaseRequestState.DRAFT);
        if (!requesterId.equals(Objects.requireNonNull(actorId, "actorId"))) {
            throw new IllegalArgumentException("Only the requester can edit a draft request");
        }
        lines = validateLines(replacement);
        version++;
    }

    public void submit(AppUserId actorId, Instant at, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireState(PurchaseRequestState.DRAFT);
        if (!requesterId.equals(Objects.requireNonNull(actorId, "actorId"))) {
            throw new IllegalArgumentException("Only the requester can submit the request");
        }
        state = PurchaseRequestState.SUBMITTED;
        submittedAt = Optional.of(Objects.requireNonNull(at, "at"));
        version++;
    }

    public void approve(AppUserId approverId, Instant at, long expectedVersion) {
        decide(approverId, at, Optional.empty(), PurchaseRequestState.APPROVED, expectedVersion);
    }

    public void reject(AppUserId approverId, Instant at, String reason, long expectedVersion) {
        decide(
                approverId,
                at,
                Optional.of(PurchasingValues.text(reason, "reason", 240)),
                PurchaseRequestState.REJECTED,
                expectedVersion);
    }

    private void decide(
            AppUserId approverId,
            Instant at,
            Optional<String> reason,
            PurchaseRequestState target,
            long expectedVersion) {
        verifyVersion(expectedVersion);
        requireState(PurchaseRequestState.SUBMITTED);
        approverId = Objects.requireNonNull(approverId, "approverId");
        if (requesterId.equals(approverId)) {
            throw new IllegalArgumentException("Requester and approver must be different users");
        }
        state = target;
        decisionActorId = Optional.of(approverId);
        decisionAt = Optional.of(Objects.requireNonNull(at, "at"));
        decisionReason = reason;
        version++;
    }

    public void cancel(AppUserId actorId, Instant at, String reason, long expectedVersion) {
        verifyVersion(expectedVersion);
        if (state != PurchaseRequestState.DRAFT && state != PurchaseRequestState.SUBMITTED) {
            throw new IllegalStateException("Only draft or submitted requests can be cancelled");
        }
        state = PurchaseRequestState.CANCELLED;
        decisionActorId = Optional.of(Objects.requireNonNull(actorId, "actorId"));
        decisionAt = Optional.of(Objects.requireNonNull(at, "at"));
        decisionReason = Optional.of(PurchasingValues.text(reason, "reason", 240));
        version++;
    }

    private static List<Line> validateLines(List<Line> lines) {
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Purchase request must contain at least one line");
        }
        HashSet<PurchaseRequestLineId> ids = new HashSet<>();
        if (lines.stream().anyMatch(line -> !ids.add(Objects.requireNonNull(line, "line").id()))) {
            throw new IllegalArgumentException("Purchase request line ids must be unique");
        }
        return lines;
    }

    private void validateStateShape() {
        boolean hasSubmission = submittedAt.isPresent();
        boolean hasDecision = decisionActorId.isPresent() && decisionAt.isPresent();
        if (decisionActorId.isPresent() != decisionAt.isPresent()) {
            throw new IllegalArgumentException("Decision actor and time must be present together");
        }
        switch (state) {
            case DRAFT -> {
                if (hasSubmission || hasDecision || decisionReason.isPresent()) {
                    throw new IllegalArgumentException("Draft request cannot contain transition metadata");
                }
            }
            case SUBMITTED -> {
                if (!hasSubmission || hasDecision || decisionReason.isPresent()) {
                    throw new IllegalArgumentException("Invalid submitted request metadata");
                }
            }
            case APPROVED -> {
                if (!hasSubmission || !hasDecision || decisionReason.isPresent()
                        || requesterId.equals(decisionActorId.orElseThrow())) {
                    throw new IllegalArgumentException("Invalid approval metadata");
                }
            }
            case REJECTED -> {
                if (!hasSubmission || !hasDecision || decisionReason.isEmpty()
                        || requesterId.equals(decisionActorId.orElseThrow())) {
                    throw new IllegalArgumentException("Invalid rejection metadata");
                }
            }
            case CANCELLED -> {
                if (!hasDecision || decisionReason.isEmpty()) {
                    throw new IllegalArgumentException("Invalid cancellation metadata");
                }
            }
        }
    }

    private void verifyVersion(long expectedVersion) {
        if (expectedVersion != version) {
            throw new ConcurrentPurchasingChangeException(expectedVersion, version);
        }
    }

    private void requireState(PurchaseRequestState expected) {
        if (state != expected) {
            throw new IllegalStateException("Purchase request must be " + expected);
        }
    }

    public PurchaseRequestReference reference() {
        return new PurchaseRequestReference(id, number, state, requestedOn, lines.size(), version);
    }

    public Snapshot snapshot() {
        return new Snapshot(
                companyId, id, number, requesterId, requestedOn, lines, state, submittedAt,
                decisionActorId, decisionAt, decisionReason, version);
    }

    public CompanyId companyId() { return companyId; }
    public PurchaseRequestId id() { return id; }
    public PurchaseRequestState state() { return state; }
    public long version() { return version; }
    public List<Line> lines() { return lines; }

    public record ExpectedPrice(BigDecimal amount, CurrencySnapshot currency) {
        public ExpectedPrice {
            amount = PurchasingValues.amount(amount, "expectedPrice");
            Objects.requireNonNull(currency, "currency");
        }
    }

    public record Line(
            PurchaseRequestLineId id,
            PurchasedItemSnapshot item,
            BigDecimal quantity,
            Optional<ExpectedPrice> expectedPrice) {
        public Line {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(item, "item");
            quantity = PurchasingValues.quantity(quantity, "quantity");
            expectedPrice = Objects.requireNonNull(expectedPrice, "expectedPrice");
        }
    }

    public record Snapshot(
            CompanyId companyId,
            PurchaseRequestId id,
            String number,
            AppUserId requesterId,
            LocalDate requestedOn,
            List<Line> lines,
            PurchaseRequestState state,
            Optional<Instant> submittedAt,
            Optional<AppUserId> decisionActorId,
            Optional<Instant> decisionAt,
            Optional<String> decisionReason,
            long version) {
        public Snapshot {
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        }
    }
}
