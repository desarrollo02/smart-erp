package py.com.logixone.plugins.inventory.application;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory;
import py.com.logixone.plugins.inventory.application.command.InventoryCommands;
import py.com.logixone.plugins.inventory.application.port.InventoryBalanceRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryIdGenerator;
import py.com.logixone.plugins.inventory.application.port.InventoryItemRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceException;
import py.com.logixone.plugins.inventory.application.port.WarehouseRepository;
import py.com.logixone.plugins.inventory.domain.ConcurrentInventoryChangeException;
import py.com.logixone.plugins.inventory.domain.InventoryItem;
import py.com.logixone.plugins.inventory.domain.InventoryItemSnapshot;
import py.com.logixone.plugins.inventory.domain.Warehouse;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;

/** Authorized storage and inventory-enrollment commands. */
public final class InventoryStructureService {
    private static final String WAREHOUSE = "warehouse";
    private static final String INVENTORY_ITEM = "inventory_item";

    private final WarehouseRepository warehouses;
    private final InventoryItemRepository items;
    private final InventoryBalanceRepository balances;
    private final CatalogItemDirectory catalog;
    private final InventoryIdGenerator ids;
    private final InventoryAuditRecorder audit;

    public InventoryStructureService(
            WarehouseRepository warehouses,
            InventoryItemRepository items,
            InventoryBalanceRepository balances,
            CatalogItemDirectory catalog,
            InventoryIdGenerator ids,
            TechnicalAudit audit,
            Clock clock) {
        this.warehouses = Objects.requireNonNull(warehouses, "warehouses");
        this.items = Objects.requireNonNull(items, "items");
        this.balances = Objects.requireNonNull(balances, "balances");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.audit = new InventoryAuditRecorder(audit, clock);
    }

