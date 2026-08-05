package py.com.logixone.kernel.infrastructure.jakarta.plugin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import py.com.logixone.kernel.application.plugin.InvalidPluginCatalogException;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.plugin.api.PluginDefinition;

/** CDI adapter that turns physically present plugin beans into the validated registry. */
@ApplicationScoped
public class CdiPluginCatalog {

    private static final Logger LOGGER = System.getLogger(CdiPluginCatalog.class.getName());

    @Inject
    Instance<PluginDefinition> definitions;

    private volatile PluginRegistry registry;

    void initialize(@Observes @Initialized(ApplicationScoped.class) Object initializationEvent) {
        Objects.requireNonNull(initializationEvent, "initializationEvent");
        try {
            registry = bootstrap(definitions.stream());
            String plugins = registry.orderedPlugins().stream()
                    .map(descriptor -> descriptor.id() + "@" + descriptor.version())
                    .collect(Collectors.joining(","));
            LOGGER.log(
                    Level.INFO,
                    "event=plugin_catalog_initialized plugin_count=" + registry.size() + " plugins=" + plugins);
        } catch (InvalidPluginCatalogException failure) {
            String diagnostics = failure.diagnostics().stream()
                    .map(diagnostic -> diagnostic.code() + ":" + diagnostic.pluginId())
                    .collect(Collectors.joining(","));
            LOGGER.log(
                    Level.ERROR,
                    "event=plugin_catalog_initialization_failed diagnostics=" + diagnostics,
                    failure);
            throw failure;
        }
    }

    static PluginRegistry bootstrap(Stream<? extends PluginDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        return PluginRegistry.create(definitions.toList());
    }

    public boolean isInitialized() {
        return registry != null;
    }

    public PluginRegistry registry() {
        PluginRegistry current = registry;
        if (current == null) {
            throw new IllegalStateException("Plugin registry has not been initialized");
        }
        return current;
    }
}
