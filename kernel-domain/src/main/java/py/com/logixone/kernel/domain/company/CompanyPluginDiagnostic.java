package py.com.logixone.kernel.domain.company;

import java.util.Objects;
import py.com.logixone.plugin.api.PluginId;

public record CompanyPluginDiagnostic(
        CompanyPluginDiagnosticCode code,
        PluginId pluginId,
        String subject) implements Comparable<CompanyPluginDiagnostic> {

    public CompanyPluginDiagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(subject, "subject");
        if (subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
    }

    @Override
    public int compareTo(CompanyPluginDiagnostic other) {
        int byCode = code.compareTo(Objects.requireNonNull(other, "other").code);
        if (byCode != 0) {
            return byCode;
        }
        int byPlugin = pluginId.compareTo(other.pluginId);
        return byPlugin != 0 ? byPlugin : subject.compareTo(other.subject);
    }
}
