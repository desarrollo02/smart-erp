package py.com.logixone.plugin.api;

import java.util.Objects;
import java.util.Optional;

public record MenuContribution(
        ContributionId id,
        String labelKey,
        String route,
        Optional<ContributionId> requiredPermission) {

    public MenuContribution {
        Objects.requireNonNull(id, "id");
        labelKey = requireText(labelKey, "labelKey");
        route = requireText(route, "route");
        if (!route.startsWith("/") || route.startsWith("//")) {
            throw new IllegalArgumentException("Menu route must be an application-relative absolute path: " + route);
        }
        requiredPermission = Objects.requireNonNull(requiredPermission, "requiredPermission");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
