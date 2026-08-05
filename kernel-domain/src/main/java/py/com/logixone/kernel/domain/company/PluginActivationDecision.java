package py.com.logixone.kernel.domain.company;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugin.api.PluginId;

/** Desired functional-plugin state retained independently from physical presence. */
public record PluginActivationDecision(
        CompanyId companyId,
        PluginId pluginId,
        PluginActivationState desiredState,
        long version) {

    public PluginActivationDecision {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(desiredState, "desiredState");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    public boolean isEnabled() {
        return desiredState == PluginActivationState.ENABLED;
    }
}
