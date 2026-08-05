package py.com.logixone.plugins.reference;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.MenuContribution;
import py.com.logixone.plugin.api.MigrationContribution;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.ScreenCustomizationOperation;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugin.api.ScreenElementDefinition;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenElementType;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenRegionId;
import py.com.logixone.plugin.api.ScreenSlotDefinition;
import py.com.logixone.plugin.api.ScreenSlotId;
import py.com.logixone.plugin.api.ScreenTextKey;
import py.com.logixone.plugin.api.VersionRange;

/** Minimal executable plugin used to prove physical composition and CDI discovery. */
@ApplicationScoped
public class ReferencePluginDefinition implements PluginDefinition {

    private static final ContributionId VIEW_PERMISSION =
            new ContributionId("reference.dashboard.view");

    private static final PluginDescriptor DESCRIPTOR = new PluginDescriptor(
            new PluginId("reference_plugin"),
            PluginKind.FUNCTIONAL,
            SemanticVersion.parse("1.0.0"),
            new VersionRange(SemanticVersion.parse("0.4.0"), SemanticVersion.parse("0.5.0")),
            "Reference plugin",
            List.of(),
            List.of(new ContributionId("reference.dashboard")),
            List.of(VIEW_PERMISSION),
            List.of(new MenuContribution(
                    new ContributionId("reference.menu"),
                    "reference.menu",
                    "/reference",
                    Optional.of(VIEW_PERMISSION))),
            List.of(new MigrationContribution(
                    "plg_reference_plugin",
                    "classpath:db/migration/reference_plugin")),
            List.of(new ScreenDefinition(
                    new ScreenId(new PluginId("reference_plugin"), "dashboard"),
                    SemanticVersion.parse("1.0.0"),
                    List.of(
                            new ScreenElementDefinition(
                                    new ScreenElementId("greeting"),
                                    ScreenElementType.DISPLAY_TEXT,
                                    new ScreenRegionId("main"),
                                    0,
                                    new ScreenTextKey("reference.dashboard.greeting"),
                                    Optional.of(new ScreenTextKey("reference.dashboard.greeting.help")),
                                    true,
                                    true,
                                    false,
                                    Set.of(
                                            ScreenCustomizationOperation.CHANGE_LABEL,
                                            ScreenCustomizationOperation.CHANGE_HELP,
                                            ScreenCustomizationOperation.HIDE,
                                            ScreenCustomizationOperation.DISABLE,
                                            ScreenCustomizationOperation.REORDER)),
                            new ScreenElementDefinition(
                                    new ScreenElementId("summary"),
                                    ScreenElementType.TEXT_INPUT,
                                    new ScreenRegionId("main"),
                                    1,
                                    new ScreenTextKey("reference.dashboard.summary"),
                                    Optional.empty(),
                                    true,
                                    true,
                                    false,
                                    EnumSet.allOf(ScreenCustomizationOperation.class)),
                            new ScreenElementDefinition(
                                    new ScreenElementId("refresh"),
                                    ScreenElementType.ACTION,
                                    new ScreenRegionId("actions"),
                                    0,
                                    new ScreenTextKey("reference.dashboard.refresh"),
                                    Optional.empty(),
                                    true,
                                    true,
                                    false,
                                    Set.of(
                                            ScreenCustomizationOperation.HIDE,
                                            ScreenCustomizationOperation.DISABLE))),
                    List.of(new ScreenSlotDefinition(
                            new ScreenSlotId("dashboard_extensions"),
                            new ScreenRegionId("main"),
                            2,
                            2)))),
            List.of());

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }
}
