package py.com.logixone.kernel.application.plugin;

import java.util.List;
import java.util.stream.Collectors;
import py.com.logixone.kernel.domain.plugin.PluginDiagnostic;

public final class InvalidPluginCatalogException extends IllegalStateException {

    private final List<PluginDiagnostic> diagnostics;

    public InvalidPluginCatalogException(List<PluginDiagnostic> diagnostics) {
        super(message(diagnostics));
        this.diagnostics = List.copyOf(diagnostics);
        if (this.diagnostics.isEmpty()) {
            throw new IllegalArgumentException("An invalid plugin catalog requires diagnostics");
        }
    }

    public List<PluginDiagnostic> diagnostics() {
        return diagnostics;
    }

    private static String message(List<PluginDiagnostic> diagnostics) {
        return "Invalid plugin catalog: " + List.copyOf(diagnostics).stream()
                .map(diagnostic -> diagnostic.code()
                        + "[plugin=" + diagnostic.pluginId()
                        + ",subject=" + diagnostic.subject() + "]")
                .collect(Collectors.joining(","));
    }
}
