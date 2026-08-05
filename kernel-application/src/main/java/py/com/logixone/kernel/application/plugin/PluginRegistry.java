package py.com.logixone.kernel.application.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import py.com.logixone.kernel.domain.plugin.PluginCatalogResolution;
import py.com.logixone.kernel.domain.plugin.PluginCatalogResolver;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;

/** Immutable runtime view of the validated, physically present plugin catalog. */
public final class PluginRegistry {

    private final List<PluginDescriptor> orderedPlugins;
    private final Map<PluginId, PluginDescriptor> pluginsById;

    private PluginRegistry(List<PluginDescriptor> orderedPlugins) {
        this.orderedPlugins = List.copyOf(orderedPlugins);
        Map<PluginId, PluginDescriptor> byId = new TreeMap<>();
        this.orderedPlugins.forEach(descriptor -> byId.put(descriptor.id(), descriptor));
        this.pluginsById = Collections.unmodifiableMap(byId);
    }

    public static PluginRegistry create(Collection<? extends PluginDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        List<PluginDescriptor> descriptors = definitions.stream()
                .map(definition -> Objects.requireNonNull(definition, "definition"))
                .map(PluginRegistry::descriptorOf)
                .toList();
        PluginCatalogResolution resolution = new PluginCatalogResolver().resolve(descriptors);
        if (!resolution.isValid()) {
            throw new InvalidPluginCatalogException(resolution.diagnostics());
        }
        return new PluginRegistry(resolution.orderedPlugins());
    }

    public List<PluginDescriptor> orderedPlugins() {
        return orderedPlugins;
    }

    public Optional<PluginDescriptor> find(PluginId pluginId) {
        return Optional.ofNullable(pluginsById.get(Objects.requireNonNull(pluginId, "pluginId")));
    }

    public boolean contains(PluginId pluginId) {
        return pluginsById.containsKey(Objects.requireNonNull(pluginId, "pluginId"));
    }

    public int size() {
        return orderedPlugins.size();
    }

    private static PluginDescriptor descriptorOf(PluginDefinition definition) {
        PluginDescriptor descriptor = definition.descriptor();
        if (descriptor == null) {
            throw new IllegalArgumentException("Plugin definition returned a null descriptor");
        }
        return descriptor;
    }
}
