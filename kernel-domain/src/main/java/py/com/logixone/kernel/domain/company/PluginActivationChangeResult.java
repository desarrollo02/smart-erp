package py.com.logixone.kernel.domain.company;

import java.util.List;
import java.util.Objects;
import py.com.logixone.plugin.api.PluginId;

public record PluginActivationChangeResult(
        PluginId pluginId,
        PluginActivationState desiredState,
        boolean allowed,
        List<CompanyPluginDiagnostic> diagnostics) {

    public PluginActivationChangeResult {
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(desiredState, "desiredState");
        diagnostics = List.copyOf(diagnostics);
        if (allowed == !diagnostics.isEmpty()) {
            throw new IllegalArgumentException("allowed changes must have no diagnostics");
        }
    }
}
