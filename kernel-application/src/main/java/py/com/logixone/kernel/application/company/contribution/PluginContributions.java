package py.com.logixone.kernel.application.company.contribution;

import java.util.List;
import java.util.Objects;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.MenuContribution;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugin.api.ScreenOverlay;
import py.com.logixone.plugin.api.SemanticVersion;

/** Immutable contribution projection that preserves the owning plugin and declared order. */
public record PluginContributions(
        PluginId pluginId,
        PluginKind pluginKind,
        SemanticVersion pluginVersion,
        List<PluginDependency> dependencies,
        List<ContributionId> capabilities,
        List<ContributionId> permissions,
        List<MenuContribution> menuContributions,
        List<ScreenDefinition> screenDefinitions,
        List<ScreenOverlay> screenOverlays) {

    public PluginContributions {
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(pluginKind, "pluginKind");
        Objects.requireNonNull(pluginVersion, "pluginVersion");
        dependencies = List.copyOf(dependencies);
        capabilities = List.copyOf(capabilities);
        permissions = List.copyOf(permissions);
        menuContributions = List.copyOf(menuContributions);
        screenDefinitions = List.copyOf(screenDefinitions);
        screenOverlays = List.copyOf(screenOverlays);
    }

    static PluginContributions fromDescriptor(PluginDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        return new PluginContributions(
                descriptor.id(),
                descriptor.kind(),
                descriptor.version(),
                descriptor.dependencies(),
                descriptor.capabilities(),
                descriptor.permissions(),
                descriptor.menuContributions(),
                descriptor.screenDefinitions(),
                descriptor.screenOverlays());
    }
}
