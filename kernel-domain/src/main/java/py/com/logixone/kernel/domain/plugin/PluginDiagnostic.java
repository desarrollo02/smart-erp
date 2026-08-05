package py.com.logixone.kernel.domain.plugin;

import java.util.Objects;
import py.com.logixone.plugin.api.PluginId;

public record PluginDiagnostic(PluginDiagnosticCode code, PluginId pluginId, String subject)
        implements Comparable<PluginDiagnostic> {

    public PluginDiagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(subject, "subject");
        if (subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
    }

    @Override
    public int compareTo(PluginDiagnostic other) {
        Objects.requireNonNull(other, "other");
        int result = pluginId.compareTo(other.pluginId);
        if (result == 0) {
            result = code.name().compareTo(other.code.name());
        }
        if (result == 0) {
            result = subject.compareTo(other.subject);
        }
        return result;
    }
}
