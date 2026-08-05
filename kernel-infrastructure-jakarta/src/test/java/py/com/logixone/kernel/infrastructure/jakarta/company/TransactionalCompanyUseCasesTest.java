package py.com.logixone.kernel.infrastructure.jakarta.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.transaction.Transactional;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.application.company.command.ChangeCompanyStatusCommand;
import py.com.logixone.kernel.application.company.command.ChangePluginActivationCommand;
import py.com.logixone.kernel.application.company.command.RegisterCompanyCommand;
import py.com.logixone.kernel.application.company.command.ReplaceCustomizationCommand;

class TransactionalCompanyUseCasesTest {

    @Test
    void everyMutatingBoundaryRequiresATransactionAndNoRestAnnotation() {
        assertTransactional("register", RegisterCompanyCommand.class);
        assertTransactional("changeStatus", ChangeCompanyStatusCommand.class);
        assertTransactional("replaceCustomization", ReplaceCustomizationCommand.class);
        assertTransactional("changeActivation", ChangePluginActivationCommand.class);
        assertTrue(TransactionalCompanyUseCases.class.getAnnotations().length > 0);
        assertEquals(
                0,
                java.util.Arrays.stream(TransactionalCompanyUseCases.class.getAnnotations())
                        .filter(annotation -> annotation.annotationType().getPackageName()
                                .startsWith("jakarta.ws.rs"))
                        .count());
    }

    private static void assertTransactional(String methodName, Class<?> commandType) {
        try {
            Method method = TransactionalCompanyUseCases.class.getMethod(methodName, commandType);
            assertTrue(method.isAnnotationPresent(Transactional.class));
        } catch (NoSuchMethodException failure) {
            throw new AssertionError(failure);
        }
    }
}
