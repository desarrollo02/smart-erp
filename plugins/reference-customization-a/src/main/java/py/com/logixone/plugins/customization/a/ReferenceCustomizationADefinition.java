package py.com.logixone.plugins.customization.a;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.ScreenChange;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenFragmentId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenOverlay;
import py.com.logixone.plugin.api.ScreenSlotId;
import py.com.logixone.plugin.api.ScreenTextKey;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

/** Physical example of a customization JAR assigned exclusively to one company. */
@ApplicationScoped
public class ReferenceCustomizationADefinition implements PluginDefinition {

    public static final PluginId ID = new PluginId("reference_custom_a");
    private static final PluginId REFERENCE_PLUGIN = new PluginId("reference_plugin");
    private static final VersionRange VERSION_ONE = new VersionRange(
            SemanticVersion.parse("1.0.0"), SemanticVersion.parse("2.0.0"));

    private static final PluginDescriptor DESCRIPTOR = new PluginDescriptor(
            ID,
            PluginKind.CUSTOMIZATION,
            SemanticVersion.parse("1.0.0"),
            new VersionRange(SemanticVersion.parse("0.4.0"), SemanticVersion.parse("0.5.0")),
            "Reference customization A",
            List.of(new PluginDependency(
                    REFERENCE_PLUGIN, VERSION_ONE, DependencyKind.REQUIRED)),
            List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(new ScreenOverlay(
                    new ContributionId("reference_custom_a.dashboard"),
                    new ScreenId(REFERENCE_PLUGIN, "dashboard"),
                    VERSION_ONE,
                    List.of(
                            new ScreenChange.Label(
                                    new ScreenElementId("summary"),
                                    new ScreenTextKey("reference_custom_a.dashboard.summary")),
                            new ScreenChange.Help(
                                    new ScreenElementId("summary"),
                                    new ScreenTextKey("reference_custom_a.dashboard.summary.help")),
                            new ScreenChange.Require(new ScreenElementId("summary")),
                            new ScreenChange.Move(new ScreenElementId("summary"), 0),
                            new ScreenChange.SlotContent(
                                    new ScreenSlotId("dashboard_extensions"),
                                    new ScreenFragmentId(ID, "tax_notice"),
                                    0)))));

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }
}
