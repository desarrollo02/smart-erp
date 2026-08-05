package py.com.logixone.plugins.inventory.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.MovementQuantity;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockMovementDirection;
import py.com.logixone.plugins.inventory.api.StockMovementLine;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;
import py.com.logixone.plugins.inventory.api.StockMovementType;
import py.com.logixone.plugins.inventory.api.StockSourceReference;
import py.com.logixone.plugins.inventory.application.command.InventoryCommands;
import py.com.logixone.plugins.inventory.application.port.InventoryBalanceRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryIdGenerator;
import py.com.logixone.plugins.inventory.application.port.InventoryItemRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceException;
import py.com.logixone.plugins.inventory.application.port.StockCountRepository;
import py.com.logixone.plugins.inventory.application.port.WarehouseRepository;
import py.com.logixone.plugins.inventory.domain.ConcurrentInventoryChangeException;
import py.com.logixone.plugins.inventory.domain.InventoryItem;
import py.com.logixone.plugins.inventory.domain.StockCount;
import py.com.logixone.plugins.inventory.domain.StockCountLineSnapshot;
import py.com.logixone.plugins.inventory.domain.StockCountSnapshot;
import py.com.logixone.plugins.inventory.domain.StockCountState;
import py.com.logixone.plugins.inventory.domain.Warehouse;

/** Physical count workflow; posting creates an immutable adjustment movement. */
public final class InventoryCountService {
    private static final String COUNT = "stock_count";

    private final WarehouseRepository warehouses;
    private final InventoryItemRepository items;
    private final InventoryBalanceRepository balances;
    private final StockCountRepository counts;
    private final InventoryMovementService movementService;
    private final InventoryIdGenerator ids;
    private final InventoryAuditRecorder audit;

    public InventoryCountService(
            WarehouseRepository warehouses,
            InventoryItemRepository items,
            InventoryBalanceRepository balances,
            StockCountRepository counts,
            InventoryMovementService movementService,
            InventoryIdGenerator ids,
            TechnicalAudit technicalAudit,
            java.time.Clock clock) {
        this.warehouses = Objects.requireNonNull(warehouses, "warehouses");
        this.items = Objects.requireNonNull(items, "items");
        this.balances = Objects.requireNonNull(balances, "balances");
        this.counts = Objects.requireNonNull(counts, "counts");
        this.movementService = Objects.requireNonNull(movementService, "movementService");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.audit = new InventoryAuditRecorder(technicalAudit, clock);
    }

