package py.com.logixone.web.admin;

import java.util.Objects;
import py.com.logixone.kernel.application.company.admin.CompanyPluginActivationView;
import py.com.logixone.kernel.domain.company.PluginActivationState;

/** JSF-friendly desired/effective plugin row. */
public final class AdminPluginActivationView {

    private final String pluginId;
    private final String displayName;
    private final String version;
    private final String desiredState;
    private final String desiredStateLabel;
    private final long decisionVersion;
    private final boolean enabled;
    private final boolean effective;

    private AdminPluginActivationView(
            String pluginId,
            String displayName,
            String version,
            String desiredState,
            String desiredStateLabel,
            long decisionVersion,
            boolean enabled,
            boolean effective) {
        this.pluginId = pluginId;
        this.displayName = displayName;
        this.version = version;
        this.desiredState = desiredState;
        this.desiredStateLabel = desiredStateLabel;
        this.decisionVersion = decisionVersion;
        this.enabled = enabled;
        this.effective = effective;
    }

    static AdminPluginActivationView from(CompanyPluginActivationView plugin) {
        Objects.requireNonNull(plugin, "plugin");
        boolean enabled = plugin.desiredState() == PluginActivationState.ENABLED;
        return new AdminPluginActivationView(
                plugin.pluginId().value(),
                plugin.displayName(),
                plugin.version(),
                plugin.desiredState().name(),
                enabled ? "Habilitado" : "Deshabilitado",
                plugin.decisionVersion(),
                enabled,
                plugin.effective());
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getVersion() {
        return version;
    }

    public String getDesiredState() {
        return desiredState;
    }

    public String getDesiredStateLabel() {
        return desiredStateLabel;
    }

    public long getDecisionVersion() {
        return decisionVersion;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEffective() {
        return effective;
    }
}
