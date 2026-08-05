package py.com.logixone.plugin.api;

import java.util.List;
import java.util.Objects;

public record PluginDescriptor(
        PluginId id,
        PluginKind kind,
        SemanticVersion version,
        VersionRange pluginApiCompatibility,
        String displayName,
        List<PluginDependency> dependencies,
        List<ContributionId> capabilities,
        List<ContributionId> permissions,
        List<MenuContribution> menuContributions,
        List<MigrationContribution> migrations,
        List<ScreenDefinition> screenDefinitions,
        List<ScreenOverlay> screenOverlays) {

    public PluginDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(pluginApiCompatibility, "pluginApiCompatibility");
        Objects.requireNonNull(displayName, "displayName");
        if (displayName.isBlank() || displayName.length() > 128) {
            throw new IllegalArgumentException("displayName must contain between 1 and 128 characters");
        }
        dependencies = List.copyOf(dependencies);
        capabilities = List.copyOf(capabilities);
        permissions = List.copyOf(permissions);
        menuContributions = List.copyOf(menuContributions);
        migrations = List.copyOf(migrations);
        screenDefinitions = List.copyOf(screenDefinitions);
        screenOverlays = List.copyOf(screenOverlays);
    }

    public PluginDescriptor(
            PluginId id,
            PluginKind kind,
            SemanticVersion version,
            VersionRange pluginApiCompatibility,
            String displayName,
            List<PluginDependency> dependencies,
            List<ContributionId> capabilities,
            List<ContributionId> permissions,
            List<MenuContribution> menuContributions,
            List<MigrationContribution> migrations) {
        this(
                id,
                kind,
                version,
                pluginApiCompatibility,
                displayName,
                dependencies,
                capabilities,
                permissions,
                menuContributions,
                migrations,
                List.of(),
                List.of());
    }
}
