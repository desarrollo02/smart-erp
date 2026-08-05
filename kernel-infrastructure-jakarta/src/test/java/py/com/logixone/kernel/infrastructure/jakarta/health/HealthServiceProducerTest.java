package py.com.logixone.kernel.infrastructure.jakarta.health;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class HealthServiceProducerTest {

    @Test
    void finalNeutralServiceUsesANonProxyingDependentProducerScope() throws NoSuchMethodException {
        Method producer = HealthServiceProducer.class.getMethod("applicationHealthService", Instance.class);

        assertNotNull(producer.getAnnotation(Produces.class));
        assertNotNull(producer.getAnnotation(Dependent.class));
        assertNull(producer.getAnnotation(ApplicationScoped.class));
    }
}
