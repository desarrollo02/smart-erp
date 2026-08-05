package py.com.logixone.kernel.application.security.access;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;

/** Public menu route paired with its current owning plugin and permission. */
public record TrustedMenuItem(
        PluginId pluginId,
        ContributionId menuId,
        String labelKey,
        String route,
        Optional<ContributionId> requiredPermission) {

    public TrustedMenuItem {
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(menuId, "menuId");
        labelKey = requireText(labelKey, "labelKey");
        route = requireText(route, "route");
        if (!route.startsWith("/") || route.startsWith("//")) {
            throw new IllegalArgumentException("route must be application-relative");
        }
        requiredPermission = Objects.requireNonNull(requiredPermission, "requiredPermission");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()
                || !value.equals(value.strip())
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(name + " must be safe text");
        }
        return value;
    }
}
