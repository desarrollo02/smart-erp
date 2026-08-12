package py.com.logixone.plugins.inventory.application;

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
import py.com.logixone.plugins.inventory.application.command.InventoryCommands;
import py.com.logixone.plugins.inventory.application.query.InventoryDirectoryQueries;
import py.com.logixone.plugins.inventory.domain.InventoryItemSnapshot;
import py.com.logixone.plugins.inventory.domain.StockCountSnapshot;
import py.com.logixone.plugins.inventory.domain.StockMovementSnapshot;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;

/** Internal facade; infrastructure owns transaction demarcation. */
public interface InventoryUseCases {
    InventoryOperationResult<WarehouseSnapshot> openWarehouse(
            InventoryOperationContext context, InventoryCommands.OpenWarehouse command);
    InventoryOperationResult<WarehouseSnapshot> addLocation(
            InventoryOperationContext context, InventoryCommands.AddLocation command);
    InventoryOperationResult<WarehouseSnapshot> renameWarehouse(
            InventoryOperationContext context, InventoryCommands.RenameWarehouse command);
    InventoryOperationResult<WarehouseSnapshot> renameLocation(
            InventoryOperationContext context, InventoryCommands.RenameLocation command);
    InventoryOperationResult<WarehouseSnapshot> inactivateWarehouse(
            InventoryOperationContext context, InventoryCommands.InactivateWarehouse command);
    InventoryOperationResult<WarehouseSnapshot> inactivateLocation(
            InventoryOperationContext context, InventoryCommands.InactivateLocation command);
    InventoryOperationResult<InventoryItemSnapshot> enrollItem(
            InventoryOperationContext context, InventoryCommands.EnrollItem command);
    InventoryOperationResult<InventoryItemSnapshot> refreshItem(
            InventoryOperationContext context, InventoryCommands.RefreshItem command);
    InventoryOperationResult<InventoryItemSnapshot> inactivateItem(
            InventoryOperationContext context, InventoryCommands.InactivateItem command);

    InventoryOperationResult<StockMovementReference> postMovement(
            InventoryOperationContext context, StockMovementRequest request);
    InventoryOperationResult<StockMovementReference> postCatalogMovement(
            InventoryOperationContext context, CatalogStockMovementRequest request);
    InventoryOperationResult<StockReservationReference> reserve(
            InventoryOperationContext context, StockReservationRequest request);
    InventoryOperationResult<StockReservationReference> consume(
            InventoryOperationContext context, InventoryCommands.ConsumeReservation command);
    InventoryOperationResult<StockReservationReference> release(
            InventoryOperationContext context, InventoryCommands.ReleaseReservation command);
    InventoryOperationResult<StockReservationReference> expire(
            InventoryOperationContext context, InventoryCommands.ExpireReservation command);

    InventoryOperationResult<StockCountSnapshot> draftCount(
            InventoryOperationContext context, InventoryCommands.DraftCount command);
    InventoryOperationResult<StockCountSnapshot> addCountLine(
            InventoryOperationContext context, InventoryCommands.AddCountLine command);
    InventoryOperationResult<StockCountSnapshot> startCount(
            InventoryOperationContext context, InventoryCommands.CountTransition command);
    InventoryOperationResult<StockCountSnapshot> recordCount(
            InventoryOperationContext context, InventoryCommands.RecordCount command);
    InventoryOperationResult<StockCountSnapshot> reviewCount(
            InventoryOperationContext context, InventoryCommands.CountTransition command);
    InventoryOperationResult<StockCountSnapshot> postCount(
            InventoryOperationContext context, InventoryCommands.CountTransition command);
    InventoryOperationResult<StockCountSnapshot> cancelCount(
            InventoryOperationContext context, InventoryCommands.CountTransition command);

    InventoryOperationResult<WarehouseSnapshot> warehouse(
            InventoryOperationContext context, WarehouseId warehouseId);
    InventoryOperationResult<InventoryItemSnapshot> item(
            InventoryOperationContext context, InventoryItemId itemId);
    InventoryOperationResult<StockAvailability> availability(
            InventoryOperationContext context, StockKey key);
    InventoryOperationResult<StockMovementSnapshot> movement(
            InventoryOperationContext context, StockMovementId movementId);
    InventoryOperationResult<StockReservationReference> reservation(
            InventoryOperationContext context, StockReservationId reservationId);
    InventoryOperationResult<StockCountSnapshot> count(
            InventoryOperationContext context, StockCountId countId);

    InventoryOperationResult<InventoryDirectoryQueries.Page<WarehouseSnapshot>> searchWarehouses(
            InventoryOperationContext context, InventoryDirectoryQueries.Criteria criteria);
    InventoryOperationResult<InventoryDirectoryQueries.Page<InventoryDirectoryQueries.ItemSummary>> searchItems(
            InventoryOperationContext context, InventoryDirectoryQueries.Criteria criteria);
    InventoryOperationResult<InventoryDirectoryQueries.ItemSummary> itemSummary(
            InventoryOperationContext context, InventoryItemId itemId);
    InventoryOperationResult<InventoryDirectoryQueries.Page<InventoryDirectoryQueries.CountSummary>> searchCounts(
            InventoryOperationContext context, InventoryDirectoryQueries.CountCriteria criteria);
}
