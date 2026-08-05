package py.com.logixone.plugins.inventory.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockMovementDirection;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockMovementLine;
import py.com.logixone.plugins.inventory.api.StockMovementReference;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;
import py.com.logixone.plugins.inventory.api.StockMovementType;

/** Immutable ledger entry; corrections are represented by linked reversals. */
public final class StockMovement {
    private final CompanyId companyId;
    private final StockMovementId id;
    private final StockMovementRequest request;
    private final Instant postedAt;

    private StockMovement(
            CompanyId companyId, StockMovementId id, StockMovementRequest request, Instant postedAt) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.id = Objects.requireNonNull(id, "id");
        this.request = Objects.requireNonNull(request, "request");
        this.postedAt = Objects.requireNonNull(postedAt, "postedAt");
        validateShape(request);
    }

    public static StockMovement post(
            CompanyId companyId, StockMovementId id, StockMovementRequest request, Instant postedAt) {
        return new StockMovement(companyId, id, request, postedAt);
    }

    public static StockMovement restore(StockMovementSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new StockMovement(snapshot.companyId(), snapshot.id(), snapshot.request(), snapshot.postedAt());
    }

    private static void validateShape(StockMovementRequest request) {
        List<StockMovementLine> lines = request.lines();
        switch (request.type()) {
            case RECEIPT -> requireAll(lines, StockMovementDirection.INCREASE, "RECEIPT");
            case ISSUE -> requireAll(lines, StockMovementDirection.DECREASE, "ISSUE");
            case TRANSFER -> validateTransfer(lines);
            case ADJUSTMENT, REVERSAL -> {
                // Both directions are valid; reason and reversal linkage are checked by the public request.
            }
        }
    }

    private static void requireAll(
            List<StockMovementLine> lines, StockMovementDirection direction, String type) {
        if (lines.stream().anyMatch(line -> line.direction() != direction)) {
            throw new IllegalArgumentException(type + " movement has an invalid direction");
        }
    }

    private static void validateTransfer(List<StockMovementLine> lines) {
        if (lines.size() != 2) {
            throw new IllegalArgumentException("TRANSFER must contain exactly one debit and one credit");
        }
        StockMovementLine debit = lines.stream()
                .filter(line -> line.direction() == StockMovementDirection.DECREASE)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("TRANSFER debit is missing"));
        StockMovementLine credit = lines.stream()
                .filter(line -> line.direction() == StockMovementDirection.INCREASE)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("TRANSFER credit is missing"));
        StockKey source = debit.key();
        StockKey target = credit.key();
        if (!source.inventoryItemId().equals(target.inventoryItemId())
                || !source.lotCode().equals(target.lotCode())
                || !source.serialNumber().equals(target.serialNumber())
                || !source.expiryDate().equals(target.expiryDate())
                || source.condition() != target.condition()
                || (source.warehouseId().equals(target.warehouseId())
                    && source.locationId().equals(target.locationId()))
                || debit.quantity().baseQuantity().compareTo(credit.quantity().baseQuantity()) != 0
                || !debit.quantity().baseUnitCode().equals(credit.quantity().baseUnitCode())) {
            throw new IllegalArgumentException(
                    "TRANSFER must preserve item, trace dimensions, condition, base unit and quantity between distinct locations");
        }
    }

    public StockMovementReference reference() {
        return new StockMovementReference(id, request.type(), postedAt, request.lines(), request.reversalOf());
    }

    public CompanyId companyId() { return companyId; }
    public StockMovementId id() { return id; }
    public StockMovementType type() { return request.type(); }
    public String reasonCode() { return request.reasonCode(); }
    public String idempotencyKey() { return request.idempotencyKey(); }
    public Instant postedAt() { return postedAt; }
    public List<StockMovementLine> lines() { return request.lines(); }
    public StockMovementSnapshot snapshot(Map<py.com.logixone.plugins.inventory.api.InventoryItemId, InventoryItem> items) {
        Objects.requireNonNull(items, "items");
        List<StockMovementLineSnapshot> lineSnapshots = new java.util.ArrayList<>();
        for (int index = 0; index < request.lines().size(); index++) {
            StockMovementLine line = request.lines().get(index);
            InventoryItem item = Objects.requireNonNull(
                    items.get(line.key().inventoryItemId()), "inventory item snapshot");
            if (!companyId.equals(item.companyId())) {
                throw new IllegalArgumentException("Movement catalog snapshot belongs to another company");
            }
            lineSnapshots.add(new StockMovementLineSnapshot(
                    index + 1, line, item.catalogItemId(), item.catalogCode(), item.catalogName()));
        }
        return new StockMovementSnapshot(companyId, id, request, postedAt, lineSnapshots);
    }
}
