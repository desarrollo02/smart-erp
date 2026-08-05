package py.com.logixone.web.selector;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.plugin.api.PlatformSelectorSourceDefinition;
import py.com.logixone.web.security.TrustedAdminWebAccess;
import py.com.logixone.web.security.TrustedWebAccessException;

/** Request-scoped authorization boundary for native selector administration links. */
@Named("nativeSelectorSources")
@RequestScoped
public class NativeSelectorSourceViewBean {

    @Inject
    TrustedAdminWebAccess access;

    private Map<String, NativeSelectorSourceView> sources = Map.of();

    @PostConstruct
    void initialize() {
        Set<SystemPermission> permissions = currentPermissions();
        Map<String, NativeSelectorSourceView> views = new LinkedHashMap<>();
        NativeSelectorSourceCatalog.all().forEach((usageId, source) -> views.put(
                usageId,
                new NativeSelectorSourceView(
                        usageId, source, isAuthorized(source, permissions))));
        sources = Map.copyOf(views);
    }

    private Set<SystemPermission> currentPermissions() {
        try {
            return access.requireAny().permissions();
        } catch (TrustedWebAccessException failure) {
            if (failure.status() == 401 || failure.status() == 403) {
                return Set.of();
            }
            throw failure;
        }
    }

    private static boolean isAuthorized(
            PlatformSelectorSourceDefinition source,
            Set<SystemPermission> permissions) {
        if (!source.manageable()) {
            return false;
        }
        String required = source.managementPermission().orElseThrow().value();
        return permissions.stream().anyMatch(permission -> permission.value().equals(required));
    }

    public Map<String, NativeSelectorSourceView> getSources() {
        return sources;
    }
}
