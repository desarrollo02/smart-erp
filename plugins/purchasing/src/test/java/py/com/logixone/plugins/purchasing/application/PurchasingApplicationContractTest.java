package py.com.logixone.plugins.purchasing.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugins.purchasing.infrastructure.application.TransactionalPurchasingUseCases;

class PurchasingApplicationContractTest {
    @Test
    void declaresTwelveDistinctPermissions() {
        assertEquals(12, PurchasingPermissions.all().size());
        assertEquals(12, PurchasingPermissions.all().stream().distinct().count());
    }

    @Test
    void allUseCasesAreOwnedByTheJtaBoundary() {
        Transactional classBoundary = TransactionalPurchasingUseCases.class
                .getAnnotation(Transactional.class);
        assertEquals(TxType.REQUIRED, classBoundary.value());
        for (Method method : PurchasingUseCases.class.getMethods()) {
            assertTrue(hasImplementation(method));
        }
    }

    @Test
    void auditedQueriesNeverOverrideTheRequiredTransactionWithSupports() throws Exception {
        for (String methodName : java.util.List.of(
                "request", "order", "requestDetail", "orderDetail", "receiptDetail",
                "returnDetail", "searchRequests", "searchOrders", "searchReceipts",
                "searchReturns")) {
            Method implementation = java.util.Arrays.stream(
                            TransactionalPurchasingUseCases.class.getMethods())
                    .filter(method -> method.getName().equals(methodName))
                    .findFirst()
                    .orElseThrow();
            Transactional boundary = implementation.getAnnotation(Transactional.class);
            assertTrue(boundary == null || boundary.value() == TxType.REQUIRED,
                    () -> methodName + " writes technical audit and requires a JTA transaction");
        }
    }

    private static boolean hasImplementation(Method contract) {
        try {
            TransactionalPurchasingUseCases.class.getMethod(
                    contract.getName(), contract.getParameterTypes());
            return true;
        } catch (NoSuchMethodException failure) {
            return false;
        }
    }
}
