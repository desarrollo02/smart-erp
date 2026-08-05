package py.com.logixone.kernel.application.company.admin;

import java.util.Objects;
import py.com.logixone.kernel.domain.company.PluginActivationState;
import py.com.logixone.plugin.api.PluginId;

/** Desired and effective state of a functional plugin for one company. */
public record CompanyPluginActivationView(
        PluginId pluginId,
        String displayName,
        String version,
        PluginActivationState desiredState,
        long decisionVersion,
        boolean effective) {

    public CompanyPluginActivationView {
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(desiredState, "desiredState");
        if (decisionVersion < 0) {
            throw new IllegalArgumentException("decisionVersion must not be negative");
        }
    }
}
