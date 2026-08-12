package py.com.logixone.plugins.inventory.application;

import java.math.BigDecimal;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversions;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.inventory.api.CatalogStockMovementRequest;
import py.com.logixone.plugins.inventory.api.MovementQuantity;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockMovementDirection;
import py.com.logixone.plugins.inventory.api.StockMovementLine;
import py.com.logixone.plugins.inventory.api.StockMovementReference;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;
import py.com.logixone.plugins.inventory.api.StockMovementType;
import py.com.logixone.plugins.inventory.application.port.InventoryBalanceRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryIdGenerator;
import py.com.logixone.plugins.inventory.application.port.InventoryItemRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceException;
import py.com.logixone.plugins.inventory.application.port.StockCountRepository;
import py.com.logixone.plugins.inventory.application.port.StockMovementRepository;
import py.com.logixone.plugins.inventory.application.port.WarehouseRepository;
import py.com.logixone.plugins.inventory.domain.ConcurrentInventoryChangeException;
import py.com.logixone.plugins.inventory.domain.InventoryBalance;
import py.com.logixone.plugins.inventory.domain.InventoryItem;
import py.com.logixone.plugins.inventory.domain.StockMovement;
import py.com.logixone.plugins.inventory.domain.StockMovementSnapshot;
import py.com.logixone.plugins.inventory.domain.Warehouse;

/** Authorized, idempotent append-only stock movement application service. */
public final class InventoryMovementService {
    private static final String MOVEMENT = "stock_movement";
    private static final Logger LOGGER = System.getLogger(InventoryMovementService.class.getName());

    private final WarehouseRepository warehouses;
    private final InventoryItemRepository items;
    private final InventoryBalanceRepository balances;
    private final StockMovementRepository movements;
    private final StockCountRepository counts;
    private final CatalogUnitConversions conversions;
    private final InventoryIdGenerator ids;
    private final InventoryAuditRecorder audit;
    private final Clock clock;

