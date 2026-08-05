package py.com.logixone.plugin.api;

import java.util.Objects;

public record PluginDependency(PluginId pluginId, VersionRange compatibleVersions, DependencyKind kind) {

    public PluginDependency {
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(compatibleVersions, "compatibleVersions");
        Objects.requireNonNull(kind, "kind");
    }
}
