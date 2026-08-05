package py.com.logixone.kernel.infrastructure.jakarta.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import java.util.ArrayList;
import java.util.List;
import py.com.logixone.kernel.application.health.ApplicationHealthService;
import py.com.logixone.kernel.application.health.ReadinessCheck;

@ApplicationScoped
public class HealthServiceProducer {

    @Produces
    @Dependent
    public ApplicationHealthService applicationHealthService(Instance<ReadinessCheck> discoveredChecks) {
        List<ReadinessCheck> checks = new ArrayList<>();
        discoveredChecks.forEach(checks::add);
        return new ApplicationHealthService(checks);
    }
}
