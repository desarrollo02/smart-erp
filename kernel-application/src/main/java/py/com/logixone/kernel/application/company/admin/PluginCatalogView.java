package py.com.logixone.kernel.application.company.admin;

import java.util.List;
import java.util.Objects;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;

/** Read-only projection of one physically present, validated plugin. */
public record PluginCatalogView(
        PluginId pluginId,
        String displayName,
        PluginKind kind,
        String version,
        List<String> dependencies) {

    public PluginCatalogView {
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(version, "version");
        dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
    }

    public static PluginCatalogView from(PluginDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        return new PluginCatalogView(
                descriptor.id(),
                descriptor.displayName(),
                descriptor.kind(),
                descriptor.version().toString(),
                descriptor.dependencies().stream()
                        .map(dependency -> dependency.pluginId() + " · " + dependency.kind())
                        .toList());
    }
}
