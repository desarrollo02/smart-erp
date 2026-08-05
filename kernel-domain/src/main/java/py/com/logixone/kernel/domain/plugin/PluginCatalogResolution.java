package py.com.logixone.kernel.domain.plugin;

import java.util.List;
import py.com.logixone.plugin.api.PluginDescriptor;

public record PluginCatalogResolution(
        List<PluginDescriptor> orderedPlugins, List<PluginDiagnostic> diagnostics) {

    public PluginCatalogResolution {
        orderedPlugins = List.copyOf(orderedPlugins);
        diagnostics = List.copyOf(diagnostics);
        if (!orderedPlugins.isEmpty() && !diagnostics.isEmpty()) {
            throw new IllegalArgumentException("An invalid catalog cannot expose a partial plugin order");
        }
    }

    public boolean isValid() {
        return diagnostics.isEmpty();
    }
}
