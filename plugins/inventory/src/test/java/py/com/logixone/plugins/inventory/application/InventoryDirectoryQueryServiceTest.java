package py.com.logixone.plugins.inventory.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.application.port.InventoryBalanceRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryDirectoryRepository;
import py.com.logixone.plugins.inventory.application.port.InventoryItemRepository;
import py.com.logixone.plugins.inventory.application.port.StockCountRepository;
import py.com.logixone.plugins.inventory.application.port.StockMovementRepository;
import py.com.logixone.plugins.inventory.application.port.StockReservationRepository;
import py.com.logixone.plugins.inventory.application.port.WarehouseRepository;
import py.com.logixone.plugins.inventory.application.query.InventoryDirectoryQueries;
import py.com.logixone.plugins.inventory.domain.StockCountScope;
import py.com.logixone.plugins.inventory.domain.StockCountState;
import py.com.logixone.plugins.inventory.domain.WarehouseSnapshot;

class InventoryDirectoryQueryServiceTest {
    private static final CompanyId COMPANY = new CompanyId(new UUID(0, 10));

    @Test
    void normalizesAndBoundsDirectoryCriteria() {
        var criteria = new InventoryDirectoryQueries.Criteria(
                Optional.of("  CENTRAL  "), Optional.of(true), 0, 20);

        assertEquals(Optional.of("central"), criteria.text());
        assertThrows(IllegalArgumentException.class, () ->
                new InventoryDirectoryQueries.Criteria(Optional.empty(), Optional.empty(), 0, 101));
        assertThrows(IllegalArgumentException.class, () ->
                new InventoryDirectoryQueries.CountCriteria(Optional.empty(), -1, 20));
    }

    @Test
    void deniesDirectoryBeforeCallingTheReadPort() {
        RecordingDirectory directory = new RecordingDirectory();
        InventoryQueryService service = service(directory);

        InventoryOperationResult<InventoryDirectoryQueries.Page<WarehouseSnapshot>> result =
                service.searchWarehouses(context(new ContributionId("inventory.items.manage")),
                        criteria());

        assertEquals(InventoryResultCode.ACCESS_DENIED, result.code());
        assertEquals(0, directory.calls);
    }

    @Test
    void returnsCompanyScopedPagesUsingTheViewPermission() {
        RecordingDirectory directory = new RecordingDirectory();
        InventoryQueryService service = service(directory);

        var warehouses = service.searchWarehouses(context(InventoryPermissions.VIEW), criteria());
        var counts = service.searchCounts(
                context(InventoryPermissions.VIEW),
                new InventoryDirectoryQueries.CountCriteria(Optional.empty(), 0, 20));

        assertTrue(warehouses.successful());
        assertEquals(1, warehouses.value().orElseThrow().total());
        assertTrue(counts.successful());
        assertEquals(2, directory.calls);
    }

    private static InventoryQueryService service(InventoryDirectoryRepository directory) {
        return new InventoryQueryService(
                noOp(WarehouseRepository.class),
                noOp(InventoryItemRepository.class),
                noOp(InventoryBalanceRepository.class),
                noOp(StockMovementRepository.class),
                noOp(StockReservationRepository.class),
                noOp(StockCountRepository.class),
                directory);
    }

    private static InventoryOperationContext context(ContributionId permission) {
        return new InventoryOperationContext(
                new AuthenticatedCompanyContext(
                        new AuthenticatedActor(new AppUserId(new UUID(0, 11))), COMPANY),
                InventoryIdentity.PLUGIN_ID,
                permission,
                "test:inventory-directory");
    }

    private static InventoryDirectoryQueries.Criteria criteria() {
        return new InventoryDirectoryQueries.Criteria(Optional.empty(), Optional.empty(), 0, 20);
    }

    @SuppressWarnings("unchecked")
    private static <T> T noOp(Class<T> type) {
        return (T) Proxy.newProxyInstance(
                InventoryDirectoryQueryServiceTest.class.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, arguments) -> {
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    if (method.getReturnType().equals(boolean.class)) return false;
                    return null;
                });
    }

    private static final class RecordingDirectory implements InventoryDirectoryRepository {
        private int calls;

        @Override
        public InventoryDirectoryQueries.Page<WarehouseSnapshot> warehouses(
                CompanyId companyId, InventoryDirectoryQueries.Criteria criteria) {
            calls++;
            assertEquals(COMPANY, companyId);
            WarehouseSnapshot value = new WarehouseSnapshot(
                    COMPANY,
                    new py.com.logixone.plugins.inventory.api.WarehouseId(new UUID(0, 12)),
                    "CENTRAL", "Depósito central", true, 0, List.of());
            return new InventoryDirectoryQueries.Page<>(List.of(value), 1, 0, 20);
        }

        @Override
        public InventoryDirectoryQueries.Page<InventoryDirectoryQueries.ItemSummary> items(
                CompanyId companyId, InventoryDirectoryQueries.Criteria criteria) {
            calls++;
            return new InventoryDirectoryQueries.Page<>(List.of(), 0, 0, 20);
        }

        @Override
        public Optional<InventoryDirectoryQueries.ItemSummary> item(
                CompanyId companyId, InventoryItemId inventoryItemId) {
            calls++;
            return Optional.empty();
        }

        @Override
        public InventoryDirectoryQueries.Page<InventoryDirectoryQueries.CountSummary> counts(
                CompanyId companyId, InventoryDirectoryQueries.CountCriteria criteria) {
            calls++;
            var value = new InventoryDirectoryQueries.CountSummary(
                    new StockCountId(new UUID(0, 13)),
                    new StockCountScope(
                            new py.com.logixone.plugins.inventory.api.WarehouseId(new UUID(0, 12)),
                            Optional.empty()),
                    StockCountState.DRAFT, 0, 0);
            return new InventoryDirectoryQueries.Page<>(List.of(value), 1, 0, 20);
        }
    }
}
