package py.com.logixone.plugin.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PluginDescriptorTest {

    @Test
    void exposesTheCurrentPluginApiVersion() {
        assertEquals("0.4.2", PluginApiVersion.CURRENT.toString());
    }

    @Test
    void copiesCollectionsAndExposesImmutableState() {
        List<ContributionId> capabilities = new ArrayList<>();
        capabilities.add(new ContributionId("reference.dashboard"));

        PluginDescriptor descriptor = descriptor(capabilities);
        capabilities.clear();

        assertEquals(List.of(new ContributionId("reference.dashboard")), descriptor.capabilities());
        assertThrows(
                UnsupportedOperationException.class,
                () -> descriptor.capabilities().add(new ContributionId("reference.reports")));
    }

    @Test
    void validatesMenuAndMigrationStructureAtTheContractBoundary() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MenuContribution(
                        new ContributionId("reference.menu"), "reference.menu", "external", Optional.empty()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MigrationContribution("plg-reference", "filesystem:migrations"));
    }

    @Test
    void exposesDescriptorThroughAFrameworkNeutralSpi() {
        PluginDescriptor descriptor = descriptor(List.of());
        PluginDefinition definition = () -> descriptor;

        assertEquals(descriptor, definition.descriptor());
    }

    @Test
    void copiesScreenContractsAndOverlays() {
        ScreenDefinition screen = screenDefinition();
        ScreenOverlay overlay = new ScreenOverlay(
                new ContributionId("reference.screen.overlay"),
                screen.id(),
                new VersionRange(
                        SemanticVersion.parse("1.0.0"), SemanticVersion.parse("2.0.0")),
                List.of(new ScreenChange.Hide(new ScreenElementId("customer"))));
        List<ScreenDefinition> screens = new ArrayList<>(List.of(screen));
        List<ScreenOverlay> overlays = new ArrayList<>(List.of(overlay));

        PluginDescriptor descriptor = new PluginDescriptor(
                new PluginId("reference_plugin"),
                PluginKind.FUNCTIONAL,
                SemanticVersion.parse("1.0.0"),
                new VersionRange(
                        SemanticVersion.parse("0.4.0"), SemanticVersion.parse("0.5.0")),
                "Reference plugin",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                screens,
                overlays);
        screens.clear();
        overlays.clear();

        assertEquals(List.of(screen), descriptor.screenDefinitions());
        assertEquals(List.of(overlay), descriptor.screenOverlays());
        assertThrows(UnsupportedOperationException.class, () -> descriptor.screenDefinitions().clear());
        assertThrows(UnsupportedOperationException.class, () -> descriptor.screenOverlays().clear());
    }

    @Test
    void requiresAnExplicitPluginKind() {
        PluginDescriptor descriptor = descriptor(List.of());

        assertEquals(PluginKind.FUNCTIONAL, descriptor.kind());
        assertThrows(
                NullPointerException.class,
                () -> new PluginDescriptor(
                        descriptor.id(),
                        null,
                        descriptor.version(),
                        descriptor.pluginApiCompatibility(),
                        descriptor.displayName(),
                        descriptor.dependencies(),
                        descriptor.capabilities(),
                        descriptor.permissions(),
                        descriptor.menuContributions(),
                        descriptor.migrations()));
    }

    private static PluginDescriptor descriptor(List<ContributionId> capabilities) {
        return new PluginDescriptor(
                new PluginId("reference_plugin"),
                PluginKind.FUNCTIONAL,
                SemanticVersion.parse("1.0.0"),
                new VersionRange(SemanticVersion.parse("0.4.0"), SemanticVersion.parse("0.5.0")),
                "Reference plugin",
                List.of(),
                capabilities,
                List.of(),
                List.of(),
                List.of());
    }

    private static ScreenDefinition screenDefinition() {
        return new ScreenDefinition(
                new ScreenId(new PluginId("reference_plugin"), "dashboard"),
                SemanticVersion.parse("1.0.0"),
                List.of(new ScreenElementDefinition(
                        new ScreenElementId("customer"),
                        new ScreenRegionId("main"),
                        0,
                        new ScreenTextKey("reference.customer"),
                        Optional.empty(),
                        true,
                        true,
                        false,
                        Set.of(ScreenCustomizationOperation.HIDE))),
                List.of());
    }
}
