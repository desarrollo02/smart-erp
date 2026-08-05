package py.com.logixone.kernel.infrastructure.jakarta.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import py.com.logixone.kernel.application.health.HealthStatus;
import py.com.logixone.kernel.application.health.ReadinessCheck;
import py.com.logixone.kernel.infrastructure.jakarta.plugin.CdiPluginCatalog;

@ApplicationScoped
public class PluginCatalogReadinessCheck implements ReadinessCheck {

    private final CdiPluginCatalog pluginCatalog;

    @Inject
    public PluginCatalogReadinessCheck(CdiPluginCatalog pluginCatalog) {
        this.pluginCatalog = pluginCatalog;
    }

    @Override
    public String name() {
        return "catalog";
    }

    @Override
    public HealthStatus check() {
        return pluginCatalog.isInitialized() ? HealthStatus.UP : HealthStatus.DOWN;
    }
}
