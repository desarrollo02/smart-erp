package py.com.logixone.plugins.inventory.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.MovementQuantity;
import py.com.logixone.plugins.inventory.api.CatalogStockReservationRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockMovementDirection;
import py.com.logixone.plugins.inventory.api.StockMovementLine;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;
import py.com.logixone.plugins.inventory.api.StockMovementType;
import py.com.logixone.plugins.inventory.api.StockReservationReference;
import py.com.logixone.plugins.inventory.api.StockReservationRequest;
import py.com.logixone.plugins.inventory.api.StockSourceReference;
import py.com.logixone.plugins.inventory.application.command.InventoryCommands;
import py.com.logixone.plugins.inventory.application.port.InventoryBalanceRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryIdGenerator;
import py.com.logixone.plugins.inventory.application.port.InventoryItemRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceException;
import py.com.logixone.plugins.inventory.application.port.ReservationOperationRepository;
import py.com.logixone.plugins.inventory.application.port.StockCountRepository;
import py.com.logixone.plugins.inventory.application.port.StockMovementRepository;
import py.com.logixone.plugins.inventory.application.port.StockReservationRepository;
import py.com.logixone.plugins.inventory.application.port.WarehouseRepository;
import py.com.logixone.plugins.inventory.domain.ConcurrentInventoryChangeException;
import py.com.logixone.plugins.inventory.domain.InventoryBalance;
import py.com.logixone.plugins.inventory.domain.InventoryItem;
import py.com.logixone.plugins.inventory.domain.ReservationOperation;
import py.com.logixone.plugins.inventory.domain.ReservationOperationType;
import py.com.logixone.plugins.inventory.domain.StockMovement;
import py.com.logixone.plugins.inventory.domain.StockReservation;
import py.com.logixone.plugins.inventory.domain.Warehouse;

/** Reservation lifecycle with physical, reserved and immutable-ledger consistency. */
public final class InventoryReservationService {
    private static final String RESERVATION = "stock_reservation";

    private final WarehouseRepository warehouses;
    private final InventoryItemRepository items;
    private final InventoryBalanceRepository balances;
    private final StockReservationRepository reservations;
    private final ReservationOperationRepository operations;
    private final StockMovementRepository movements;
    private final StockCountRepository counts;
    private final InventoryIdGenerator ids;
    private final InventoryAuditRecorder audit;
    private final Clock clock;

