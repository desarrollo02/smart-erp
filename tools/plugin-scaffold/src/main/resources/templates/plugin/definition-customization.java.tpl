package {{PACKAGE_NAME}};

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

/** Company-specific customization entry point for {{DISPLAY_NAME_JAVA}}. */
@ApplicationScoped
public class {{DEFINITION_CLASS}} implements PluginDefinition {

    public static final PluginId ID = new PluginId("{{PLUGIN_ID}}");
    public static final PluginId TARGET_PLUGIN_ID = new PluginId("{{TARGET_PLUGIN_ID}}");
    public static final VersionRange TARGET_PLUGIN_VERSIONS = new VersionRange(
            SemanticVersion.parse("{{TARGET_VERSION_MIN}}"),
            SemanticVersion.parse("{{TARGET_VERSION_MAX}}"));

    private static final PluginDescriptor DESCRIPTOR = new PluginDescriptor(
            ID,
            PluginKind.CUSTOMIZATION,
            SemanticVersion.parse("{{PLUGIN_VERSION}}"),
            new VersionRange(
                    SemanticVersion.parse("{{PLUGIN_API_MIN}}"),
                    SemanticVersion.parse("{{PLUGIN_API_MAX}}")),
            "{{DISPLAY_NAME_JAVA}}",
            List.of(new PluginDependency(
                    TARGET_PLUGIN_ID,
                    TARGET_PLUGIN_VERSIONS,
                    DependencyKind.REQUIRED)),
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
