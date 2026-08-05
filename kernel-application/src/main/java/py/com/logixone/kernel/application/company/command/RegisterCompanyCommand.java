package py.com.logixone.kernel.application.company.command;

import java.util.Objects;
import py.com.logixone.plugin.api.PluginId;

public record RegisterCompanyCommand(PluginId customizationPluginId) {

    public RegisterCompanyCommand {
        Objects.requireNonNull(customizationPluginId, "customizationPluginId");
    }
}