    public InventoryReservationService(
            WarehouseRepository warehouses,
            InventoryItemRepository items,
            InventoryBalanceRepository balances,
            StockReservationRepository reservations,
            ReservationOperationRepository operations,
            StockMovementRepository movements,
            StockCountRepository counts,
            InventoryIdGenerator ids,
            TechnicalAudit technicalAudit,
            Clock clock) {
        this.warehouses = Objects.requireNonNull(warehouses, "warehouses");
        this.items = Objects.requireNonNull(items, "items");
        this.balances = Objects.requireNonNull(balances, "balances");
        this.reservations = Objects.requireNonNull(reservations, "reservations");
        this.operations = Objects.requireNonNull(operations, "operations");
        this.movements = Objects.requireNonNull(movements, "movements");
        this.counts = Objects.requireNonNull(counts, "counts");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.audit = new InventoryAuditRecorder(technicalAudit, clock);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public InventoryOperationResult<StockReservationReference> reserve(
            InventoryOperationContext context, StockReservationRequest request) {
        Objects.requireNonNull(request, "request");
        if (!authorized(context)) {
            return denied(context, "RESERVE_STOCK", Optional.empty(), Optional.empty());
        }
        CompanyId companyId = company(context);
        Optional<StockReservation> existing = reservations.findByIdempotencyKey(
                companyId, request.source().sourceType(), request.idempotencyKey());
        if (existing.isPresent()) {
            StockReservation reservation = existing.orElseThrow();
            if (reservation.snapshot().request().equals(request)) {
                audit.unchanged(context, InventoryPermissions.RESERVATIONS_MANAGE,
                        "RESERVE_STOCK", RESERVATION, reservation.id().toString(), reservation.version());
                return InventoryOperationResult.success(reservation.reference());
            }
            return rejected(context, "RESERVE_STOCK", Optional.empty(), Optional.empty(),
                    InventoryResultCode.IDEMPOTENCY_CONFLICT);
        }
        try {
            InventoryItem item = validateReference(companyId, request.key());
            ensureUnlocked(companyId, request.key());
            InventoryBalance balance = balances.find(companyId, request.key())
                    .orElseThrow(() -> new ReservationFailure(InventoryResultCode.INSUFFICIENT_STOCK));
            if (!balance.baseUnitCode().equals(item.baseUnitCode())
                    || balance.availableQuantity().compareTo(request.quantity()) < 0) {
                throw new ReservationFailure(InventoryResultCode.INSUFFICIENT_STOCK);
            }
            long balanceVersion = balance.version();
            balance.reserve(request.quantity(), balanceVersion);
            balances.update(balance, balanceVersion);
            StockReservation reservation = StockReservation.create(
                    companyId, ids.nextReservationId(), request, clock.instant());
            reservations.insert(reservation);
            audit.changed(context, InventoryPermissions.RESERVATIONS_MANAGE,
                    "RESERVE_STOCK", RESERVATION, reservation.id().toString(),
                    Optional.empty(), reservation.version());
            return InventoryOperationResult.success(reservation.reference());
        } catch (ReservationFailure failure) {
            return rejected(context, "RESERVE_STOCK", Optional.empty(), Optional.empty(), failure.code);
        } catch (InventoryPersistenceException failure) {
            return rejected(context, "RESERVE_STOCK", Optional.empty(), Optional.empty(),
                    InventoryApplicationSupport.map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return rejected(context, "RESERVE_STOCK", Optional.empty(), Optional.empty(),
                    InventoryResultCode.INVALID_OPERATION);
        }
    }

    public InventoryOperationResult<StockReservationReference> reserveCatalogItem(
            InventoryOperationContext context, CatalogStockReservationRequest request) {
        Objects.requireNonNull(request, "request");
        if (!authorized(context)) {
            return denied(context, "RESERVE_CATALOG_STOCK", Optional.empty(), Optional.empty());
        }
        CompanyId companyId = company(context);
        InventoryItem item = items.findByCatalogItemId(
                        companyId, new CatalogItemId(request.catalogItemId()))
                .orElse(null);
        if (item == null || !item.active()) {
            return rejected(context, "RESERVE_CATALOG_STOCK", Optional.empty(),
                    Optional.empty(), InventoryResultCode.REFERENCE_CONFLICT);
        }
        return reserve(context, request.resolve(item.id()));
    }

    public InventoryOperationResult<StockReservationReference> consume(
            InventoryOperationContext context, InventoryCommands.ConsumeReservation command) {
        Objects.requireNonNull(command, "command");
        return mutateQuantity(context, command.reservationId(), command.expectedVersion(),
                command.quantity(), command.idempotencyKey(), ReservationOperationType.CONSUME,
                "CONSUME_STOCK_RESERVATION");
    }

    public InventoryOperationResult<StockReservationReference> release(
            InventoryOperationContext context, InventoryCommands.ReleaseReservation command) {
        Objects.requireNonNull(command, "command");
        return mutateQuantity(context, command.reservationId(), command.expectedVersion(),
                command.quantity(), command.idempotencyKey(), ReservationOperationType.RELEASE,
                "RELEASE_STOCK_RESERVATION");
    }

    public InventoryOperationResult<StockReservationReference> expire(
            InventoryOperationContext context, InventoryCommands.ExpireReservation command) {
        Objects.requireNonNull(command, "command");
        String operation = "EXPIRE_STOCK_RESERVATION";
        if (!authorized(context)) {
            return denied(context, operation, Optional.of(command.reservationId().toString()),
                    Optional.of(command.expectedVersion()));
        }
        CompanyId companyId = company(context);
        Optional<ReservationOperation> previous = operations.findByIdempotencyKey(
                companyId, command.idempotencyKey());
        if (previous.isPresent()) {
            ReservationOperation receipt = previous.orElseThrow();
            StockReservation reservation = reservations.findById(companyId, command.reservationId())
                    .orElse(null);
            if (reservation != null
                    && receipt.reservationId().equals(command.reservationId())
                    && receipt.type() == ReservationOperationType.EXPIRE) {
                audit.unchanged(context, InventoryPermissions.RESERVATIONS_MANAGE,
                        operation, RESERVATION, reservation.id().toString(), receipt.resultingVersion());
                return InventoryOperationResult.success(receipt.result(reservation));
            }
            return rejected(context, operation, Optional.of(command.reservationId().toString()),
                    Optional.of(command.expectedVersion()), InventoryResultCode.IDEMPOTENCY_CONFLICT);
        }
        StockReservation reservation = reservations.findById(companyId, command.reservationId())
                .orElse(null);
        if (reservation == null) {
            return rejected(context, operation, Optional.of(command.reservationId().toString()),
                    Optional.of(command.expectedVersion()), InventoryResultCode.NOT_FOUND);
        }
        try {
            StockKey key = reservation.snapshot().request().key();
            ensureUnlocked(companyId, key);
            InventoryBalance balance = balances.find(companyId, key)
                    .orElseThrow(() -> new ReservationFailure(InventoryResultCode.NOT_FOUND));
            BigDecimal quantity = reservation.remainingQuantity();
            long balanceVersion = balance.version();
            long reservationVersion = reservation.version();
            balance.release(quantity, balanceVersion);
            reservation.expire(clock.instant(), command.expectedVersion());
            balances.update(balance, balanceVersion);
            reservations.update(reservation, reservationVersion);
            ReservationOperation receipt = ReservationOperation.capture(
                    reservation, ReservationOperationType.EXPIRE, quantity,
                    command.idempotencyKey(), clock.instant());
            operations.append(receipt);
            changed(context, operation, reservation, reservationVersion);
            return InventoryOperationResult.success(reservation.reference());
        } catch (ReservationFailure failure) {
            return rejected(context, operation, Optional.of(reservation.id().toString()),
                    Optional.of(reservation.version()), failure.code);
        } catch (ConcurrentInventoryChangeException failure) {
            return rejected(context, operation, Optional.of(reservation.id().toString()),
                    Optional.of(reservation.version()), InventoryResultCode.VERSION_CONFLICT);
        } catch (InventoryPersistenceException failure) {
            return rejected(context, operation, Optional.of(reservation.id().toString()),
                    Optional.of(reservation.version()), InventoryApplicationSupport.map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return rejected(context, operation, Optional.of(reservation.id().toString()),
                    Optional.of(reservation.version()), InventoryResultCode.INVALID_OPERATION);
        }
    }

    private InventoryOperationResult<StockReservationReference> mutateQuantity(
            InventoryOperationContext context,
            py.com.logixone.plugins.inventory.api.StockReservationId reservationId,
            long expectedVersion,
            BigDecimal quantity,
            String idempotencyKey,
            ReservationOperationType type,
            String operation) {
        if (!authorized(context)) {
            return denied(context, operation, Optional.of(reservationId.toString()),
                    Optional.of(expectedVersion));
        }
        CompanyId companyId = company(context);
        Optional<ReservationOperation> previous = operations.findByIdempotencyKey(
                companyId, idempotencyKey);
        if (previous.isPresent()) {
            ReservationOperation receipt = previous.orElseThrow();
            StockReservation reservation = reservations.findById(companyId, reservationId).orElse(null);
            if (reservation != null && receipt.matches(reservationId, type, quantity)) {
                audit.unchanged(context, InventoryPermissions.RESERVATIONS_MANAGE,
                        operation, RESERVATION, reservation.id().toString(), receipt.resultingVersion());
                return InventoryOperationResult.success(receipt.result(reservation));
            }
            return rejected(context, operation, Optional.of(reservationId.toString()),
                    Optional.of(expectedVersion), InventoryResultCode.IDEMPOTENCY_CONFLICT);
        }
        StockReservation reservation = reservations.findById(companyId, reservationId).orElse(null);
        if (reservation == null) {
            return rejected(context, operation, Optional.of(reservationId.toString()),
                    Optional.of(expectedVersion), InventoryResultCode.NOT_FOUND);
        }
        try {
            StockKey key = reservation.snapshot().request().key();
            InventoryItem item = validateReference(companyId, key);
            ensureUnlocked(companyId, key);
            InventoryBalance balance = balances.find(companyId, key)
                    .orElseThrow(() -> new ReservationFailure(InventoryResultCode.NOT_FOUND));
            long balanceVersion = balance.version();
            long reservationVersion = reservation.version();
            if (type == ReservationOperationType.CONSUME) {
                balance.consumeReserved(quantity, balanceVersion);
                reservation.consume(quantity, expectedVersion);
            } else {
                balance.release(quantity, balanceVersion);
                reservation.release(quantity, expectedVersion);
            }
            balances.update(balance, balanceVersion);
            reservations.update(reservation, reservationVersion);
            if (type == ReservationOperationType.CONSUME) {
                appendConsumptionMovement(reservation, item, key, quantity, idempotencyKey);
            }
            ReservationOperation receipt = ReservationOperation.capture(
                    reservation, type, quantity, idempotencyKey, clock.instant());
            operations.append(receipt);
            changed(context, operation, reservation, reservationVersion);
            return InventoryOperationResult.success(reservation.reference());
        } catch (ReservationFailure failure) {
            return rejected(context, operation, Optional.of(reservation.id().toString()),
                    Optional.of(reservation.version()), failure.code);
        } catch (ConcurrentInventoryChangeException failure) {
            return rejected(context, operation, Optional.of(reservation.id().toString()),
                    Optional.of(reservation.version()), InventoryResultCode.VERSION_CONFLICT);
        } catch (InventoryPersistenceException failure) {
            return rejected(context, operation, Optional.of(reservation.id().toString()),
                    Optional.of(reservation.version()), InventoryApplicationSupport.map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return rejected(context, operation, Optional.of(reservation.id().toString()),
                    Optional.of(reservation.version()), InventoryResultCode.INVALID_OPERATION);
        }
    }

    private void appendConsumptionMovement(
            StockReservation reservation,
            InventoryItem item,
            StockKey key,
            BigDecimal quantity,
            String idempotencyKey) {
        StockSourceReference source = new StockSourceReference(
                "RESERVATION", reservation.id().toString());
        StockMovementRequest request = new StockMovementRequest(
                StockMovementType.ISSUE, "RESERVATION_CONSUMED", source, idempotencyKey,
                List.of(new StockMovementLine(key, StockMovementDirection.DECREASE,
                        new MovementQuantity(
                                item.baseUnitCode(), quantity, item.baseUnitCode(),
                                BigDecimal.ONE, quantity, item.catalogItemVersion()))),
                Optional.empty());
        if (movements.findByIdempotencyKey(
                reservation.companyId(), source.sourceType(), idempotencyKey).isPresent()) {
            throw new ReservationFailure(InventoryResultCode.IDEMPOTENCY_CONFLICT);
        }
        StockMovement movement = StockMovement.post(
                reservation.companyId(), ids.nextMovementId(), request, clock.instant());
        movements.append(movement.snapshot(Map.of(item.id(), item)));
    }

    private InventoryItem validateReference(CompanyId companyId, StockKey key) {
        InventoryItem item = items.findById(companyId, key.inventoryItemId())
                .orElseThrow(() -> new ReservationFailure(InventoryResultCode.NOT_FOUND));
        if (!item.active()) {
            throw new ReservationFailure(InventoryResultCode.REFERENCE_CONFLICT);
        }
        item.validateKey(key);
        Warehouse warehouse = warehouses.findById(companyId, key.warehouseId())
                .orElseThrow(() -> new ReservationFailure(InventoryResultCode.NOT_FOUND));
        var location = warehouse.locations().get(key.locationId());
        if (!warehouse.active() || location == null || !location.active()) {
            throw new ReservationFailure(InventoryResultCode.REFERENCE_CONFLICT);
        }
        return item;
    }

    private void ensureUnlocked(CompanyId companyId, StockKey key) {
        if (counts.blocks(companyId, key)) {
            throw new ReservationFailure(InventoryResultCode.SCOPE_LOCKED);
        }
    }

    private static boolean authorized(InventoryOperationContext context) {
        return InventoryApplicationSupport.authorized(
                context, InventoryPermissions.RESERVATIONS_MANAGE);
    }

    private void changed(
            InventoryOperationContext context,
            String operation,
            StockReservation reservation,
            long previousVersion) {
        audit.changed(context, InventoryPermissions.RESERVATIONS_MANAGE,
                operation, RESERVATION, reservation.id().toString(),
                Optional.of(previousVersion), reservation.version());
    }

    private InventoryOperationResult<StockReservationReference> denied(
            InventoryOperationContext context,
            String operation,
            Optional<String> id,
            Optional<Long> version) {
        return rejected(context, operation, id, version, InventoryResultCode.ACCESS_DENIED);
    }

    private InventoryOperationResult<StockReservationReference> rejected(
            InventoryOperationContext context,
            String operation,
            Optional<String> id,
            Optional<Long> version,
            InventoryResultCode code) {
        return audit.rejected(context, InventoryPermissions.RESERVATIONS_MANAGE,
                operation, RESERVATION, id, version, code);
    }

    private static CompanyId company(InventoryOperationContext context) {
        return context.companyContext().companyId();
    }

    private static final class ReservationFailure extends RuntimeException {
        private final InventoryResultCode code;

        private ReservationFailure(InventoryResultCode code) {
            super(code.name());
            this.code = code;
        }
    }
}
