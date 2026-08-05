package py.com.logixone.web.shell;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;

/** JSF-friendly authorized menu projection for one current request. */
public final class ShellMenuItemView {

    private final PluginId pluginId;
    private final ContributionId menuId;
    private final String label;
    private final String route;
    private final Optional<ContributionId> requiredPermission;

    public ShellMenuItemView(
            PluginId pluginId,
            ContributionId menuId,
            String label,
            String route,
            Optional<ContributionId> requiredPermission) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId");
        this.menuId = Objects.requireNonNull(menuId, "menuId");
        this.label = Objects.requireNonNull(label, "label");
        this.route = Objects.requireNonNull(route, "route");
        this.requiredPermission = Objects.requireNonNull(
                requiredPermission, "requiredPermission");
    }

    public PluginId pluginId() {
        return pluginId;
    }

    public ContributionId menuId() {
        return menuId;
    }

    public Optional<ContributionId> requiredPermission() {
        return requiredPermission;
    }

    public String getLabel() {
        return label;
    }

    public String getRoute() {
        return route;
    }
}
