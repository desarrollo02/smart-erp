package py.com.logixone.plugins.inventory.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.time.Clock;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversions;
import py.com.logixone.plugins.inventory.api.CatalogStockMovementRequest;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockAvailability;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockMovementReference;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.StockReservationReference;
import py.com.logixone.plugins.inventory.api.StockReservationRequest;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.InventoryCountService;
import py.com.logixone.plugins.inventory.application.InventoryMovementService;
import py.com.logixone.plugins.inventory.application.InventoryOperationContext;
import py.com.logixone.plugins.inventory.application.InventoryOperationResult;
import py.com.logixone.plugins.inventory.application.InventoryQueryService;
import py.com.logixone.plugins.inventory.application.InventoryReservationService;
import py.com.logixone.plugins.inventory.application.InventoryStructureService;
import py.com.logixone.plugins.inventory.application.InventoryUseCases;
import py.com.logixone.plugins.inventory.application.command.InventoryCommands;
import py.com.logixone.plugins.inventory.application.port.InventoryBalanceRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryDirectoryRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryIdGenerator;
import py.com.logixone.plugins.inventory.application.port.InventoryItemRepository;
import py.com.logixone.plugins.inventory.application.port.ReservationOperationRepository;
import py.com.logixone.plugins.inventory.application.port.StockCountRepository;
import py.com.logixone.plugins.inventory.application.port.StockMovementRepository;
import py.com.logixone.plugins.inventory.application.port.StockReservationRepository;
import py.com.logixone.plugins.inventory.application.port.WarehouseRepository;
import py.com.logixone.plugins.inventory.domain.InventoryItemSnapshot;
import py.com.logixone.plugins.inventory.domain.StockCountSnapshot;
import py.com.logixone.plugins.inventory.domain.StockMovementSnapshot;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;
import py.com.logixone.plugins.inventory.application.query.InventoryDirectoryQueries;

/** CDI/JTA boundary for every inventory mutation and read. */
@ApplicationScoped
@Transactional(rollbackOn = RuntimeException.class)
public class TransactionalInventoryUseCases implements InventoryUseCases {
    @Inject WarehouseRepository warehouses;
    @Inject InventoryItemRepository items;
    @Inject InventoryBalanceRepository balances;
    @Inject StockMovementRepository movements;
    @Inject StockReservationRepository reservations;
    @Inject ReservationOperationRepository reservationOperations;
    @Inject StockCountRepository counts;
    @Inject InventoryDirectoryRepository directory;
    @Inject CatalogItemDirectory catalog;
    @Inject CatalogUnitConversions conversions;
    @Inject InventoryIdGenerator ids;
    @Inject TechnicalAudit audit;
    @Inject TransactionSynchronizationRegistry transactions;

    @Override public InventoryOperationResult<WarehouseSnapshot> openWarehouse(
            InventoryOperationContext context, InventoryCommands.OpenWarehouse command) {
        return mutation(structure().openWarehouse(context, command));
    }
    @Override public InventoryOperationResult<WarehouseSnapshot> addLocation(
            InventoryOperationContext context, InventoryCommands.AddLocation command) {
        return mutation(structure().addLocation(context, command));
    }
    @Override public InventoryOperationResult<WarehouseSnapshot> renameWarehouse(
            InventoryOperationContext context, InventoryCommands.RenameWarehouse command) {
        return mutation(structure().renameWarehouse(context, command));
    }
    @Override public InventoryOperationResult<WarehouseSnapshot> renameLocation(
            InventoryOperationContext context, InventoryCommands.RenameLocation command) {
        return mutation(structure().renameLocation(context, command));
    }
    @Override public InventoryOperationResult<WarehouseSnapshot> inactivateWarehouse(
            InventoryOperationContext context, InventoryCommands.InactivateWarehouse command) {
        return mutation(structure().inactivateWarehouse(context, command));
    }
    @Override public InventoryOperationResult<WarehouseSnapshot> inactivateLocation(
            InventoryOperationContext context, InventoryCommands.InactivateLocation command) {
        return mutation(structure().inactivateLocation(context, command));
    }
    @Override public InventoryOperationResult<InventoryItemSnapshot> enrollItem(
            InventoryOperationContext context, InventoryCommands.EnrollItem command) {
        return mutation(structure().enrollItem(context, command));
    }
    @Override public InventoryOperationResult<InventoryItemSnapshot> refreshItem(
            InventoryOperationContext context, InventoryCommands.RefreshItem command) {
        return mutation(structure().refreshItem(context, command));
    }
    @Override public InventoryOperationResult<InventoryItemSnapshot> inactivateItem(
            InventoryOperationContext context, InventoryCommands.InactivateItem command) {
        return mutation(structure().inactivateItem(context, command));
    }

