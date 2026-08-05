package py.com.logixone.kernel.application.company.command;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.company.PluginActivationState;
import py.com.logixone.plugin.api.PluginId;

public record ChangePluginActivationCommand(
        CompanyId companyId,
        PluginId pluginId,
        PluginActivationState desiredState,
        long expectedVersion) {

    public ChangePluginActivationCommand {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(desiredState, "desiredState");
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
    }
}
