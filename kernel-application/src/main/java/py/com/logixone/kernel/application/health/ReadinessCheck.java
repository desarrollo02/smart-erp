package py.com.logixone.kernel.application.health;

public interface ReadinessCheck {

    String name();

    HealthStatus check();
}
