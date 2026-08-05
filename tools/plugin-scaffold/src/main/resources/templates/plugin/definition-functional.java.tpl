package {{PACKAGE_NAME}};

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

/** Neutral entry point for {{DISPLAY_NAME_JAVA}}. */
@ApplicationScoped
public class {{DEFINITION_CLASS}} implements PluginDefinition {

    public static final PluginId ID = new PluginId("{{PLUGIN_ID}}");

    private static final PluginDescriptor DESCRIPTOR = new PluginDescriptor(
            ID,
            PluginKind.FUNCTIONAL,
            SemanticVersion.parse("{{PLUGIN_VERSION}}"),
            new VersionRange(
                    SemanticVersion.parse("{{PLUGIN_API_MIN}}"),
                    SemanticVersion.parse("{{PLUGIN_API_MAX}}")),
            "{{DISPLAY_NAME_JAVA}}",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of());

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }
}
