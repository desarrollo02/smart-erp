package py.com.logixone.kernel.application.company.command;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugin.api.PluginId;

public record ReplaceCustomizationCommand(
        CompanyId companyId,
        PluginId newCustomizationPluginId,
        long expectedVersion) {

    public ReplaceCustomizationCommand {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(newCustomizationPluginId, "newCustomizationPluginId");
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
    }
}
