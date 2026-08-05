package py.com.logixone.plugins.inventory.infrastructure.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.kernel.api.security.AuthorizedCompanyOperation;
import py.com.logixone.plugins.inventory.api.MovementQuantity;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StockMovementDirection;
import py.com.logixone.plugins.inventory.api.StockMovementLine;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;
import py.com.logixone.plugins.inventory.api.StockMovementType;
import py.com.logixone.plugins.inventory.api.StockSourceReference;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.InventoryOperationResult;
import py.com.logixone.plugins.inventory.application.InventoryPermissions;
import py.com.logixone.plugins.inventory.application.InventoryResultCode;
import py.com.logixone.plugins.inventory.application.InventoryUseCases;

class InventoryApplicationAdaptersTest {
    private static final CompanyId COMPANY = new CompanyId(uuid(1));

    @Test
    void cdiAdapterRequestsTheExactPermissionAndRejectsCompanySubstitution() {
        List<String> requestedPermissions = new ArrayList<>();
        CdiInventoryContracts adapter = new CdiInventoryContracts();
        adapter.authorization = (plugin, permission) -> {
            requestedPermissions.add(permission);
            return authorization(COMPANY, plugin, permission);
        };
        adapter.useCases = useCases((method, arguments) -> {
            if (method.getName().equals("availability")) {
                return InventoryOperationResult.failure(InventoryResultCode.NOT_FOUND);
            }
            return InventoryOperationResult.failure(InventoryResultCode.INVALID_OPERATION);
        });

        assertTrue(adapter.find(COMPANY, key()).isEmpty());
        assertEquals(InventoryPermissions.VIEW.value(), requestedPermissions.getFirst());

        assertThrows(SecurityException.class, () -> adapter.find(
                new CompanyId(uuid(999)), key()));
    }

    @Test
    void cdiAdapterSelectsTheReinforcedAdjustmentPermission() {
        List<String> requestedPermissions = new ArrayList<>();
        CdiInventoryContracts adapter = new CdiInventoryContracts();
        adapter.authorization = (plugin, permission) -> {
            requestedPermissions.add(permission);
            return authorization(COMPANY, plugin, permission);
        };
        adapter.useCases = useCases((method, arguments) ->
                InventoryOperationResult.failure(InventoryResultCode.INVALID_OPERATION));

        assertThrows(IllegalStateException.class, () -> adapter.post(COMPANY, adjustment()));
        assertEquals(InventoryPermissions.ADJUSTMENTS_POST.value(), requestedPermissions.getFirst());
    }

    @Test
    void failedMutationMarksTheJtaTransactionForRollback() throws Exception {
        TransactionalInventoryUseCases useCases = new TransactionalInventoryUseCases();
        boolean[] rollback = {false};
        useCases.transactions = (TransactionSynchronizationRegistry) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{TransactionSynchronizationRegistry.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setRollbackOnly")) rollback[0] = true;
                    return defaultValue(method.getReturnType());
                });
        Method mutation = TransactionalInventoryUseCases.class
                .getDeclaredMethod("mutation", InventoryOperationResult.class);
        mutation.setAccessible(true);

        mutation.invoke(useCases, InventoryOperationResult.failure(InventoryResultCode.STORAGE_FAILURE));

        assertTrue(rollback[0]);
        Transactional classBoundary = TransactionalInventoryUseCases.class
                .getAnnotation(Transactional.class);
        assertEquals(TxType.REQUIRED, classBoundary.value());
        assertEquals(TxType.SUPPORTS, TransactionalInventoryUseCases.class
                .getMethod("availability",
                        py.com.logixone.plugins.inventory.application.InventoryOperationContext.class,
                        StockKey.class)
                .getAnnotation(Transactional.class).value());
    }

    private static InventoryUseCases useCases(Invocation invocation) {
        return (InventoryUseCases) Proxy.newProxyInstance(
                InventoryApplicationAdaptersTest.class.getClassLoader(),
                new Class<?>[]{InventoryUseCases.class},
                (proxy, method, args) -> invocation.invoke(method, args));
    }

    private static AuthorizedCompanyOperation authorization(
            CompanyId companyId, String plugin, String permission) {
        return new AuthorizedCompanyOperation(
                new AuthenticatedCompanyContext(
                        new AuthenticatedActor(new AppUserId(uuid(90))), companyId),
                plugin, permission, "request:inventory-adapter");
    }

    private static StockMovementRequest adjustment() {
        return new StockMovementRequest(
                StockMovementType.ADJUSTMENT, "COUNT",
                new StockSourceReference("TEST", "source-1"), "adjust-1",
                List.of(new StockMovementLine(
                        key(), StockMovementDirection.INCREASE,
                        new MovementQuantity("EA", BigDecimal.ONE, "EA", BigDecimal.ONE,
                                BigDecimal.ONE, 3))), Optional.empty());
    }

    private static StockKey key() {
        return new StockKey(
                new InventoryItemId(uuid(2)), new WarehouseId(uuid(3)),
                new StockLocationId(uuid(4)), Optional.empty(), Optional.empty(),
                Optional.empty(), StockCondition.AVAILABLE);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }

    private static UUID uuid(long suffix) { return new UUID(0, suffix); }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(Method method, Object[] arguments);
    }
}