    public InventoryMovementService(
            WarehouseRepository warehouses,
            InventoryItemRepository items,
            InventoryBalanceRepository balances,
            StockMovementRepository movements,
            StockCountRepository counts,
            CatalogUnitConversions conversions,
            InventoryIdGenerator ids,
            TechnicalAudit technicalAudit,
            Clock clock) {
        this.warehouses = Objects.requireNonNull(warehouses, "warehouses");
        this.items = Objects.requireNonNull(items, "items");
        this.balances = Objects.requireNonNull(balances, "balances");
        this.movements = Objects.requireNonNull(movements, "movements");
        this.counts = Objects.requireNonNull(counts, "counts");
        this.conversions = Objects.requireNonNull(conversions, "conversions");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.audit = new InventoryAuditRecorder(technicalAudit, clock);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public InventoryOperationResult<StockMovementReference> post(
            InventoryOperationContext context, StockMovementRequest request) {
        return post(context, request, permissionFor(request), false, false,
                "POST_STOCK_MOVEMENT");
    }

    public InventoryOperationResult<StockMovementReference> postCatalog(
            InventoryOperationContext context, CatalogStockMovementRequest request) {
        Objects.requireNonNull(request, "request");
        if (!InventoryApplicationSupport.authorized(
                context, InventoryPermissions.PURCHASE_MOVEMENTS_POST)) {
            return audit.rejected(
                    context,
                    InventoryPermissions.PURCHASE_MOVEMENTS_POST,
                    "POST_PURCHASE_MOVEMENT",
                    MOVEMENT,
                    Optional.empty(),
                    Optional.empty(),
                    InventoryResultCode.ACCESS_DENIED);
        }
        CompanyId companyId = company(context);
        InventoryItem item = items.findByCatalogItemId(
                        companyId, new CatalogItemId(request.catalogItemId()))
                .orElse(null);
        if (item == null) {
            return rejected(
                    context,
                    InventoryPermissions.PURCHASE_MOVEMENTS_POST,
                    "POST_PURCHASE_MOVEMENT",
                    Optional.empty(),
                    InventoryResultCode.NOT_FOUND);
        }
        return post(
                context,
                request.resolve(item.id()),
                InventoryPermissions.PURCHASE_MOVEMENTS_POST,
                false,
                true,
                "POST_PURCHASE_MOVEMENT");
    }

    InventoryOperationResult<StockMovementReference> postCountAdjustment(
            InventoryOperationContext context, StockMovementRequest request) {
        if (request.type() != StockMovementType.ADJUSTMENT) {
            throw new IllegalArgumentException("A count may only post an ADJUSTMENT movement");
        }
        return post(context, request, InventoryPermissions.ADJUSTMENTS_POST,
                true, false, "POST_STOCK_COUNT_ADJUSTMENT");
    }

    private InventoryOperationResult<StockMovementReference> post(
            InventoryOperationContext context,
            StockMovementRequest request,
            ContributionId permission,
            boolean bypassCountLock,
            boolean useHistoricalConversion,
            String operation) {
        Objects.requireNonNull(request, "request");
        if (!InventoryApplicationSupport.authorized(context, permission)) {
            return audit.rejected(context, permission, operation, MOVEMENT,
                    Optional.empty(), Optional.empty(), InventoryResultCode.ACCESS_DENIED);
        }
        CompanyId companyId = company(context);
        Optional<StockMovementSnapshot> previous = movements.findByIdempotencyKey(
                companyId, request.source().sourceType(), request.idempotencyKey());
        if (previous.isPresent()) {
            if (previous.orElseThrow().request().equals(request)) {
                var reference = StockMovement.restore(previous.orElseThrow()).reference();
                audit.unchanged(context, permission, operation, MOVEMENT,
                        reference.id().toString(), 0);
                return InventoryOperationResult.success(reference);
            }
            return rejected(context, permission, operation, Optional.empty(),
                    InventoryResultCode.IDEMPOTENCY_CONFLICT);
        }

        try {
            validateReversal(companyId, request);
            PreparedMovement prepared = prepare(
                    companyId, request, bypassCountLock, useHistoricalConversion);
            persistBalances(prepared.balances());
            StockMovement movement = StockMovement.post(
                    companyId, ids.nextMovementId(), request, clock.instant());
            StockMovementSnapshot stored = movements.append(movement.snapshot(prepared.items()));
            audit.changed(context, permission, operation, MOVEMENT,
                    stored.id().toString(), Optional.empty(), 0);
            return InventoryOperationResult.success(StockMovement.restore(stored).reference());
        } catch (MovementFailure failure) {
            return rejected(context, permission, operation, Optional.empty(), failure.code);
        } catch (ConcurrentInventoryChangeException failure) {
            return rejected(context, permission, operation, Optional.empty(),
                    InventoryResultCode.VERSION_CONFLICT);
        } catch (InventoryPersistenceException failure) {
            return rejected(context, permission, operation, Optional.empty(),
                    InventoryApplicationSupport.map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            LOGGER.log(Level.WARNING,
                    "event=inventory_movement_rejected operation={0} company_id={1} "
                            + "failure_type={2} failure_message={3}",
                    operation,
                    companyId,
                    failure.getClass().getSimpleName(),
                    failure.getMessage());
            return rejected(context, permission, operation, Optional.empty(),
                    InventoryResultCode.INVALID_OPERATION);
        }
    }

    private PreparedMovement prepare(
            CompanyId companyId,
            StockMovementRequest request,
            boolean bypassCountLock,
            boolean useHistoricalConversion) {
        Map<py.com.logixone.plugins.inventory.api.InventoryItemId, InventoryItem> itemSnapshots =
                new LinkedHashMap<>();
        Map<StockKey, BalanceChange> changes = new LinkedHashMap<>();
        for (StockMovementLine line : request.lines()) {
            InventoryItem item = itemSnapshots.computeIfAbsent(
                    line.key().inventoryItemId(), id -> items.findById(companyId, id)
                            .orElseThrow(() -> new MovementFailure(InventoryResultCode.NOT_FOUND)));
            validateLine(companyId, request, line, item,
                    bypassCountLock, useHistoricalConversion);
            BigDecimal signed = line.direction() == StockMovementDirection.INCREASE
                    ? line.quantity().baseQuantity()
                    : line.quantity().baseQuantity().negate();
            changes.compute(line.key(), (key, current) -> current == null
                    ? new BalanceChange(loadBalance(companyId, key, item.baseUnitCode()), signed)
                    : current.add(signed));
        }
        changes.values().forEach(BalanceChange::validateAvailability);
        return new PreparedMovement(itemSnapshots, changes.values().stream().toList());
    }

    private void validateLine(
            CompanyId companyId,
            StockMovementRequest request,
            StockMovementLine line,
            InventoryItem item,
            boolean bypassCountLock,
            boolean useHistoricalConversion) {
        if (!item.active()) {
            throw new MovementFailure(InventoryResultCode.REFERENCE_CONFLICT);
        }
        item.validateKey(line.key());
        item.validateMovementQuantity(line.quantity().baseQuantity());
        if (!item.baseUnitCode().equals(line.quantity().baseUnitCode())) {
            throw new MovementFailure(InventoryResultCode.REFERENCE_CONFLICT);
        }
        validateWarehouse(companyId, line.key());
        if (!bypassCountLock && counts.blocks(companyId, line.key())) {
            throw new MovementFailure(InventoryResultCode.SCOPE_LOCKED);
        }
        if (request.type() != StockMovementType.REVERSAL && !useHistoricalConversion) {
            validateConversion(companyId, item, line.quantity());
        }
    }

    private void validateWarehouse(CompanyId companyId, StockKey key) {
        Warehouse warehouse = warehouses.findById(companyId, key.warehouseId())
                .orElseThrow(() -> new MovementFailure(InventoryResultCode.NOT_FOUND));
        var location = warehouse.locations().get(key.locationId());
        if (!warehouse.active() || location == null || !location.active()) {
            throw new MovementFailure(InventoryResultCode.REFERENCE_CONFLICT);
        }
    }

    private void validateConversion(
            CompanyId companyId, InventoryItem item, MovementQuantity quantity) {
        var result = conversions.convert(companyId, new CatalogUnitConversionRequest(
                        item.catalogItemId(), quantity.presentedUnitCode(),
                        item.baseUnitCode(), quantity.presentedQuantity()))
                .orElseThrow(() -> new MovementFailure(InventoryResultCode.REFERENCE_CONFLICT));
        if (!result.itemId().equals(item.catalogItemId())
                || !result.sourceUnitCode().equals(quantity.presentedUnitCode())
                || !result.targetUnitCode().equals(quantity.baseUnitCode())
                || result.sourceQuantity().compareTo(quantity.presentedQuantity()) != 0
                || result.factor().compareTo(quantity.conversionFactor()) != 0
                || result.convertedQuantity().compareTo(quantity.baseQuantity()) != 0
                || result.itemVersion() != quantity.catalogItemVersion()
                || result.itemVersion() != item.catalogItemVersion()
                || !item.baseUnitCode().equals(quantity.baseUnitCode())) {
            throw new MovementFailure(InventoryResultCode.REFERENCE_CONFLICT);
        }
    }

    private void validateReversal(CompanyId companyId, StockMovementRequest request) {
        if (request.type() != StockMovementType.REVERSAL) {
            return;
        }
        StockMovementSnapshot original = movements.findById(
                        companyId, request.reversalOf().orElseThrow())
                .orElseThrow(() -> new MovementFailure(InventoryResultCode.NOT_FOUND));
        if (original.request().type() == StockMovementType.REVERSAL
                || !inverseOf(original.request().lines(), request.lines())) {
            throw new MovementFailure(InventoryResultCode.INVALID_OPERATION);
        }
    }

    private static boolean inverseOf(
            List<StockMovementLine> original, List<StockMovementLine> reversal) {
        if (original.size() != reversal.size()) {
            return false;
        }
        for (int index = 0; index < original.size(); index++) {
            StockMovementLine expected = original.get(index);
            StockMovementLine actual = reversal.get(index);
            if (!expected.key().equals(actual.key())
                    || !expected.quantity().equals(actual.quantity())
                    || actual.direction() != inverse(expected.direction())) {
                return false;
            }
        }
        return true;
    }

    private static StockMovementDirection inverse(StockMovementDirection direction) {
        return direction == StockMovementDirection.INCREASE
                ? StockMovementDirection.DECREASE : StockMovementDirection.INCREASE;
    }

    private BalanceState loadBalance(CompanyId companyId, StockKey key, String baseUnitCode) {
        Optional<InventoryBalance> existing = balances.find(companyId, key);
        if (existing.isPresent() && !existing.orElseThrow().baseUnitCode().equals(baseUnitCode)) {
            throw new MovementFailure(InventoryResultCode.REFERENCE_CONFLICT);
        }
        return new BalanceState(
                existing.orElseGet(() -> InventoryBalance.empty(companyId, key, baseUnitCode)),
                existing.isPresent());
    }

    private void persistBalances(List<BalanceChange> changes) {
        for (BalanceChange change : changes) {
            if (change.delta().signum() == 0) {
                continue;
            }
            InventoryBalance balance = change.state().balance();
            long previousVersion = balance.version();
            balance.adjustPhysical(change.delta(), previousVersion);
            if (change.state().persisted()) {
                balances.update(balance, previousVersion);
            } else {
                balances.insert(balance);
            }
        }
    }

    private InventoryOperationResult<StockMovementReference> rejected(
            InventoryOperationContext context,
            ContributionId permission,
            String operation,
            Optional<String> id,
            InventoryResultCode code) {
        return audit.rejected(context, permission, operation, MOVEMENT,
                id, Optional.empty(), code);
    }

    private static ContributionId permissionFor(StockMovementRequest request) {
        Objects.requireNonNull(request, "request");
        return switch (request.type()) {
            case ADJUSTMENT, REVERSAL -> InventoryPermissions.ADJUSTMENTS_POST;
            case RECEIPT, ISSUE, TRANSFER -> InventoryPermissions.MOVEMENTS_POST;
        };
    }

    private static CompanyId company(InventoryOperationContext context) {
        return context.companyContext().companyId();
    }

    private record PreparedMovement(
            Map<py.com.logixone.plugins.inventory.api.InventoryItemId, InventoryItem> items,
            List<BalanceChange> balances) {
    }

    private record BalanceState(InventoryBalance balance, boolean persisted) {
    }

    private record BalanceChange(BalanceState state, BigDecimal delta) {
        BalanceChange add(BigDecimal additional) {
            return new BalanceChange(state, delta.add(additional));
        }

        void validateAvailability() {
            BigDecimal nextPhysical = state.balance().physicalQuantity().add(delta);
            if (nextPhysical.signum() < 0
                    || nextPhysical.compareTo(state.balance().reservedQuantity()) < 0) {
                throw new MovementFailure(InventoryResultCode.INSUFFICIENT_STOCK);
            }
        }
    }

    private static final class MovementFailure extends RuntimeException {
        private final InventoryResultCode code;

        private MovementFailure(InventoryResultCode code) {
            super(code.name());
            this.code = code;
        }
    }
}