    public InventoryOperationResult<StockCountSnapshot> draft(
            InventoryOperationContext context, InventoryCommands.DraftCount command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, false)) {
            return denied(context, "DRAFT_STOCK_COUNT", Optional.empty(), Optional.empty(), false);
        }
        try {
            validateScope(company(context), command.scope());
            StockCount count = StockCount.draft(company(context), ids.nextCountId(), command.scope());
            counts.insert(count);
            audit.changed(context, InventoryPermissions.COUNTS_MANAGE,
                    "DRAFT_STOCK_COUNT", COUNT, count.id().toString(), Optional.empty(), count.version());
            return InventoryOperationResult.success(count.snapshot());
        } catch (InventoryPersistenceException failure) {
            return rejected(context, "DRAFT_STOCK_COUNT", Optional.empty(), Optional.empty(),
                    InventoryApplicationSupport.map(failure.code()), false);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return rejected(context, "DRAFT_STOCK_COUNT", Optional.empty(), Optional.empty(),
                    InventoryResultCode.INVALID_OPERATION, false);
        }
    }

    public InventoryOperationResult<StockCountSnapshot> addLine(
            InventoryOperationContext context, InventoryCommands.AddCountLine command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, false)) {
            return denied(context, "ADD_STOCK_COUNT_LINE",
                    Optional.of(command.countId().toString()), Optional.of(command.expectedVersion()), false);
        }
        StockCount count = find(context, command.countId(), command.expectedVersion(),
                "ADD_STOCK_COUNT_LINE", false);
        if (count == null) {
            return InventoryOperationResult.failure(InventoryResultCode.NOT_FOUND);
        }
        try {
            validateKey(company(context), command.key());
            BigDecimal theoretical = balances.find(company(context), command.key())
                    .map(balance -> balance.physicalQuantity()).orElse(BigDecimal.ZERO);
            long previousVersion = count.version();
            count.addLine(command.key(), theoretical, command.expectedVersion());
            counts.update(count, previousVersion);
            changed(context, "ADD_STOCK_COUNT_LINE", count, previousVersion, false);
            return InventoryOperationResult.success(count.snapshot());
        } catch (ConcurrentInventoryChangeException failure) {
            return conflict(context, "ADD_STOCK_COUNT_LINE", count, false);
        } catch (InventoryPersistenceException failure) {
            return persistence(context, "ADD_STOCK_COUNT_LINE", count, failure, false);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return invalid(context, "ADD_STOCK_COUNT_LINE", count, false);
        }
    }

    public InventoryOperationResult<StockCountSnapshot> start(
            InventoryOperationContext context, InventoryCommands.CountTransition command) {
        return transition(context, command, "START_STOCK_COUNT", StockCountState.COUNTING,
                false, StockCount::start);
    }

    public InventoryOperationResult<StockCountSnapshot> record(
            InventoryOperationContext context, InventoryCommands.RecordCount command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, false)) {
            return denied(context, "RECORD_STOCK_COUNT",
                    Optional.of(command.countId().toString()), Optional.of(command.expectedVersion()), false);
        }
        StockCount count = find(context, command.countId(), command.expectedVersion(),
                "RECORD_STOCK_COUNT", false);
        if (count == null) {
            return InventoryOperationResult.failure(InventoryResultCode.NOT_FOUND);
        }
        try {
            long previousVersion = count.version();
            count.record(command.key(), command.countedQuantity(), command.expectedVersion());
            counts.update(count, previousVersion);
            changed(context, "RECORD_STOCK_COUNT", count, previousVersion, false);
            return InventoryOperationResult.success(count.snapshot());
        } catch (ConcurrentInventoryChangeException failure) {
            return conflict(context, "RECORD_STOCK_COUNT", count, false);
        } catch (InventoryPersistenceException failure) {
            return persistence(context, "RECORD_STOCK_COUNT", count, failure, false);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return invalid(context, "RECORD_STOCK_COUNT", count, false);
        }
    }

    public InventoryOperationResult<StockCountSnapshot> review(
            InventoryOperationContext context, InventoryCommands.CountTransition command) {
        return transition(context, command, "REVIEW_STOCK_COUNT", StockCountState.REVIEW,
                false, StockCount::sendToReview);
    }

    public InventoryOperationResult<StockCountSnapshot> cancel(
            InventoryOperationContext context, InventoryCommands.CountTransition command) {
        return transition(context, command, "CANCEL_STOCK_COUNT", StockCountState.CANCELLED,
                false, StockCount::cancel);
    }

    public InventoryOperationResult<StockCountSnapshot> post(
            InventoryOperationContext context, InventoryCommands.CountTransition command) {
        Objects.requireNonNull(command, "command");
        String operation = "POST_STOCK_COUNT";
        if (!authorized(context, true)) {
            return denied(context, operation, Optional.of(command.countId().toString()),
                    Optional.of(command.expectedVersion()), true);
        }
        StockCount count = find(context, command.countId(), command.expectedVersion(), operation, true);
        if (count == null) {
            return InventoryOperationResult.failure(InventoryResultCode.NOT_FOUND);
        }
        if (count.state() == StockCountState.POSTED
                && count.version() == command.expectedVersion() + 1) {
            var retry = postAdjustment(context, count.snapshot(), command.expectedVersion());
            if (!retry.successful()) {
                return InventoryOperationResult.failure(retry.code());
            }
            audit.unchanged(context, InventoryPermissions.ADJUSTMENTS_POST,
                    operation, COUNT, count.id().toString(), count.version());
            return InventoryOperationResult.success(count.snapshot());
        }
        try {
            long previousVersion = count.version();
            count.post(command.expectedVersion());
            var movement = postAdjustment(context, count.snapshot(), previousVersion);
            if (!movement.successful()) {
                return InventoryOperationResult.failure(movement.code());
            }
            counts.update(count, previousVersion);
            changed(context, operation, count, previousVersion, true);
            return InventoryOperationResult.success(count.snapshot());
        } catch (ConcurrentInventoryChangeException failure) {
            return conflict(context, operation, count, true);
        } catch (InventoryPersistenceException failure) {
            return persistence(context, operation, count, failure, true);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return invalid(context, operation, count, true);
        }
    }

    private InventoryOperationResult<?> postAdjustment(
            InventoryOperationContext context, StockCountSnapshot snapshot, long sourceVersion) {
        List<StockMovementLine> lines = adjustmentLines(snapshot);
        if (lines.isEmpty()) {
            return InventoryOperationResult.success(snapshot);
        }
        StockMovementRequest request = new StockMovementRequest(
                StockMovementType.ADJUSTMENT,
                "PHYSICAL_COUNT",
                new StockSourceReference("STOCK_COUNT", snapshot.id().toString()),
                "stock-count:" + snapshot.id() + ":v" + sourceVersion,
                lines,
                Optional.empty());
        return movementService.postCountAdjustment(context, request);
    }

    private List<StockMovementLine> adjustmentLines(StockCountSnapshot snapshot) {
        List<StockMovementLine> result = new ArrayList<>();
        for (StockCountLineSnapshot line : snapshot.lines()) {
            BigDecimal difference = line.countedQuantity().orElseThrow()
                    .subtract(line.theoreticalQuantity());
            if (difference.signum() == 0) {
                continue;
            }
            InventoryItem item = items.findById(snapshot.companyId(), line.key().inventoryItemId())
                    .orElseThrow(() -> new IllegalStateException("Count item no longer exists"));
            BigDecimal amount = difference.abs();
            result.add(new StockMovementLine(
                    line.key(),
                    difference.signum() > 0
                            ? StockMovementDirection.INCREASE : StockMovementDirection.DECREASE,
                    new MovementQuantity(item.baseUnitCode(), amount, item.baseUnitCode(),
                            BigDecimal.ONE, amount, item.catalogItemVersion())));
        }
        return result;
    }

    private InventoryOperationResult<StockCountSnapshot> transition(
            InventoryOperationContext context,
            InventoryCommands.CountTransition command,
            String operation,
            StockCountState idempotentState,
            boolean adjustmentPermission,
            CountMutation mutation) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, adjustmentPermission)) {
            return denied(context, operation, Optional.of(command.countId().toString()),
                    Optional.of(command.expectedVersion()), adjustmentPermission);
        }
        StockCount count = find(context, command.countId(), command.expectedVersion(),
                operation, adjustmentPermission);
        if (count == null) {
            return InventoryOperationResult.failure(InventoryResultCode.NOT_FOUND);
        }
        if (count.state() == idempotentState && count.version() == command.expectedVersion() + 1) {
            audit.unchanged(context, permission(adjustmentPermission), operation,
                    COUNT, count.id().toString(), count.version());
            return InventoryOperationResult.success(count.snapshot());
        }
        try {
            if (idempotentState == StockCountState.COUNTING) {
                verifyTheoreticalBalances(count.snapshot());
            }
            long previousVersion = count.version();
            mutation.apply(count, command.expectedVersion());
            counts.update(count, previousVersion);
            changed(context, operation, count, previousVersion, adjustmentPermission);
            return InventoryOperationResult.success(count.snapshot());
        } catch (ConcurrentInventoryChangeException failure) {
            return conflict(context, operation, count, adjustmentPermission);
        } catch (InventoryPersistenceException failure) {
            return persistence(context, operation, count, failure, adjustmentPermission);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return invalid(context, operation, count, adjustmentPermission);
        }
    }

    private void verifyTheoreticalBalances(StockCountSnapshot snapshot) {
        for (StockCountLineSnapshot line : snapshot.lines()) {
            BigDecimal current = balances.find(snapshot.companyId(), line.key())
                    .map(balance -> balance.physicalQuantity()).orElse(BigDecimal.ZERO);
            if (current.compareTo(line.theoreticalQuantity()) != 0) {
                throw new ConcurrentInventoryChangeException(snapshot.version(), snapshot.version() + 1);
            }
        }
    }

    private void validateScope(CompanyId companyId, py.com.logixone.plugins.inventory.domain.StockCountScope scope) {
        Warehouse warehouse = warehouses.findById(companyId, scope.warehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse does not exist"));
        if (!warehouse.active()) {
            throw new IllegalStateException("Warehouse is inactive");
        }
        scope.locationId().ifPresent(id -> {
            var location = warehouse.locations().get(id);
            if (location == null || !location.active()) {
                throw new IllegalArgumentException("Count location is invalid");
            }
        });
    }

    private void validateKey(CompanyId companyId, StockKey key) {
        InventoryItem item = items.findById(companyId, key.inventoryItemId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory item does not exist"));
        if (!item.active()) {
            throw new IllegalStateException("Inventory item is inactive");
        }
        item.validateKey(key);
        Warehouse warehouse = warehouses.findById(companyId, key.warehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse does not exist"));
        var location = warehouse.locations().get(key.locationId());
        if (!warehouse.active() || location == null || !location.active()) {
            throw new IllegalArgumentException("Stock location is invalid");
        }
    }

    private StockCount find(
            InventoryOperationContext context,
            py.com.logixone.plugins.inventory.api.StockCountId id,
            long version,
            String operation,
            boolean adjustmentPermission) {
        StockCount count = counts.findById(company(context), id).orElse(null);
        if (count == null) {
            rejected(context, operation, Optional.of(id.toString()), Optional.of(version),
                    InventoryResultCode.NOT_FOUND, adjustmentPermission);
        }
        return count;
    }

    private static boolean authorized(
            InventoryOperationContext context, boolean adjustmentPermission) {
        return InventoryApplicationSupport.authorized(context, permission(adjustmentPermission));
    }

    private static py.com.logixone.plugin.api.ContributionId permission(boolean adjustmentPermission) {
        return adjustmentPermission
                ? InventoryPermissions.ADJUSTMENTS_POST : InventoryPermissions.COUNTS_MANAGE;
    }

    private void changed(
            InventoryOperationContext context,
            String operation,
            StockCount count,
            long previousVersion,
            boolean adjustmentPermission) {
        audit.changed(context, permission(adjustmentPermission), operation, COUNT,
                count.id().toString(), Optional.of(previousVersion), count.version());
    }

    private InventoryOperationResult<StockCountSnapshot> conflict(
            InventoryOperationContext context, String operation, StockCount count,
            boolean adjustmentPermission) {
        return rejected(context, operation, Optional.of(count.id().toString()),
                Optional.of(count.version()), InventoryResultCode.VERSION_CONFLICT, adjustmentPermission);
    }

    private InventoryOperationResult<StockCountSnapshot> persistence(
            InventoryOperationContext context, String operation, StockCount count,
            InventoryPersistenceException failure, boolean adjustmentPermission) {
        return rejected(context, operation, Optional.of(count.id().toString()),
                Optional.of(count.version()), InventoryApplicationSupport.map(failure.code()),
                adjustmentPermission);
    }

    private InventoryOperationResult<StockCountSnapshot> invalid(
            InventoryOperationContext context, String operation, StockCount count,
            boolean adjustmentPermission) {
        return rejected(context, operation, Optional.of(count.id().toString()),
                Optional.of(count.version()), InventoryResultCode.INVALID_OPERATION, adjustmentPermission);
    }

    private InventoryOperationResult<StockCountSnapshot> denied(
            InventoryOperationContext context,
            String operation,
            Optional<String> id,
            Optional<Long> version,
            boolean adjustmentPermission) {
        return rejected(context, operation, id, version,
                InventoryResultCode.ACCESS_DENIED, adjustmentPermission);
    }

    private InventoryOperationResult<StockCountSnapshot> rejected(
            InventoryOperationContext context,
            String operation,
            Optional<String> id,
            Optional<Long> version,
            InventoryResultCode code,
            boolean adjustmentPermission) {
        return audit.rejected(context, permission(adjustmentPermission), operation,
                COUNT, id, version, code);
    }

    private static CompanyId company(InventoryOperationContext context) {
        return context.companyContext().companyId();
    }

    @FunctionalInterface
    private interface CountMutation {
        void apply(StockCount count, long expectedVersion);
    }
}