    public InventoryOperationResult<WarehouseSnapshot> openWarehouse(
            InventoryOperationContext context, InventoryCommands.OpenWarehouse command) {
        Objects.requireNonNull(command, "command");
        if (!InventoryApplicationSupport.authorized(context, InventoryPermissions.STORAGE_MANAGE)) {
            return audit.rejected(context, InventoryPermissions.STORAGE_MANAGE,
                    "OPEN_WAREHOUSE", WAREHOUSE, Optional.empty(), Optional.empty(),
                    InventoryResultCode.ACCESS_DENIED);
        }
        try {
            Warehouse warehouse = Warehouse.open(
                    company(context), ids.nextWarehouseId(), ids.nextLocationId(),
                    command.code(), command.name());
            warehouses.insert(warehouse);
            audit.changed(context, InventoryPermissions.STORAGE_MANAGE,
                    "OPEN_WAREHOUSE", WAREHOUSE, warehouse.id().toString(),
                    Optional.empty(), warehouse.version());
            return InventoryOperationResult.success(warehouse.snapshot());
        } catch (InventoryPersistenceException failure) {
            return rejected(context, "OPEN_WAREHOUSE", WAREHOUSE, Optional.empty(),
                    InventoryApplicationSupport.map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return rejected(context, "OPEN_WAREHOUSE", WAREHOUSE, Optional.empty(),
                    InventoryResultCode.INVALID_OPERATION);
        }
    }

    public InventoryOperationResult<WarehouseSnapshot> addLocation(
            InventoryOperationContext context, InventoryCommands.AddLocation command) {
        Objects.requireNonNull(command, "command");
        return mutateWarehouse(context, command.warehouseId(), command.expectedVersion(),
                "ADD_STOCK_LOCATION", warehouse -> warehouse.addLocation(
                        ids.nextLocationId(), command.code(), command.name(), command.type(),
                        command.expectedVersion()));
    }

    public InventoryOperationResult<WarehouseSnapshot> renameWarehouse(
            InventoryOperationContext context, InventoryCommands.RenameWarehouse command) {
        Objects.requireNonNull(command, "command");
        return mutateWarehouse(context, command.warehouseId(), command.expectedVersion(),
                "RENAME_WAREHOUSE", warehouse ->
                        warehouse.rename(command.name(), command.expectedVersion()));
    }

    public InventoryOperationResult<WarehouseSnapshot> renameLocation(
            InventoryOperationContext context, InventoryCommands.RenameLocation command) {
        Objects.requireNonNull(command, "command");
        return mutateWarehouse(context, command.warehouseId(), command.expectedVersion(),
                "RENAME_STOCK_LOCATION", warehouse -> warehouse.renameLocation(
                        command.locationId(), command.name(), command.expectedVersion(),
                        command.expectedLocationVersion()));
    }

    public InventoryOperationResult<WarehouseSnapshot> inactivateWarehouse(
            InventoryOperationContext context, InventoryCommands.InactivateWarehouse command) {
        Objects.requireNonNull(command, "command");
        if (!InventoryApplicationSupport.authorized(context, InventoryPermissions.STORAGE_MANAGE)) {
            return denied(context, "INACTIVATE_WAREHOUSE", WAREHOUSE,
                    Optional.of(command.warehouseId().toString()), Optional.of(command.expectedVersion()));
        }
        if (balances.hasQuantity(company(context), command.warehouseId(), Optional.empty())) {
            return rejected(context, "INACTIVATE_WAREHOUSE", WAREHOUSE,
                    Optional.of(command.warehouseId().toString()), InventoryResultCode.INVALID_OPERATION);
        }
        return mutateAuthorizedWarehouse(context, command.warehouseId(), command.expectedVersion(),
                "INACTIVATE_WAREHOUSE", warehouse ->
                        warehouse.inactivate(command.expectedVersion()));
    }

    public InventoryOperationResult<WarehouseSnapshot> inactivateLocation(
            InventoryOperationContext context, InventoryCommands.InactivateLocation command) {
        Objects.requireNonNull(command, "command");
        if (!InventoryApplicationSupport.authorized(context, InventoryPermissions.STORAGE_MANAGE)) {
            return denied(context, "INACTIVATE_STOCK_LOCATION", WAREHOUSE,
                    Optional.of(command.warehouseId().toString()), Optional.of(command.expectedVersion()));
        }
        if (balances.hasQuantity(
                company(context), command.warehouseId(), Optional.of(command.locationId()))) {
            return rejected(context, "INACTIVATE_STOCK_LOCATION", WAREHOUSE,
                    Optional.of(command.warehouseId().toString()), InventoryResultCode.INVALID_OPERATION);
        }
        return mutateAuthorizedWarehouse(context, command.warehouseId(), command.expectedVersion(),
                "INACTIVATE_STOCK_LOCATION", warehouse -> warehouse.inactivateLocation(
                        command.locationId(), command.expectedVersion(), command.expectedLocationVersion()));
    }

    public InventoryOperationResult<InventoryItemSnapshot> enrollItem(
            InventoryOperationContext context, InventoryCommands.EnrollItem command) {
        Objects.requireNonNull(command, "command");
        if (!InventoryApplicationSupport.authorized(context, InventoryPermissions.ITEMS_MANAGE)) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "ENROLL_INVENTORY_ITEM", INVENTORY_ITEM, Optional.empty(), Optional.empty(),
                    InventoryResultCode.ACCESS_DENIED);
        }
        CompanyId companyId = company(context);
        if (items.findByCatalogItemId(companyId, command.catalogItemId()).isPresent()) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "ENROLL_INVENTORY_ITEM", INVENTORY_ITEM, Optional.empty(), Optional.empty(),
                    InventoryResultCode.DUPLICATE);
        }
        var reference = catalog.findById(companyId, command.catalogItemId());
        if (reference.isEmpty()) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "ENROLL_INVENTORY_ITEM", INVENTORY_ITEM, Optional.empty(), Optional.empty(),
                    InventoryResultCode.REFERENCE_CONFLICT);
        }
        try {
            InventoryItem item = InventoryItem.enroll(
                    companyId, ids.nextItemId(), reference.orElseThrow(),
                    command.trackingMode(), command.expiryPolicy());
            items.insert(item);
            audit.changed(context, InventoryPermissions.ITEMS_MANAGE,
                    "ENROLL_INVENTORY_ITEM", INVENTORY_ITEM, item.id().toString(),
                    Optional.empty(), item.version());
            return InventoryOperationResult.success(item.snapshot());
        } catch (InventoryPersistenceException failure) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "ENROLL_INVENTORY_ITEM", INVENTORY_ITEM, Optional.empty(), Optional.empty(),
                    InventoryApplicationSupport.map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "ENROLL_INVENTORY_ITEM", INVENTORY_ITEM, Optional.empty(), Optional.empty(),
                    InventoryResultCode.INVALID_OPERATION);
        }
    }

    public InventoryOperationResult<InventoryItemSnapshot> inactivateItem(
            InventoryOperationContext context, InventoryCommands.InactivateItem command) {
        Objects.requireNonNull(command, "command");
        if (!InventoryApplicationSupport.authorized(context, InventoryPermissions.ITEMS_MANAGE)) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "INACTIVATE_INVENTORY_ITEM", INVENTORY_ITEM,
                    Optional.of(command.itemId().toString()), Optional.of(command.expectedVersion()),
                    InventoryResultCode.ACCESS_DENIED);
        }
        CompanyId companyId = company(context);
        InventoryItem item = items.findById(companyId, command.itemId()).orElse(null);
        if (item == null) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "INACTIVATE_INVENTORY_ITEM", INVENTORY_ITEM,
                    Optional.of(command.itemId().toString()), Optional.of(command.expectedVersion()),
                    InventoryResultCode.NOT_FOUND);
        }
        if (balances.hasQuantity(companyId, command.itemId())) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "INACTIVATE_INVENTORY_ITEM", INVENTORY_ITEM,
                    Optional.of(command.itemId().toString()), Optional.of(item.version()),
                    InventoryResultCode.INVALID_OPERATION);
        }
        long previousVersion = item.version();
        try {
            item.inactivate(command.expectedVersion());
            if (item.version() == previousVersion) {
                audit.unchanged(context, InventoryPermissions.ITEMS_MANAGE,
                        "INACTIVATE_INVENTORY_ITEM", INVENTORY_ITEM,
                        item.id().toString(), item.version());
            } else {
                items.update(item, previousVersion);
                audit.changed(context, InventoryPermissions.ITEMS_MANAGE,
                        "INACTIVATE_INVENTORY_ITEM", INVENTORY_ITEM,
                        item.id().toString(), Optional.of(previousVersion), item.version());
            }
            return InventoryOperationResult.success(item.snapshot());
        } catch (ConcurrentInventoryChangeException failure) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "INACTIVATE_INVENTORY_ITEM", INVENTORY_ITEM,
                    Optional.of(item.id().toString()), Optional.of(previousVersion),
                    InventoryResultCode.VERSION_CONFLICT);
        } catch (InventoryPersistenceException failure) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "INACTIVATE_INVENTORY_ITEM", INVENTORY_ITEM,
                    Optional.of(item.id().toString()), Optional.of(previousVersion),
                    InventoryApplicationSupport.map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "INACTIVATE_INVENTORY_ITEM", INVENTORY_ITEM,
                    Optional.of(item.id().toString()), Optional.of(previousVersion),
                    InventoryResultCode.INVALID_OPERATION);
        }
    }

    public InventoryOperationResult<InventoryItemSnapshot> refreshItem(
            InventoryOperationContext context, InventoryCommands.RefreshItem command) {
        Objects.requireNonNull(command, "command");
        if (!InventoryApplicationSupport.authorized(context, InventoryPermissions.ITEMS_MANAGE)) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "REFRESH_INVENTORY_ITEM", INVENTORY_ITEM,
                    Optional.of(command.itemId().toString()), Optional.of(command.expectedVersion()),
                    InventoryResultCode.ACCESS_DENIED);
        }
        CompanyId companyId = company(context);
        InventoryItem item = items.findById(companyId, command.itemId()).orElse(null);
        if (item == null) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "REFRESH_INVENTORY_ITEM", INVENTORY_ITEM,
                    Optional.of(command.itemId().toString()), Optional.of(command.expectedVersion()),
                    InventoryResultCode.NOT_FOUND);
        }
        var reference = catalog.findById(companyId, item.catalogItemId());
        if (reference.isEmpty()) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "REFRESH_INVENTORY_ITEM", INVENTORY_ITEM,
                    Optional.of(item.id().toString()), Optional.of(item.version()),
                    InventoryResultCode.REFERENCE_CONFLICT);
        }
        long previousVersion = item.version();
        try {
            item.refreshCatalogReference(reference.orElseThrow(), command.expectedVersion());
            items.update(item, previousVersion);
            audit.changed(context, InventoryPermissions.ITEMS_MANAGE,
                    "REFRESH_INVENTORY_ITEM", INVENTORY_ITEM, item.id().toString(),
                    Optional.of(previousVersion), item.version());
            return InventoryOperationResult.success(item.snapshot());
        } catch (ConcurrentInventoryChangeException failure) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "REFRESH_INVENTORY_ITEM", INVENTORY_ITEM,
                    Optional.of(item.id().toString()), Optional.of(previousVersion),
                    InventoryResultCode.VERSION_CONFLICT);
        } catch (InventoryPersistenceException failure) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "REFRESH_INVENTORY_ITEM", INVENTORY_ITEM,
                    Optional.of(item.id().toString()), Optional.of(previousVersion),
                    InventoryApplicationSupport.map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return audit.rejected(context, InventoryPermissions.ITEMS_MANAGE,
                    "REFRESH_INVENTORY_ITEM", INVENTORY_ITEM,
                    Optional.of(item.id().toString()), Optional.of(previousVersion),
                    InventoryResultCode.INVALID_OPERATION);
        }
    }

    private InventoryOperationResult<WarehouseSnapshot> mutateWarehouse(
            InventoryOperationContext context,
            py.com.logixone.plugins.inventory.api.WarehouseId warehouseId,
            long expectedVersion,
            String operation,
            WarehouseMutation mutation) {
        if (!InventoryApplicationSupport.authorized(context, InventoryPermissions.STORAGE_MANAGE)) {
            return denied(context, operation, WAREHOUSE,
                    Optional.of(warehouseId.toString()), Optional.of(expectedVersion));
        }
        return mutateAuthorizedWarehouse(context, warehouseId, expectedVersion, operation, mutation);
    }

    private InventoryOperationResult<WarehouseSnapshot> mutateAuthorizedWarehouse(
            InventoryOperationContext context,
            py.com.logixone.plugins.inventory.api.WarehouseId warehouseId,
            long expectedVersion,
            String operation,
            WarehouseMutation mutation) {
        Warehouse warehouse = warehouses.findById(company(context), warehouseId).orElse(null);
        if (warehouse == null) {
            return rejected(context, operation, WAREHOUSE,
                    Optional.of(warehouseId.toString()), InventoryResultCode.NOT_FOUND);
        }
        long previousVersion = warehouse.version();
        try {
            mutation.apply(warehouse);
            if (warehouse.version() == previousVersion) {
                audit.unchanged(context, InventoryPermissions.STORAGE_MANAGE,
                        operation, WAREHOUSE, warehouse.id().toString(), warehouse.version());
            } else {
                warehouses.update(warehouse, previousVersion);
                audit.changed(context, InventoryPermissions.STORAGE_MANAGE,
                        operation, WAREHOUSE, warehouse.id().toString(),
                        Optional.of(previousVersion), warehouse.version());
            }
            return InventoryOperationResult.success(warehouse.snapshot());
        } catch (ConcurrentInventoryChangeException failure) {
            return rejected(context, operation, WAREHOUSE,
                    Optional.of(warehouseId.toString()), InventoryResultCode.VERSION_CONFLICT);
        } catch (InventoryPersistenceException failure) {
            return rejected(context, operation, WAREHOUSE,
                    Optional.of(warehouseId.toString()),
                    InventoryApplicationSupport.map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return rejected(context, operation, WAREHOUSE,
                    Optional.of(warehouseId.toString()), InventoryResultCode.INVALID_OPERATION);
        }
    }

    private InventoryOperationResult<WarehouseSnapshot> denied(
            InventoryOperationContext context,
            String operation,
            String resourceType,
            Optional<String> resourceId,
            Optional<Long> version) {
        return audit.rejected(context, InventoryPermissions.STORAGE_MANAGE,
                operation, resourceType, resourceId, version, InventoryResultCode.ACCESS_DENIED);
    }

    private InventoryOperationResult<WarehouseSnapshot> rejected(
            InventoryOperationContext context,
            String operation,
            String resourceType,
            Optional<String> resourceId,
            InventoryResultCode code) {
        return audit.rejected(context, InventoryPermissions.STORAGE_MANAGE,
                operation, resourceType, resourceId, Optional.empty(), code);
    }

    private static CompanyId company(InventoryOperationContext context) {
        return context.companyContext().companyId();
    }

    @FunctionalInterface
    private interface WarehouseMutation {
        void apply(Warehouse warehouse);
    }
}
