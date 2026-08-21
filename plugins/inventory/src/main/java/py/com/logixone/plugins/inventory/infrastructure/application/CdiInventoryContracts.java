package py.com.logixone.plugins.inventory.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.inventory.api.CatalogStockMovementRequest;
import py.com.logixone.plugins.inventory.api.CatalogStockReservationRequest;
import py.com.logixone.plugins.inventory.api.InventoryAvailability;
import py.com.logixone.plugins.inventory.api.InventoryPurchaseMovements;
import py.com.logixone.plugins.inventory.api.InventoryMovements;
import py.com.logixone.plugins.inventory.api.InventoryReservations;
import py.com.logixone.plugins.inventory.api.InventoryStorageDirectory;
import py.com.logixone.plugins.inventory.api.StockLocationReference;
import py.com.logixone.plugins.inventory.api.StorageSearchPage;
import py.com.logixone.plugins.inventory.api.StorageSearchQuery;
import py.com.logixone.plugins.inventory.api.StockAvailability;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockMovementReference;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.StockReservationReference;
import py.com.logixone.plugins.inventory.api.StockReservationRequest;
import py.com.logixone.plugins.inventory.api.WarehouseReference;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.InventoryIdentity;
import py.com.logixone.plugins.inventory.application.InventoryOperationContext;
import py.com.logixone.plugins.inventory.application.InventoryOperationResult;
import py.com.logixone.plugins.inventory.application.InventoryPermissions;
import py.com.logixone.plugins.inventory.application.InventoryResultCode;
import py.com.logixone.plugins.inventory.application.InventoryUseCases;
import py.com.logixone.plugins.inventory.application.command.InventoryCommands;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;

/** Authorized CDI adapters for the stable contracts published by inventory-api. */
@ApplicationScoped
@Transactional(rollbackOn = RuntimeException.class)
public class CdiInventoryContracts implements InventoryAvailability, InventoryMovements,
        InventoryPurchaseMovements, InventoryReservations, InventoryStorageDirectory {

    @Inject CurrentCompanyAuthorization authorization;
    @Inject InventoryUseCases useCases;

    @Override
    @Transactional(TxType.SUPPORTS)
    public Optional<StockAvailability> find(CompanyId companyId, StockKey key) {
        InventoryOperationContext context = context(companyId, InventoryPermissions.VIEW);
        InventoryOperationResult<StockAvailability> result = useCases.availability(context, key);
        if (result.code() == InventoryResultCode.NOT_FOUND) {
            return Optional.empty();
        }
        return Optional.of(required(result));
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public StorageSearchPage searchWarehouses(
            CompanyId companyId, StorageSearchQuery query) {
        Objects.requireNonNull(query, "query");
        var result = required(useCases.searchWarehouses(
                context(companyId, InventoryPermissions.VIEW),
                new py.com.logixone.plugins.inventory.application.query.InventoryDirectoryQueries.Criteria(
                        query.text(), query.activeOnly() ? Optional.of(true) : Optional.empty(),
                        query.offset(), query.limit())));
        return new StorageSearchPage(
                result.items().stream().map(CdiInventoryContracts::warehouseReference).toList(),
                result.total(), result.offset(), result.limit());
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public Optional<WarehouseReference> findWarehouse(
            CompanyId companyId, WarehouseId warehouseId) {
        Objects.requireNonNull(warehouseId, "warehouseId");
        var result = useCases.warehouse(
                context(companyId, InventoryPermissions.VIEW), warehouseId);
        if (result.code() == InventoryResultCode.NOT_FOUND) {
            return Optional.empty();
        }
        return Optional.of(warehouseReference(required(result)));
    }

    @Override
    public StockMovementReference post(CompanyId companyId, StockMovementRequest request) {
        Objects.requireNonNull(request, "request");
        ContributionId permission = switch (request.type()) {
            case ADJUSTMENT, REVERSAL -> InventoryPermissions.ADJUSTMENTS_POST;
            case RECEIPT, ISSUE, TRANSFER -> InventoryPermissions.MOVEMENTS_POST;
        };
        return required(useCases.postMovement(context(companyId, permission), request));
    }

    @Override
    public StockMovementReference postCatalogItem(
            CompanyId companyId, CatalogStockMovementRequest request) {
        Objects.requireNonNull(request, "request");
        return required(useCases.postCatalogMovement(
                context(companyId, InventoryPermissions.PURCHASE_MOVEMENTS_POST), request));
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public Optional<StockReservationReference> find(
            CompanyId companyId, StockReservationId reservationId) {
        var result = useCases.reservation(
                context(companyId, InventoryPermissions.VIEW), reservationId);
        if (result.code() == InventoryResultCode.NOT_FOUND) {
            return Optional.empty();
        }
        return Optional.of(required(result));
    }

    @Override
    public StockReservationReference reserve(
            CompanyId companyId, StockReservationRequest request) {
        return required(useCases.reserve(
                context(companyId, InventoryPermissions.RESERVATIONS_MANAGE), request));
    }

    @Override
    public StockReservationReference reserveCatalogItem(
            CompanyId companyId, CatalogStockReservationRequest request) {
        return required(useCases.reserveCatalogItem(
                context(companyId, InventoryPermissions.RESERVATIONS_MANAGE), request));
    }

    @Override
    public StockReservationReference consume(
            CompanyId companyId,
            StockReservationId reservationId,
            long expectedVersion,
            BigDecimal quantity,
            String idempotencyKey) {
        return required(useCases.consume(
                context(companyId, InventoryPermissions.RESERVATIONS_MANAGE),
                new InventoryCommands.ConsumeReservation(
                        reservationId, expectedVersion, quantity, idempotencyKey)));
    }

    @Override
    public StockReservationReference release(
            CompanyId companyId,
            StockReservationId reservationId,
            long expectedVersion,
            BigDecimal quantity,
            String idempotencyKey) {
        return required(useCases.release(
                context(companyId, InventoryPermissions.RESERVATIONS_MANAGE),
                new InventoryCommands.ReleaseReservation(
                        reservationId, expectedVersion, quantity, idempotencyKey)));
    }

    @Override
    public StockReservationReference expire(
            CompanyId companyId,
            StockReservationId reservationId,
            long expectedVersion,
            String idempotencyKey) {
        return required(useCases.expire(
                context(companyId, InventoryPermissions.RESERVATIONS_MANAGE),
                new InventoryCommands.ExpireReservation(
                        reservationId, expectedVersion, idempotencyKey)));
    }

    private InventoryOperationContext context(CompanyId requestedCompany, ContributionId permission) {
        Objects.requireNonNull(requestedCompany, "companyId");
        var authorized = authorization.require(
                InventoryIdentity.PLUGIN_ID.value(), permission.value());
        if (!requestedCompany.equals(authorized.context().companyId())) {
            throw new SecurityException("Requested inventory company differs from the authorized company");
        }
        return InventoryOperationContext.from(authorized);
    }

    private static <T> T required(InventoryOperationResult<T> result) {
        if (!result.successful()) {
            throw new IllegalStateException("Inventory operation failed: " + result.code().name());
        }
        return result.value().orElseThrow();
    }

    private static WarehouseReference warehouseReference(WarehouseSnapshot warehouse) {
        return new WarehouseReference(
                warehouse.id(), warehouse.code(), warehouse.name(), warehouse.active(),
                warehouse.version(), warehouse.locations().stream()
                        .map(location -> new StockLocationReference(
                                location.id(), location.code(), location.name(),
                                location.active(), location.version()))
                        .toList());
    }
}
