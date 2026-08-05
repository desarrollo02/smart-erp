package py.com.logixone.tests.jta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.MenuContribution;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.ScreenChange;
import py.com.logixone.plugin.api.ScreenCustomizationOperation;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugin.api.ScreenElementDefinition;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenFragmentId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenOverlay;
import py.com.logixone.plugin.api.ScreenRegionId;
import py.com.logixone.plugin.api.ScreenSlotDefinition;
import py.com.logixone.plugin.api.ScreenSlotId;
import py.com.logixone.plugin.api.ScreenTextKey;
import py.com.logixone.plugin.api.VersionRange;

@ApplicationScoped
public class JtaTestPluginDefinitions {

    private static final PluginId FUNCTIONAL = new PluginId("jta_functional");
    private static final PluginId CUSTOM_A = new PluginId("jta_custom_a");
    private static final PluginId CUSTOM_B = new PluginId("jta_custom_b");
    private static final ScreenId SCREEN = new ScreenId(FUNCTIONAL, "dashboard");
    private static final VersionRange VERSION_ONE = new VersionRange(
            SemanticVersion.parse("1.0.0"), SemanticVersion.parse("2.0.0"));

    @Produces
    @Dependent
    PluginDefinition functional() {
        return definition("jta_functional", PluginKind.FUNCTIONAL);
    }

    @Produces
    @Dependent
    PluginDefinition customizationA() {
        return definition("jta_custom_a", PluginKind.CUSTOMIZATION);
    }

    @Produces
    @Dependent
    PluginDefinition customizationB() {
        return definition("jta_custom_b", PluginKind.CUSTOMIZATION);
    }

    @Produces
    @Dependent
    PluginDefinition rollbackCustomization() {
        return definition("jta_custom_rollback", PluginKind.CUSTOMIZATION);
    }

    private static PluginDefinition definition(String id, PluginKind kind) {
        PluginId pluginId = new PluginId(id);
        ContributionId permission = new ContributionId(id + ".permission");
        PluginDescriptor descriptor = new PluginDescriptor(
                pluginId,
                kind,
                SemanticVersion.parse("1.0.0"),
                new VersionRange(
                        SemanticVersion.parse("0.4.0"),
                        SemanticVersion.parse("0.5.0")),
                id,
                dependencies(pluginId),
                List.of(new ContributionId(id + ".capability")),
                List.of(permission),
                List.of(new MenuContribution(
                        new ContributionId(id + ".menu"),
                        id + ".menu",
                        "/" + id,
                        Optional.of(permission))),
                List.of(),
                screenDefinitions(pluginId),
                screenOverlays(pluginId));
        return () -> descriptor;
    }

    private static List<PluginDependency> dependencies(PluginId pluginId) {
        return pluginId.equals(CUSTOM_A) || pluginId.equals(CUSTOM_B)
                ? List.of(new PluginDependency(FUNCTIONAL, VERSION_ONE, DependencyKind.REQUIRED))
                : List.of();
    }

    private static List<ScreenDefinition> screenDefinitions(PluginId pluginId) {
        if (!pluginId.equals(FUNCTIONAL)) {
            return List.of();
        }
        return List.of(new ScreenDefinition(
                SCREEN,
                SemanticVersion.parse("1.0.0"),
                List.of(
                        new ScreenElementDefinition(
                                new ScreenElementId("summary"),
                                new ScreenRegionId("main"),
                                0,
                                new ScreenTextKey("jta.dashboard.summary"),
                                Optional.empty(),
                                true,
                                true,
                                false,
                                Set.of(
                                        ScreenCustomizationOperation.CHANGE_LABEL,
                                        ScreenCustomizationOperation.HIDE,
                                        ScreenCustomizationOperation.REQUIRE)),
                        new ScreenElementDefinition(
                                new ScreenElementId("refresh"),
                                new ScreenRegionId("actions"),
                                0,
                                new ScreenTextKey("jta.dashboard.refresh"),
                                Optional.empty(),
                                true,
                                true,
                                false,
                                Set.of(ScreenCustomizationOperation.DISABLE))),
                List.of(new ScreenSlotDefinition(
                        new ScreenSlotId("extensions"),
                        new ScreenRegionId("main"),
                        1,
                        1))));
    }

    private static List<ScreenOverlay> screenOverlays(PluginId pluginId) {
        if (pluginId.equals(CUSTOM_A)) {
            return List.of(overlay(
                    pluginId,
                    "jta_custom_a.dashboard",
                    List.of(
                            new ScreenChange.Label(
                                    new ScreenElementId("summary"),
                                    new ScreenTextKey("jta_custom_a.dashboard.summary")),
                            new ScreenChange.Require(new ScreenElementId("summary")),
                            new ScreenChange.SlotContent(
                                    new ScreenSlotId("extensions"),
                                    new ScreenFragmentId(pluginId, "notice"),
                                    0))));
        }
        if (pluginId.equals(CUSTOM_B)) {
            return List.of(overlay(
                    pluginId,
                    "jta_custom_b.dashboard",
                    List.of(
                            new ScreenChange.Label(
                                    new ScreenElementId("summary"),
                                    new ScreenTextKey("jta_custom_b.dashboard.summary")),
                            new ScreenChange.Hide(new ScreenElementId("summary")),
                            new ScreenChange.Disable(new ScreenElementId("refresh")),
                            new ScreenChange.SlotContent(
                                    new ScreenSlotId("extensions"),
                                    new ScreenFragmentId(pluginId, "notice"),
                                    0))));
        }
        return List.of();
    }

    private static ScreenOverlay overlay(
            PluginId pluginId,
            String overlayId,
            List<ScreenChange> changes) {
        return new ScreenOverlay(
                new ContributionId(overlayId),
                SCREEN,
                VERSION_ONE,
                changes);
    }
}
