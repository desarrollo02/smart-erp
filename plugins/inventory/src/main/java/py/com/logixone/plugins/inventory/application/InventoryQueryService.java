package py.com.logixone.plugins.inventory.application;

import java.util.Objects;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockAvailability;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.port.InventoryBalanceRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryDirectoryRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryItemRepository;
import py.com.logixone.plugins.inventory.application.port.StockCountRepository;
import py.com.logixone.plugins.inventory.application.port.StockMovementRepository;
import py.com.logixone.plugins.inventory.application.port.StockReservationRepository;
import py.com.logixone.plugins.inventory.application.port.WarehouseRepository;
import py.com.logixone.plugins.inventory.application.query.InventoryDirectoryQueries;
import py.com.logixone.plugins.inventory.domain.InventoryItemSnapshot;
import py.com.logixone.plugins.inventory.domain.StockCountSnapshot;
import py.com.logixone.plugins.inventory.domain.StockMovementSnapshot;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;

/** Company-scoped reads guarded by the exact view permission. */
public final class InventoryQueryService {
    private final WarehouseRepository warehouses;
    private final InventoryItemRepository items;
    private final InventoryBalanceRepository balances;
    private final StockMovementRepository movements;
    private final StockReservationRepository reservations;
    private final StockCountRepository counts;
    private final InventoryDirectoryRepository directory;

    public InventoryQueryService(
            WarehouseRepository warehouses,
            InventoryItemRepository items,
            InventoryBalanceRepository balances,
            StockMovementRepository movements,
            StockReservationRepository reservations,
            StockCountRepository counts,
            InventoryDirectoryRepository directory) {
        this.warehouses = Objects.requireNonNull(warehouses, "warehouses");
        this.items = Objects.requireNonNull(items, "items");
        this.balances = Objects.requireNonNull(balances, "balances");
        this.movements = Objects.requireNonNull(movements, "movements");
        this.reservations = Objects.requireNonNull(reservations, "reservations");
        this.counts = Objects.requireNonNull(counts, "counts");
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    public InventoryOperationResult<WarehouseSnapshot> warehouse(
            InventoryOperationContext context, WarehouseId warehouseId) {
        Objects.requireNonNull(warehouseId, "warehouseId");
        if (!authorized(context)) return denied();
        return warehouses.findById(company(context), warehouseId)
                .map(value -> InventoryOperationResult.success(value.snapshot()))
                .orElseGet(InventoryQueryService::notFound);
    }

    public InventoryOperationResult<InventoryItemSnapshot> item(
            InventoryOperationContext context, InventoryItemId itemId) {
        Objects.requireNonNull(itemId, "itemId");
        if (!authorized(context)) return denied();
        return items.findById(company(context), itemId)
                .map(value -> InventoryOperationResult.success(value.snapshot()))
                .orElseGet(InventoryQueryService::notFound);
    }

    public InventoryOperationResult<StockAvailability> availability(
            InventoryOperationContext context, StockKey key) {
        Objects.requireNonNull(key, "key");
        if (!authorized(context)) return denied();
        return balances.find(company(context), key)
                .map(value -> InventoryOperationResult.success(value.availability()))
                .orElseGet(InventoryQueryService::notFound);
    }

    public InventoryOperationResult<StockMovementSnapshot> movement(
            InventoryOperationContext context, StockMovementId movementId) {
        Objects.requireNonNull(movementId, "movementId");
        if (!authorized(context)) return denied();
        return movements.findById(company(context), movementId)
                .map(InventoryOperationResult::success)
                .orElseGet(InventoryQueryService::notFound);
    }

    public InventoryOperationResult<py.com.logixone.plugins.inventory.api.StockReservationReference> reservation(
            InventoryOperationContext context, StockReservationId reservationId) {
        Objects.requireNonNull(reservationId, "reservationId");
        if (!authorized(context)) return denied();
        return reservations.findById(company(context), reservationId)
                .map(value -> InventoryOperationResult.success(value.reference()))
                .orElseGet(InventoryQueryService::notFound);
    }

    public InventoryOperationResult<StockCountSnapshot> count(
            InventoryOperationContext context, StockCountId countId) {
        Objects.requireNonNull(countId, "countId");
        if (!authorized(context)) return denied();
        return counts.findById(company(context), countId)
                .map(value -> InventoryOperationResult.success(value.snapshot()))
                .orElseGet(InventoryQueryService::notFound);
    }

    public InventoryOperationResult<InventoryDirectoryQueries.Page<WarehouseSnapshot>> searchWarehouses(
            InventoryOperationContext context, InventoryDirectoryQueries.Criteria criteria) {
        Objects.requireNonNull(criteria, "criteria");
        if (!authorized(context)) return denied();
        return InventoryOperationResult.success(directory.warehouses(company(context), criteria));
    }

    public InventoryOperationResult<InventoryDirectoryQueries.Page<InventoryDirectoryQueries.ItemSummary>> searchItems(
            InventoryOperationContext context, InventoryDirectoryQueries.Criteria criteria) {
        Objects.requireNonNull(criteria, "criteria");
        if (!authorized(context)) return denied();
        return InventoryOperationResult.success(directory.items(company(context), criteria));
    }

    public InventoryOperationResult<InventoryDirectoryQueries.ItemSummary> itemSummary(
            InventoryOperationContext context, InventoryItemId itemId) {
        Objects.requireNonNull(itemId, "itemId");
        if (!authorized(context)) return denied();
        return directory.item(company(context), itemId)
                .map(InventoryOperationResult::success)
                .orElseGet(InventoryQueryService::notFound);
    }

    public InventoryOperationResult<InventoryDirectoryQueries.Page<InventoryDirectoryQueries.CountSummary>> searchCounts(
            InventoryOperationContext context, InventoryDirectoryQueries.CountCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");
        if (!authorized(context)) return denied();
        return InventoryOperationResult.success(directory.counts(company(context), criteria));
    }

    private static boolean authorized(InventoryOperationContext context) {
        return InventoryApplicationSupport.authorized(context, InventoryPermissions.VIEW);
    }

    private static py.com.logixone.kernel.api.company.CompanyId company(
            InventoryOperationContext context) {
        return context.companyContext().companyId();
    }

    private static <T> InventoryOperationResult<T> denied() {
        return InventoryOperationResult.failure(InventoryResultCode.ACCESS_DENIED);
    }

    private static <T> InventoryOperationResult<T> notFound() {
        return InventoryOperationResult.failure(InventoryResultCode.NOT_FOUND);
    }
}
