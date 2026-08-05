package py.com.logixone.kernel.domain.company;

import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;

public record CompanyPluginResolution(
        CompanyId companyId,
        PluginId customizationPluginId,
        boolean operational,
        List<PluginDescriptor> orderedPlugins,
        List<CompanyPluginDiagnostic> diagnostics) {

    public CompanyPluginResolution {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(customizationPluginId, "customizationPluginId");
        orderedPlugins = List.copyOf(orderedPlugins);
        diagnostics = List.copyOf(diagnostics);
        if (!operational && !orderedPlugins.isEmpty()) {
            throw new IllegalArgumentException("a non-operational company cannot expose plugins");
        }
    }
}
