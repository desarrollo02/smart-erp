package py.com.logixone.plugins.purchasing.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.transaction.Transactional;
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
        assertTrue(TransactionalPurchasingUseCases.class.isAnnotationPresent(Transactional.class));
        for (Method method : PurchasingUseCases.class.getMethods()) {
            assertTrue(hasImplementation(method));
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