    @Override public InventoryOperationResult<StockMovementReference> postMovement(
            InventoryOperationContext context, StockMovementRequest request) {
        return mutation(movementService().post(context, request));
    }
    @Override public InventoryOperationResult<StockMovementReference> postCatalogMovement(
            InventoryOperationContext context, CatalogStockMovementRequest request) {
        return mutation(movementService().postCatalog(context, request));
    }
    @Override public InventoryOperationResult<StockReservationReference> reserve(
            InventoryOperationContext context, StockReservationRequest request) {
        return mutation(reservationService().reserve(context, request));
    }
    @Override public InventoryOperationResult<StockReservationReference> consume(
            InventoryOperationContext context, InventoryCommands.ConsumeReservation command) {
        return mutation(reservationService().consume(context, command));
    }
    @Override public InventoryOperationResult<StockReservationReference> release(
            InventoryOperationContext context, InventoryCommands.ReleaseReservation command) {
        return mutation(reservationService().release(context, command));
    }
    @Override public InventoryOperationResult<StockReservationReference> expire(
            InventoryOperationContext context, InventoryCommands.ExpireReservation command) {
        return mutation(reservationService().expire(context, command));
    }

    @Override public InventoryOperationResult<StockCountSnapshot> draftCount(
            InventoryOperationContext context, InventoryCommands.DraftCount command) {
        return mutation(countService().draft(context, command));
    }
    @Override public InventoryOperationResult<StockCountSnapshot> addCountLine(
            InventoryOperationContext context, InventoryCommands.AddCountLine command) {
        return mutation(countService().addLine(context, command));
    }
    @Override public InventoryOperationResult<StockCountSnapshot> startCount(
            InventoryOperationContext context, InventoryCommands.CountTransition command) {
        return mutation(countService().start(context, command));
    }
    @Override public InventoryOperationResult<StockCountSnapshot> recordCount(
            InventoryOperationContext context, InventoryCommands.RecordCount command) {
        return mutation(countService().record(context, command));
    }
    @Override public InventoryOperationResult<StockCountSnapshot> reviewCount(
            InventoryOperationContext context, InventoryCommands.CountTransition command) {
        return mutation(countService().review(context, command));
    }
    @Override public InventoryOperationResult<StockCountSnapshot> postCount(
            InventoryOperationContext context, InventoryCommands.CountTransition command) {
        return mutation(countService().post(context, command));
    }
    @Override public InventoryOperationResult<StockCountSnapshot> cancelCount(
            InventoryOperationContext context, InventoryCommands.CountTransition command) {
        return mutation(countService().cancel(context, command));
    }

    @Override @Transactional(TxType.SUPPORTS)
    public InventoryOperationResult<WarehouseSnapshot> warehouse(
            InventoryOperationContext context, WarehouseId id) {
        return queries().warehouse(context, id);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public InventoryOperationResult<InventoryItemSnapshot> item(
            InventoryOperationContext context, InventoryItemId id) {
        return queries().item(context, id);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public InventoryOperationResult<StockAvailability> availability(
            InventoryOperationContext context, StockKey key) {
        return queries().availability(context, key);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public InventoryOperationResult<StockMovementSnapshot> movement(
            InventoryOperationContext context, StockMovementId id) {
        return queries().movement(context, id);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public InventoryOperationResult<StockReservationReference> reservation(
            InventoryOperationContext context, StockReservationId id) {
        return queries().reservation(context, id);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public InventoryOperationResult<StockCountSnapshot> count(
            InventoryOperationContext context, StockCountId id) {
        return queries().count(context, id);
    }

    @Override @Transactional(TxType.SUPPORTS)
    public InventoryOperationResult<InventoryDirectoryQueries.Page<WarehouseSnapshot>> searchWarehouses(
            InventoryOperationContext context, InventoryDirectoryQueries.Criteria criteria) {
        return queries().searchWarehouses(context, criteria);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public InventoryOperationResult<InventoryDirectoryQueries.Page<InventoryDirectoryQueries.ItemSummary>> searchItems(
            InventoryOperationContext context, InventoryDirectoryQueries.Criteria criteria) {
        return queries().searchItems(context, criteria);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public InventoryOperationResult<InventoryDirectoryQueries.ItemSummary> itemSummary(
            InventoryOperationContext context, InventoryItemId id) {
        return queries().itemSummary(context, id);
    }
    @Override @Transactional(TxType.SUPPORTS)
    public InventoryOperationResult<InventoryDirectoryQueries.Page<InventoryDirectoryQueries.CountSummary>> searchCounts(
            InventoryOperationContext context, InventoryDirectoryQueries.CountCriteria criteria) {
        return queries().searchCounts(context, criteria);
    }

    private InventoryStructureService structure() {
        return new InventoryStructureService(
                warehouses, items, balances, catalog, ids, audit, Clock.systemUTC());
    }

    private InventoryMovementService movementService() {
        return new InventoryMovementService(
                warehouses, items, balances, movements, counts, conversions,
                ids, audit, Clock.systemUTC());
    }

    private InventoryReservationService reservationService() {
        return new InventoryReservationService(
                warehouses, items, balances, reservations, reservationOperations,
                movements, counts, ids, audit, Clock.systemUTC());
    }

    private InventoryCountService countService() {
        return new InventoryCountService(
                warehouses, items, balances, counts, movementService(),
                ids, audit, Clock.systemUTC());
    }

    private InventoryQueryService queries() {
        return new InventoryQueryService(
                warehouses, items, balances, movements, reservations, counts, directory);
    }

    private <T> InventoryOperationResult<T> mutation(InventoryOperationResult<T> result) {
        if (!result.successful()) {
            transactions.setRollbackOnly();
        }
        return result;
    }
}
